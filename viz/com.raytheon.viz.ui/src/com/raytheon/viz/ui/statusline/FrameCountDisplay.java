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
package com.raytheon.viz.ui.statusline;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchWindow;

import com.raytheon.uf.viz.core.VizConstants;
import com.raytheon.uf.viz.core.globals.IGlobalChangedListener;
import com.raytheon.uf.viz.core.globals.VizGlobalsManager;

/**
 * Status line display to show frame count
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 23, 2010            mschenke    Initial creation
 * Mar 24, 2016  5232      njensen     Fix label coloring for test mode
 * Oct 02, 2019  69438     ksunil      work to add current frame number in the label
 * </pre>
 *
 * @author mschenke
 */

public class FrameCountDisplay extends ContributionItem {

    private Label frameCountLabel;

    private Composite layout;
    
    private Number frameCount;

    private Number currentFrameNum;

    private IWorkbenchWindow window;
    
    private FrameNumberListener frameNumListener = new FrameNumberListener();

    private FrameCountListener frameCountListener = new FrameCountListener();

    public FrameCountDisplay(IWorkbenchWindow window) {
        this.window = window;
        frameCount = (Number) VizGlobalsManager.getInstance(window)
                .getPropery(VizConstants.FRAME_COUNT_ID);
        VizGlobalsManager.addListener(VizConstants.FRAME_COUNT_ID,
                frameCountListener);
        VizGlobalsManager.addListener(VizConstants.FRAME_NUM_IN_LOOP,
                frameNumListener);
    }

    @Override
    public void fill(Composite parent) {
        layout = new Composite(parent, SWT.NONE);

        layout.setLayout(new GridLayout(2, false));

        Label label = new Label(layout, SWT.NONE);
        label.setText("Frames:");
        label.setForeground(parent.getForeground());

        frameCountLabel = new Label(layout, SWT.BORDER);
        frameCountLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                true));
        frameCountLabel.setForeground(parent.getForeground());
        frameCountLabel.setBackground(parent.getBackground());
        update();
    }

    @Override
    public void update() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                updateFrameText();
            }
        });
    }

    private void updateFrameText() {
        if (frameCountLabel != null) {
            if (frameCount != null && currentFrameNum != null) {
                frameCountLabel
                        .setText(Math.max(currentFrameNum.intValue() + 1, 1)
                                + "/" + Math.max(frameCount.intValue(), 1));
            } else {
                frameCountLabel.setText("1/1");
            }
            window.getShell().layout(true, true);
            frameCountLabel.pack();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        VizGlobalsManager.removeListener(VizConstants.FRAME_COUNT_ID,
                frameCountListener);
        VizGlobalsManager.removeListener(VizConstants.FRAME_NUM_IN_LOOP,
                frameNumListener);
    }
    
    private class FrameNumberListener implements IGlobalChangedListener {
        public void updateValue(IWorkbenchWindow changedWindow, Object value) {
            currentFrameNum = (Number) value;
            update();
        }
    }
    
    private class FrameCountListener implements IGlobalChangedListener {
        public void updateValue(IWorkbenchWindow changedWindow, Object value) {
            if (changedWindow == window) {
                frameCount = (Number) value;
                update();
            }
        }
    }

}
