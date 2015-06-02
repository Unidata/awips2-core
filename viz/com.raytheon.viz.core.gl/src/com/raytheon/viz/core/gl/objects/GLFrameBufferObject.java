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
package com.raytheon.viz.core.gl.objects;

import com.jogamp.opengl.GL2;

/**
 * 
 * A simple wrapper around a GL frameBuffer id that manages creation and
 * disposal of frameBuffer ids.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 17, 2012            bsteffen    Initial creation
 * Jan  9, 2014 2680       mschenke    Added default error message handling
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class GLFrameBufferObject extends GLIdWrapper {

    /**
     * Create a new frameBuffer id in gl and register this frameBuffer to be
     * disposed when parent is garbage collected.
     * 
     * @param parent
     *            - the object that will be using the texture.
     * @param gl
     */
    public GLFrameBufferObject(Object parent) {
        super(parent);
    }

    /**
     * Create a new frameBuffer id in gl
     * 
     * @param gl
     */
    public GLFrameBufferObject() {
        super();
    }

    @Override
    protected void genId(GL2 gl, int[] arr) {
        gl.glGenFramebuffers(1, arr, 0);
    }

    @Override
    protected void deleteId(GL2 gl, int[] arr) {
        gl.glDeleteFramebuffers(1, arr, 0);

    }

    public void bind(GL2 gl) {
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, id);
    }

    public String checkStatus(GL2 gl) {
        String errorMessage = null;

        switch (gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER)) {
        case GL2.GL_FRAMEBUFFER_COMPLETE: {
            // Everything is ok.
            break;
        }
        case GL2.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT: {
            errorMessage = "Error: Framebuffer incomplete, fbo attachement is NOT complete";
            break;
        }
        case GL2.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: {
            errorMessage = "Error: Framebuffer incomplete, no image is attached to FBO";
            break;
        }
        case GL2.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS: {
            errorMessage = "Error: Framebuffer incomplete, attached images have different dimensions";
            break;
        }
        case GL2.GL_FRAMEBUFFER_INCOMPLETE_FORMATS: {
            errorMessage = "Error: Framebuffer incomplete, color attached images have different internal formats";
            break;
        }
        case GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER: {
            errorMessage = "Error: Framebuffer incomplete, draw buffer";
            break;
        }
        case GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER: {
            errorMessage = "Error: Framebuffer incomplete, read buffer";
            break;
        }
        case GL2.GL_FRAMEBUFFER_UNSUPPORTED: {
            errorMessage = "Error: Framebuffer not supported by hardware/drivers";
            break;
        }
        default: {
            errorMessage = "Framebuffer is not complete, unknown reason";
            break;
        }
        }
        return errorMessage;
    }
}
