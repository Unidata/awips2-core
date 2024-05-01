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

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.core.exception.VizException;

/**
 * 
 * Provide a mechanism for keeping JOGL GLContext in sync with the SWT GLCanvas
 * context. The JOGL GLContext has most of the logic needed to manage contexts,
 * except for when the context is external. Since our context is created by the
 * SWT GLCanvas the JOGL GLContext does not actually control the active context
 * in GL. This class contains a GLCanvas and a GLContext and keeps the active
 * context in sync between the two.
 * 
 * This class also provides static access to the "Master Context" which is a
 * context with which all other contexts are sharing data. Ideally we would not
 * need to use the master context however there is a bug in some Windows/Intel
 * drivers which causes crashes if a texture is created on a context, then the
 * context is deleted, then the texture is deleted on a different context. To
 * resolve this all textures should be created and deleted on the master
 * context. This is done by calling makeMasterContextCurrent before calling
 * glGenTexture or glDeleteTexture and then calling releaseMasterContext
 * immediately after to restore the previous context.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 17, 2012            bsteffen     Initial creation
 * Sep 15, 2016            mjames@ucar  Prevent context.destroy() to avoid segfault
 *                                      when closing displays and refactor for jogamp
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class GLContextBridge {

    private static GLContextBridge activeContext;

    private static GLContextBridge masterContext;

    private static GLContextBridge previousContext;

    private final GLCanvas canvas;

    private final GLContext context;

    public GLContextBridge(int width, int height) throws VizException {
    	GLProfile glp = GLProfile.getDefault();
    	GLCapabilities glCap = new GLCapabilities(null);
    	if (!GLDrawableFactory.getFactory(glp)
    			.canCreateGLPbuffer(GLProfile.getDefaultDevice(), glp)) {
    		throw new VizException(
    				"Graphics card does not support GLPbuffer and "
    						+ "therefore does not support offscreen rendering.");
    	}
    	
    	GLOffscreenAutoDrawable drawable = GLDrawableFactory.getFactory(glp).
    					createOffscreenAutoDrawable(GLProfile.getDefaultDevice(),
    					glCap, null, width, height);
    	this.context = drawable.createContext(null);
    	this.canvas = null;
    	activeContext = this;
    	releaseContext();
    }

    public GLContextBridge(GLCanvas canvas) {
    	GLProfile glp = GLProfile.getDefault();
        this.canvas = canvas;
        this.canvas.setCurrent();
        this.context = GLDrawableFactory.getFactory(glp).createExternalGLContext();
        activeContext = this;
        releaseContext();
    }

    /**
     * Make this context current for any GL Operations and release any
     * previously active context.
     * 
     * @return true if this context was not previously active and should
     *         therefore be released when you are done performing gl operations
     */
    public boolean makeContextCurrent() {
        if (canvas != null && !canvas.isDisposed()) {
            canvas.setCurrent();
        } else if (canvas != null) {
            throw new RuntimeException(
                    "Cannot make gl context current, GLCanvas is disposed");
        }
        GLContext oldContext = GLContext.getCurrent();
        if (context != oldContext) {
            if (oldContext != null) {
                oldContext.release();
            }
            if (context.makeCurrent() == GLContext.CONTEXT_NOT_CURRENT) {
                throw new RuntimeException(
                        "Cannot make gl context current, Unknown error occured.");
            }
        }
        boolean retVal = (activeContext != this);
        activeContext = this;
        return retVal;
    }

    public void releaseContext() {
        if (context == GLContext.getCurrent()) {
            context.release();
        }
        if (activeContext == this) {
            activeContext = null;
        }
    }

    public void destroyContext() {
        releaseContext();
    }

    /**
     * get the GLData to use when creating a new GLCanvas, data.shareContext
     * will contain the canvas for the "Master Context"
     * 
     * @return
     */
    public static GLData getGLData() {
        return getGLData(true);
    }

    private static GLData getGLData(boolean setContext) {
        GLData data = new GLData();
        data.stencilSize = 1;
        data.depthSize = 1;
        data.doubleBuffer = true;
        if (setContext) {
            data.shareContext = getMasterContext().canvas;
        }
        return data;
    }

    private static GLContextBridge getMasterContext() {
        if (masterContext == null) {
            GLCanvas canvas = new GLCanvas(new Shell(), SWT.NONE,
                    getGLData(false));
            masterContext = new GLContextBridge(canvas);
        }
        return masterContext;
    }

    /**
     * This method makes the shared master context the active contexts, it also
     * stores the current active context to be restored on release.
     * 
     */
    public static void makeMasterContextCurrent() {
        if (masterContext != null) {
            if (activeContext != null) {
                previousContext = activeContext;
                activeContext.releaseContext();
            }
            masterContext.makeContextCurrent();
        }
    }

    /**
     * Releases the master context and restores the context that was active
     * before makeMasterContextCurrent was called.
     */
    public static void releaseMasterContext() {
        if (masterContext != null) {
            masterContext.releaseContext();
            if (previousContext != null) {
                previousContext.makeContextCurrent();
                previousContext = null;
            }
        }
    }
}