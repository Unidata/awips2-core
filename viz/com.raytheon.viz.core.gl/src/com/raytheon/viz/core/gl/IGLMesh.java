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

import com.raytheon.uf.viz.core.IMesh;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.PaintStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.core.gl.ext.imaging.AbstractGLImagingExtension;

/**
 * Interface required for a mesh to work with
 * {@link AbstractGLImagingExtension}. Most implementations will want to
 * asynchronously calculate matching {@link GLGeometryObject2D} objects for the
 * vertex and texture coordinates and render them using
 * {@link GLGeometryPainter}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Oct 25, 2017  6387     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public interface IGLMesh extends IMesh {

    /**
     * This method should draw texture and vertex coordinates to display an
     * image in the correct location on the target. This method is expected to
     * be very fast since multiple meshes may be involved in rendering a single
     * frame. If the mesh is calculated asynchronously and it is incomplete then
     * this method can return {@link PaintStatus#REPAINT}, which will cause the
     * frame to be rendered again later.
     * 
     * @param target
     *            the target to paint on
     * @param paintProps
     *            the properties being used for this render.
     * @return {@link PaintStatus#PAINTED} on success,
     *         {@link PaintStatus#REPAINT} if the mesh is still being calculated
     *         or {@link PaintStatus#ERROR} if an error prevents the mesh from
     *         painting completely.
     * @throws VizException
     *             if an internal error occurs that leaves the gl context in an
     *             unknown or broken state.
     */
    public PaintStatus paint(IGLTarget target, PaintProperties paintProps)
            throws VizException;
}
