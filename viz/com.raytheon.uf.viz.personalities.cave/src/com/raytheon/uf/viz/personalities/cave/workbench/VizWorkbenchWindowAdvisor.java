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

package com.raytheon.uf.viz.personalities.cave.workbench;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import com.raytheon.uf.viz.core.ProgramArguments;
import com.raytheon.viz.ui.dialogs.ModeListener;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;
import com.raytheon.viz.ui.statusline.VizActionBarAdvisor;

/**
 * Workbench window advisor
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	--------------------------
 * 7/1/06                   chammack    Initial Creation.
 * Oct 21, 2008   #1450     randerso    Fixed to support multipane editors
 * Oct 27, 2015   #5053     randerso    Changed initial cave window size to be more intelligntly
 *                                      computed instead of just maximized.
 *                                      Added command line parameters to allow window size
 *                                      and location to be specified.
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1
 */
public class VizWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

    private VizPerspectiveListener listener;

    private boolean firstTime = true;

    /**
     * Constructor
     * 
     * @param configurer
     */
    public VizWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#preWindowOpen()
     */
    @Override
    public void preWindowOpen() {
        super.preWindowOpen();
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setShowProgressIndicator(true);
        configurer.setShowPerspectiveBar(true);
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);

        OpenPerspectiveList.getInstance(configurer.getWindow());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.application.WorkbenchWindowAdvisor#createActionBarAdvisor
     * (org.eclipse.ui.application.IActionBarConfigurer)
     */
    @Override
    public ActionBarAdvisor createActionBarAdvisor(
            IActionBarConfigurer configurer) {
        return new VizActionBarAdvisor(configurer);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#postWindowOpen()
     */
    @Override
    public void postWindowOpen() {
        super.postWindowOpen();

        final IWorkbenchWindow window = super.getWindowConfigurer().getWindow();

        listener = new VizPerspectiveListener(window, VizActionBarAdvisor
                .getInstance(window).getStatusLine());
        window.addPerspectiveListener(listener);
        window.addPageListener(AbstractVizPerspectiveManager.pageListener);
        IWorkbenchPage page = window.getActivePage();
        page.addPartListener(AbstractVizPerspectiveManager.partListener);

        IPerspectiveDescriptor perspective = page.getPerspective();
        if (perspective != null) {
            listener.perspectiveActivated(page, perspective);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#postWindowCreate()
     */
    @Override
    public void postWindowCreate() {
        super.postWindowCreate();

        IWorkbenchWindow window = super.getWindowConfigurer().getWindow();

        Shell shell = window.getShell();
        shell.setMinimumSize(new Point(400, 400));

        Display display = shell.getDisplay();
        Monitor[] monitors = display.getMonitors();

        boolean save = true;
        ProgramArguments args = ProgramArguments.getInstance();
        Integer monitor = args.getInteger("-monitor");
        if (monitor == null) {
            monitor = 0;

            Point cursor = display.getCursorLocation();
            for (int i = 0; i < monitors.length; i++) {
                if (monitors[i].getBounds().contains(cursor)) {
                    monitor = i;
                    break;
                }
            }
        } else {
            save = false;
        }

        if (monitor >= monitors.length) {
            monitor = monitors.length - 1;
        }
        Rectangle bounds = monitors[monitor].getBounds();

        Integer width = args.getInteger("-width");
        if (width == null) {
            if (firstTime) {
                width = Math.max(1200, (bounds.height * 5) / 4);
            } else {
                // use saved width unless greater than monitor width
                width = Math.min(bounds.width, shell.getSize().x);
            }
        } else {
            save = false;
        }

        Integer height = args.getInteger("-height");
        if (height == null) {
            if (firstTime) {
                height = bounds.height;
            } else {
                // use saved height unless greater than monitor height
                height = Math.min(bounds.height, shell.getSize().y);
            }
        } else {
            save = false;
        }

        shell.setSize(width, height);

        Integer x = args.getInteger("-x");
        Integer y = args.getInteger("-y");
        if (x != null && y != null) {
            save = false;
            shell.setLocation(x + bounds.x, y + bounds.y);
        } else if (firstTime) {
            shell.setLocation(bounds.x + bounds.width - width, bounds.y);
        } else {
            // scale saved location to selected monitor size
            Point loc = shell.getLocation();
            for (Monitor m : monitors) {
                Rectangle b = m.getBounds();
                if (b.contains(loc)) {
                    x = (((loc.x - b.x) * bounds.width) / b.width) + bounds.x;
                    y = (((loc.y - b.y) * bounds.height) / b.width) + bounds.y;
                    shell.setLocation(x, y);
                    break;
                }
            }
        }

        /* if we've overridden the window size/position don't save it on exit */
        if (!save) {
            getWindowConfigurer().getWorkbenchConfigurer().setSaveAndRestore(
                    false);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.application.WorkbenchWindowAdvisor#createWindowContents
     * (org.eclipse.swt.widgets.Shell)
     */
    @Override
    public void createWindowContents(Shell shell) {
        super.createWindowContents(shell);

        // Gets the main shell and colors the shell if the mode is set to
        // practice or training
        new ModeListener(shell);
    }

    @Override
    public IStatus restoreState(IMemento memento) {
        // If we have state to restore then this isn't our first time
        firstTime = false;
        return super.restoreState(memento);
    }

}
