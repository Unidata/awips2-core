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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.core.gl.objects.GLVertexBufferObject;

/**
 * GL Geometry object, supports compiling into VBOs. Coordinates passed in will
 * be treated like whatever the geometry object is
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 3, 2011             mschenke    Initial creation
 * Feb 14, 2014 2804       mschenke    Removed ability to clip points outside
 *                                     of a defined IExtent
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class GLGeometryObject2D {

    public static class GLGeometryObjectData {

        protected int geometryType;

        protected int coordType;

        public boolean manageIndicies = true;

        public boolean mutable = false;

        public GLGeometryObjectData(int geometryType, int coordType) {
            this.geometryType = geometryType;
            this.coordType = coordType;
        }

    }

    protected static enum State {
        INVALID, MUTABLE, COMPILED, COMPILED_HIGH_END;
    }

    protected GrowableBuffer<FloatBuffer> coordBuffer;

    private int initialPoints = 50000;

    protected List<Integer> indicies;

    protected IntBuffer compiledLengths;

    protected IntBuffer compiledIndicies;

    protected int points;

    protected GLVertexBufferObject vbo;

    protected State state;

    protected GLGeometryObjectData data;

    public GLGeometryObject2D(GLGeometryObjectData data) {
        this.data = new GLGeometryObjectData(data.geometryType, data.coordType);
        this.data.manageIndicies = data.manageIndicies;
        this.data.mutable = data.mutable;
        initialize();
    }

    private void initialize() {
        coordBuffer = null;
        if (data.manageIndicies) {
            indicies = new ArrayList<Integer>(100);
        }
        compiledIndicies = null;
        points = 0;
        vbo = null;
        state = State.MUTABLE;
    }

    public void compile(GL gl) throws VizException {
        // We will use glMultiDrawArrays if the cardSupportsHighEndFeatures, so
        // we will keep track of a lenghts buffer
        state = State.COMPILED;
        int add = 1;
        if (GLCapabilities.getInstance(gl).cardSupportsHighEndFeatures) {
            state = State.COMPILED_HIGH_END;
            add = 0;
        }

        try {
            if (data.manageIndicies) {
                compiledIndicies = IntBuffer.allocate((indicies.size() + add));
                compiledIndicies.position(0);
                for (int i = 0; i < indicies.size(); ++i) {
                    compiledIndicies.put(indicies.get(i));
                }

                if (add == 1) {
                    compiledIndicies.put(points);
                } else {
                    compiledLengths = IntBuffer.allocate(indicies.size());
                    compiledLengths.position(0);
                    int size = compiledIndicies.capacity();
                    for (int i = 0; i < size - 1; ++i) {
                        compiledLengths.put(compiledIndicies.get(i + 1)
                                - compiledIndicies.get(i));
                    }
                    compiledLengths
                            .put(points
                                    - compiledIndicies.get(compiledIndicies
                                            .capacity() - 1));
                }
            }

            if (coordBuffer != null && points > 0) {
                // Trim to size
                FloatBuffer data = coordBuffer.getBuffer();
                data.position(0);
                FloatBuffer copy = ByteBuffer
                        .allocateDirect(points * pointsPerCoordinate() * 4)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                for (int i = 0; i < copy.capacity(); ++i) {
                    copy.put(data.get());
                }

                // clear the error bit.
                gl.glGetError();

                // generate vbo
                vbo = new GLVertexBufferObject(this);

                // verify successful
                if (!vbo.isValid()) {
                    vbo = null;
                    throw new VizException("Error compiling wireframe shape, "
                            + "could not generate vertex buffer object");
                }

                // bind and load
                vbo.bind(gl, GL.GL_ARRAY_BUFFER);
                gl.glBufferData(GL.GL_ARRAY_BUFFER, copy.capacity() * 4,
                        copy.rewind(), GL.GL_STATIC_DRAW);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

                // dispose of coords
                coordBuffer.dispose();
                coordBuffer = null;
                indicies = null;
            }
        } catch (Throwable t) {
            state = State.INVALID;
            throw new VizException("Error compiling geometry", t);
        }
    }

    public boolean isMutable() {
        return state == State.MUTABLE;
    }

    public boolean isDrawable() {
        return !((state == State.INVALID) || (state == State.MUTABLE
                && data.manageIndicies && (indicies == null || indicies
                .isEmpty())));
    }

    public synchronized void dispose() {
        if (coordBuffer != null) {
            coordBuffer.dispose();
            coordBuffer = null;
        }

        indicies = null;
        compiledIndicies = null;
        compiledLengths = null;
        state = State.INVALID;

        if (vbo != null && vbo.isValid()) {
            vbo.dispose();
        }
    }

    public synchronized void reset() {
        dispose();
        initialize();
    }

    public synchronized void addSegment(double[][] glCoordinates) {
        if (!isMutable()) {
            throw new RuntimeException(
                    "Cannot add new coordinates, shape has already been compiled.  Must initialize shape to modify");
        }

        int verticesNeeded = glCoordinates.length * pointsPerCoordinate();
        if (coordBuffer == null) {
            FloatBuffer vertexBufferTmp = null;
            int sz = Math.max(initialPoints, verticesNeeded);

            if (data.mutable) {
                ByteBuffer vertexBufferAsBytes = ByteBuffer.allocateDirect(sz
                        * pointsPerCoordinate() * 4);
                vertexBufferAsBytes.order(ByteOrder.nativeOrder());
                vertexBufferTmp = vertexBufferAsBytes.asFloatBuffer();
            } else {
                vertexBufferTmp = FloatBuffer.allocate(sz
                        * pointsPerCoordinate());
            }
            vertexBufferTmp.position(0);
            coordBuffer = new GrowableBuffer<FloatBuffer>(vertexBufferTmp);
        }

        coordBuffer.position(points * pointsPerCoordinate());

        if (verticesNeeded > 0) {
            coordBuffer.ensureCapacity(verticesNeeded);
        }

        int idx = points;
        for (double[] coordinate : glCoordinates) {
            addToBuffer(coordBuffer, coordinate);
            points += 1;
        }
        if (points != idx && data.manageIndicies
                && (data.geometryType != GL.GL_LINES || indicies.isEmpty())) {
            indicies.add(idx);
        }
    }

    public void allocate(int points) {
        if (coordBuffer == null) {
            initialPoints = points;
        } else {
            coordBuffer.allocate(points * pointsPerCoordinate());
        }
    }

    public synchronized void paint(GL gl) throws VizException {
        GLGeometryPainter.paintGeometries(gl, this);
    }

    // 2D specific functions, override for 3D support

    protected void addToBuffer(GrowableBuffer<FloatBuffer> vb, double[] point) {
        vb.put((float) point[0]);
        vb.put((float) point[1]);
    }

    protected int pointsPerCoordinate() {
        return 2;
    }

}
