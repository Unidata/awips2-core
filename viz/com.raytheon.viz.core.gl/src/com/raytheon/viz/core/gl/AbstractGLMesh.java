/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.viz.core.gl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jogamp.opengl.GL2;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.util.GridGeometryWrapChecker;
import com.raytheon.uf.common.geospatial.util.WorldWrapChecker;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGridMesh;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.PaintStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.jobs.JobPool;
import com.raytheon.viz.core.gl.GLGeometryObject2D.GLGeometryObjectData;
import com.raytheon.viz.core.gl.SharedCoordMap.SharedCoordinateKey;
import com.raytheon.viz.core.gl.SharedCoordMap.SharedCoordinates;

/**
 * Base class for all GL meshes. This class maintains a set of vertex
 * coordinates that contain reprojected points spaced out evenly over an image.
 * These coordinates will be rendered using triangle strips to perform an image
 * reprojection.
 * 
 * Each mesh will have unique vertex coordinates but since all mesh coordinates
 * are evenly spaced the texture coordinates can be shared by any meshes that
 * have the same number of points. This helps reduce memory usage especially for
 * tilesets which tend to need the same size mesh for all tiles.
 * 
 * This class handles world wrap issues by breaking apart triangle strips that
 * wrap the world. The missing sections of the strip are rebuilt using
 * individual triangles on either side of the antimeridian.
 * 
 * This class handles most of the work of rendering a mesh, subclasses have to
 * generate a mesh key and efficiently generate world coordinates. A mesh key
 * specifies the size of the mesh, larger meshes require more space but are also
 * more accurate, subclasses must determine a balance between space. accuracy.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------
 * Jul 01, 2010           mschenke  Initial creation
 * Feb 21, 2014  2817     bsteffen  Remove Deprecated reproject.
 * Apr 05, 2016  5400     bsteffen  implement IGridMesh, javadoc.
 * Oct 25, 2017  6387     bsteffen  implement IGLMesh
 * Feb 05, 2018  7209     bsteffen  Remove unreasonably large triangles.
 * Aug 30, 2018  7440     bsteffen  Add texture wrapping for worldwide grids.
 * Jan 18, 2023		  srcarter@ucar Bring over MJ changes for GL2
 * 
 * </pre>
 * 
 * @author mschenke
 */
public abstract class AbstractGLMesh implements IGLMesh, IGridMesh {

    private static final JobPool calculator = new JobPool("Mesh Calculator", 2,
            false);

    protected static enum State {
        NEW, CALCULATING, CALCULATED, COMPILED, INVALID;
    }

    private State internalState = State.NEW;

    private GLGeometryObject2D vertexCoords;

    private SharedCoordinates sharedTextureCoords;

    protected SharedCoordinateKey key;

    /*
     * For world wrapping we maintain a set of triangle strips that fill in any
     * cut segments.
     */
    private GLGeometryObject2D wwcVertexCoords;

    private GLGeometryObject2D wwcTextureCoords;

    private Runnable calculate = new Runnable() {
        @Override
        public void run() {
            synchronized (calculate) {
                if (internalState == State.CALCULATING) {
                    /*
                     * If we aren't in CALCULATING state, we were disposed while
                     * waiting to run and shouldn't run now
                     */
                    if (calculateMesh()) {
                        internalState = State.CALCULATED;
                    } else {
                        internalState = State.INVALID;
                    }
                }
            }
        }
    };

    private int geometryType;

    private MathTransform imageCRSToLatLon;

    private MathTransform latLonToTargetGrid;

    protected GeneralGridGeometry targetGeometry;

    protected GridGeometry2D imageGeometry;

    protected int refCount;

    protected boolean wrapTexture = false;

    protected AbstractGLMesh(int geometryType) {
        this.geometryType = geometryType;
        this.refCount = 1;
    }

    protected final void initialize(GridGeometry2D imageGeometry,
            GeneralGridGeometry targetGeometry) throws VizException {
        this.imageGeometry = imageGeometry;
        if (imageGeometry != null) {
            try {
                imageCRSToLatLon = MapUtil.getTransformToLatLon(
                        imageGeometry.getCoordinateReferenceSystem());
            } catch (Throwable t) {
                throw new VizException(
                        "Error construcing image to lat/lon transform", t);
            }
            wrapTexture = GridGeometryWrapChecker.checkForWrapping(
                    imageGeometry) == imageGeometry.getGridRange2D().width;
        }
        this.targetGeometry = targetGeometry;

        // Set up convenience transforms
        try {
            DefaultMathTransformFactory factory = new DefaultMathTransformFactory();
            latLonToTargetGrid = factory.createConcatenatedTransform(
                    MapUtil.getTransformFromLatLon(
                            targetGeometry.getCoordinateReferenceSystem()),
                    targetGeometry.getGridToCRS(PixelInCell.CELL_CENTER)
                            .inverse());
        } catch (Throwable t) {
            internalState = State.INVALID;
            throw new VizException("Error projecting mesh", t);
        }

        internalState = State.CALCULATING;
        calculator.schedule(calculate);
    }

    @Override
    public final synchronized PaintStatus paint(IGLTarget glTarget,
            PaintProperties paintProps) throws VizException {
        State internalState = this.internalState;
        if (internalState == State.NEW) {
            throw new VizException(
                    "Class did not properly call initialize on construction");
        } else if (internalState == State.INVALID) {
            // Don't paint if invalid to avoid crashes
            return PaintStatus.ERROR;
        }

        try {
            if (internalState == State.CALCULATED) {
                // We finished calculating the mesh, compile it
                sharedTextureCoords = SharedCoordMap.get(key, glTarget);
                vertexCoords.compile(glTarget.getGl().getGL2());
                if (wwcTextureCoords != null && wwcVertexCoords != null) {
                    wwcTextureCoords.compile(glTarget.getGl().getGL2());
                    wwcVertexCoords.compile(glTarget.getGl().getGL2());
                }
                this.internalState = internalState = State.COMPILED;
            }

            if (internalState == State.COMPILED) {
                if (wrapTexture) {
                    GL2 gl = glTarget.getGl().getGL2();
                    gl.glActiveTexture(GL2.GL_TEXTURE0);
                    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
                            GL2.GL_REPEAT);
                }

                GLGeometryPainter.paintGeometries(glTarget.getGl().getGL2(),
                        vertexCoords, sharedTextureCoords.getTextureCoords());
                if (wwcTextureCoords != null && wwcVertexCoords != null) {
                    glTarget.getGl().getGL2().glColor3f(1.0f, 0.0f, 0.0f);
                    GLGeometryPainter.paintGeometries(glTarget.getGl().getGL2(),
                            wwcVertexCoords, wwcTextureCoords);
                    glTarget.getGl().getGL2().glColor3f(0.0f, 1.0f, 0.0f);
                }
                return PaintStatus.PAINTED;
            } else if (internalState == State.CALCULATING) {
                glTarget.setNeedsRefresh(true);
                return PaintStatus.REPAINT;
            } else {
                return PaintStatus.ERROR;
            }
        } catch (VizException e) {
            this.internalState = State.INVALID;
            throw e;
        }
    }

    protected void use() {
        refCount += 1;
    }

    @Override
    public synchronized void dispose() {
        refCount -= 1;
        if (refCount > 0) {
            return;
        }
        // Synchronize on calculate so we don't dispose while running
        synchronized (calculate) {
            // Cancel calculation job from running
            calculator.cancel(calculate);
            // dispose and reset vertexCoords
            if (vertexCoords != null) {
                vertexCoords.dispose();
                vertexCoords = null;
            }
            if (sharedTextureCoords != null) {
                SharedCoordMap.remove(key);
                sharedTextureCoords = null;
            }
            if (wwcTextureCoords != null) {
                wwcTextureCoords.dispose();
                wwcTextureCoords = null;
            }
            if (wwcVertexCoords != null) {
                wwcVertexCoords.dispose();
                wwcVertexCoords = null;
            }
            internalState = State.INVALID;
        }
    }

    private boolean calculateMesh() {
        key = generateKey(imageGeometry, imageCRSToLatLon);
        try {
            double[][][] worldCoordinates = generateWorldCoords(imageGeometry,
                    imageCRSToLatLon);
            vertexCoords = new GLGeometryObject2D(
                    new GLGeometryObjectData(geometryType, GL2.GL_VERTEX_ARRAY));
            vertexCoords.allocate(
                    worldCoordinates.length * worldCoordinates[0].length);
            UnreasonablyLargeTriangleFilter filter = new UnreasonablyLargeTriangleFilter(
                    key, targetGeometry, imageGeometry, vertexCoords);
            // Check for world wrapping
            WorldWrapChecker wwc = new WorldWrapChecker(targetGeometry);
            List<double[]> vSegment = new ArrayList<>();
            for (int i = 0; i < worldCoordinates.length; ++i) {
                double[][] strip = worldCoordinates[i];
                double[] prev1 = null;
                double[] prev2 = null;
                for (int j = 0; j < strip.length; ++j) {
                    double[] next = strip[j];
                    boolean wrap1 = prev1 != null
                            && wwc.check(prev1[0], next[0]);
                    boolean wrap2 = prev2 != null
                            && wwc.check(prev2[0], next[0]);
                    if (wrap1 || wrap2) {
                        fixWorldWrap(wwc, prev2, prev1, next, i, j);
                        if (wrap1 || vSegment.size() > 1) {
                            filter.addSegment(vSegment);
                            vSegment.clear();
                        }
                    }
                    vSegment.add(worldToPixel(next));

                    prev2 = prev1;
                    prev1 = next;
                }
                filter.addSegment(vSegment);
                vSegment.clear();

            }
            return true;
        } catch (Exception e) {
            Activator.statusHandler.handle(Priority.PROBLEM,
                    "Error calculating mesh", e);
        }
        return false;
    }

    private void fixWorldWrap(WorldWrapChecker wwc, double[] p2, double[] p1,
            double[] n, int i, int j) {
        // make sure we have all 3 points
        if (p2 == null || p1 == null || n == null) {
            return;
        }
        // figure out texture coordinates
        float dX = (1.0f / (key.horizontalDivisions));
        float dY = (1.0f / (key.verticalDivisions));
        double[] tp2 = { (i + ((j - 2) % 2)) * dX, (j - 2) / 2 * dY };
        double[] tp1 = { (i + ((j - 1) % 2)) * dX, (j - 1) / 2 * dY };
        double[] tn = { (i + (j % 2)) * dX, j / 2 * dY };
        // find which two sides are cut
        boolean wwcp1n = wwc.check(p1[0], n[0]);
        boolean wwcp2n = wwc.check(p2[0], n[0]);
        boolean wwcp1p2 = wwc.check(p1[0], p2[0]);
        double[] a = null;
        double[] b = null;
        double[] c = null;
        double[] ta = null;
        double[] tb = null;
        double[] tc = null;
        if (wwcp1n && wwcp2n && !wwcp1p2) {
            a = n;
            b = p1;
            c = p2;
            ta = tn;
            tb = tp1;
            tc = tp2;
        } else if (wwcp1n && !wwcp2n && wwcp1p2) {
            a = p1;
            b = p2;
            c = n;
            ta = tp1;
            tb = tp2;
            tc = tn;
        } else if (!wwcp1n && wwcp2n && wwcp1p2) {
            a = p2;
            b = n;
            c = p1;
            ta = tp2;
            tb = tn;
            tc = tp1;
        } else {
            // this occurs when a pole is within the triangle, maybe we should
            // try to cut these triangles, but its hard.
            return;
        }
        if (wwcTextureCoords == null || wwcVertexCoords == null) {
            wwcVertexCoords = new GLGeometryObject2D(new GLGeometryObjectData(
                    GL2.GL_TRIANGLE_STRIP, GL2.GL_VERTEX_ARRAY));
            wwcTextureCoords = new GLGeometryObject2D(new GLGeometryObjectData(
                    GL2.GL_TRIANGLE_STRIP, GL2.GL_TEXTURE_COORD_ARRAY));
        }
        // at this point triangle abc is a triangle in which sides ab and ac
        // are cut by the inverse central meridian. We need to find the two
        // points of intersection and use them to make a triangle with a ion one
        // side and a quad with bc on the other side. ta, tb, tc represent the
        // texture coordinates for their respective points.
        double ax = wwc.toProjectionRange(a[0]);
        double bx = wwc.toProjectionRange(b[0]);
        double cx = wwc.toProjectionRange(c[0]);
        // Get various x distances to use as weights in interpolating
        double abDist = 360 - Math.abs(ax - bx);
        double acDist = 360 - Math.abs(ax - cx);
        double amDist = ax - wwc.getLowInverseCentralMeridian();
        if (amDist > 360) {
            amDist = amDist - 360;
        }
        // x location to use for midpoints on the triangle side, should be on
        // same side of central meridian as a
        double tx = wwc.getLowInverseCentralMeridian() + 0.00001;
        // x location to use for midpoints on the quad side, should be on
        // same side of central meridian as b and c
        double qx = wwc.getHighInverseCentralMeridian() - 0.00001;
        // If a is closer to the central meridian on the other side then switch
        // amDist, tx, and qx
        if (amDist > 180) {
            amDist = 360 - amDist;
            double tmp = tx;
            tx = qx;
            qx = tmp;
        }
        // interpolated y coordinate and texture coordinates along the ab line.
        double aby = a[1] + amDist * (b[1] - a[1]) / abDist;
        double abtx = ta[0] + amDist * (tb[0] - ta[0]) / abDist;
        double abty = ta[1] + amDist * (tb[1] - ta[1]) / abDist;
        // interpolated y coordinate and texture coordinates along the ac line.
        double acy = a[1] + amDist * (c[1] - a[1]) / acDist;
        double actx = ta[0] + amDist * (tc[0] - ta[0]) / acDist;
        double acty = ta[1] + amDist * (tc[1] - ta[1]) / acDist;
        // all done with math, assemble everything into a triangle and a quad to
        // set in the geometry.
        double[][] tri = new double[3][];
        double[][] triTex = new double[3][];
        tri[0] = worldToPixel(a);
        triTex[0] = ta;
        tri[1] = worldToPixel(new double[] { tx, aby });
        triTex[1] = new double[] { abtx, abty };
        tri[2] = worldToPixel(new double[] { tx, acy });
        triTex[2] = new double[] { actx, acty };
        double[][] quad = new double[4][];
        double[][] quadTex = new double[4][];
        quad[0] = worldToPixel(b);
        quadTex[0] = tb;
        quad[1] = worldToPixel(c);
        quadTex[1] = tc;
        quad[2] = worldToPixel(new double[] { qx, aby });
        quadTex[2] = new double[] { abtx, abty };
        quad[3] = worldToPixel(new double[] { qx, acy });
        quadTex[3] = new double[] { actx, acty };
        wwcVertexCoords.addSegment(tri);
        wwcTextureCoords.addSegment(triTex);
        wwcVertexCoords.addSegment(quad);
        wwcTextureCoords.addSegment(quadTex);
    }

    protected final double[] worldToPixel(double[] world) {
        double[] in = null;
        if (world.length == 2) {
            in = new double[] { world[0], world[1], 0.0 };
        } else {
            in = world;
        }
        double[] out = new double[in.length];
        try {
            latLonToTargetGrid.transform(in, 0, out, 0, 1);
        } catch (TransformException e) {
            return new double[] { Double.NaN, Double.NaN, Double.NaN };
        }
        return out;
    }

    @Override
    public boolean intersects(IExtent extent) {
        return false;
    }

    @Override
    public GridGeometry getSourceGeometry() {
        return imageGeometry;
    }

    @Override
    public GridGeometry getTargetGeometry() {
        return targetGeometry;
    }

    protected abstract SharedCoordinateKey generateKey(
            GridGeometry2D imageGeometry, MathTransform mt);

    protected abstract double[][][] generateWorldCoords(
            GridGeometry2D imageGeometry, MathTransform mt)
            throws TransformException;

    /**
     * A wrapper around {@link GLGeometryObject2D} that detects and removes
     * unreasonably large triangles. The main purpose of this is to remove
     * invalid triangles that form around inconsistencies in a reprojection. For
     * example the opposite of the origin in a stereographic projection is often
     * inverted, resulting in a single triangle covering the entire area of the
     * world. Rather than trying to determine and detect all possibility
     * inconsistencies, it is easier to just remove unreasonably large
     * triangles.
     * 
     * A triangle is considered unreasonably large when a single pixel from the
     * source image would be large enough to cover the entire area of the target
     * screen. Although a valid mesh could theoretically generate a triangle
     * this big, it wouldn't be displaying much useful information. Calculating
     * exactly when a specific triangle is unreasonably large would be slow and
     * complicated so this class should just be considered an estimate.
     */
    private static class UnreasonablyLargeTriangleFilter {

        private final GLGeometryObject2D vertexCoords;

        private final double limit;

        private UnreasonablyLargeTriangleFilter(SharedCoordinateKey key,
                GeneralGridGeometry targetGeometry,
                GridGeometry2D sourceGeometry,
                GLGeometryObject2D vertexCoords) {
            this.vertexCoords = vertexCoords;
            GridEnvelope sourcePixelRange = sourceGeometry.getGridRange();
            GridEnvelope targetPixelRange = targetGeometry.getGridRange();

            /*
             * Everything is a double because the math to calculate the limit
             * can get some really big intermediate values.
             */
            double meshDivisions = key.horizontalDivisions
                    * key.verticalDivisions;
            double sourcePixels = sourcePixelRange.getSpan(0)
                    * sourcePixelRange.getSpan(1);
            double targetPixels = targetPixelRange.getSpan(0)
                    * targetPixelRange.getSpan(1);
            limit = Math.sqrt(targetPixels * sourcePixels / meshDivisions);
        }

        /**
         * Filter a set of coordinates and add any reasonably sized triangles to
         * the GLGeometryObject.
         * 
         * @param segment
         *            a set of coordinates making up a strip of triangles
         */
        public void addSegment(List<double[]> segment) {
            double[][] array = new double[segment.size()][];
            for (int i = 0; i < segment.size(); i += 1) {
                double[] next = segment.get(i);
                boolean valid = true;
                if (i > 0) {
                    valid &= check(next, array[i - 1]);
                }
                if (i > 1) {
                    valid &= check(next, array[i - 2]);
                }
                if (!valid) {
                    double[][] alreadyDone = Arrays.copyOf(array, i);
                    vertexCoords.addSegment(alreadyDone);
                    // reset and continue on.
                    segment = segment.subList(i, segment.size());
                    array = new double[segment.size()][];
                    i = 0;
                }
                array[i] = next;
            }
            vertexCoords.addSegment(array);
        }

        private boolean check(double[] p1, double[] p2) {
            double dx = Math.abs(p1[0] - p2[0]);
            double dy = Math.abs(p1[1] - p2[1]);
            /*
             * To save time, the x distance and the y distance are checked
             * independently rather than calculating a true distance. Although
             * this may allow a few edge cases to slip through, this filter
             * shouldn't generally be considered accurate enough that this
             * matters.
             */
            return dx < limit && dy < limit;
        }
    }

}
