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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.raytheon.uf.viz.core.AbstractGraphicsFactoryAdapter;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IView;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.core.gl.internal.GLTarget;
import com.raytheon.viz.core.gl.internal.GLView2D;

/**
 * 
 * GL Graphics adapter for 2D.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 19, 2010            mschenke     Initial creation
 * Nov 14, 2016 5976       bsteffen    Remove deprecated methods
 * 
 * </pre>
 * 
 * @author mschenke
 */
public class GLFactoryAdapter extends AbstractGraphicsFactoryAdapter {

    public GLFactoryAdapter() {

    }

    @Override
    public IGraphicsTarget constructTarget(Canvas canvas, float width,
            float height) throws VizException {
        return new GLTarget(canvas, width, height);
    }

    @Override
    public IView constructView() {
        return new GLView2D();
    }

    @Override
    public Canvas constrcutCanvas(Composite canvasComp) throws VizException {
        GLCanvas canvas;
        GLData data = GLContextBridge.getGLData();
        canvas = new GLCanvas(canvasComp, SWT.NONE, data);

        canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        return canvas;
    }

}
