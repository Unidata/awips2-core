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

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.ISaveHandler;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import com.raytheon.uf.viz.core.ProgramArguments;
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
 * Dec 23, 2015   5189      bsteffen    Add custom save handler.
 * Jan 05, 2016   5193      bsteffen    Move perspective listener activation to the workbench advisor.
 * Jan 12, 2016   5232      njensen     Removed code that doesn't work in Eclipse 4
 * Jun 06, 2016   5195      bsteffen    Prevent tiny CAVE.
 * Aug 05, 2016   5764      bsteffen    Move window placement logic to the WindowPlacementProcessor.
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1
 */
public class VizWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

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

        Point minimum = new Point(400, 400);
        IWorkbenchWindow window = configurer.getWindow();
        Shell shell = window.getShell();
        shell.setMinimumSize(minimum);

        MWindow model = configurer.getWindow().getService(MWindow.class);
        int width = model.getWidth();
        int height = model.getHeight();
        /* The WindowPlacementProcessor should have set reasonable values. */
        if (width >= minimum.x && height >= minimum.y) {
            configurer.setInitialSize(new Point(width, height));
        }

        OpenPerspectiveList.getInstance(window);
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

        VizPerspectiveListener listener = new VizPerspectiveListener(window,
                VizActionBarAdvisor.getInstance(window).getStatusLine());
        window.addPerspectiveListener(listener);
        window.addPageListener(AbstractVizPerspectiveManager.pageListener);
        IWorkbenchPage page = window.getActivePage();
        page.addPartListener(AbstractVizPerspectiveManager.partListener);
        
        IEclipseContext windowContext = window.getService(IEclipseContext.class);
        ISaveHandler defaultSaveHandler = windowContext.get(ISaveHandler.class);
        ISaveHandler localSaveHandler = new VizSaveHandler(defaultSaveHandler);
        windowContext.set(ISaveHandler.class, localSaveHandler);

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#postWindowCreate()
     */
    @Override
    public void postWindowCreate() {
        super.postWindowCreate();
        
        ProgramArguments args = ProgramArguments.getInstance();
        Integer monitor = args.getInteger("-monitor");
        Integer width = args.getInteger("-width");
        Integer height = args.getInteger("-height");
        Integer x = args.getInteger("-x");
        Integer y = args.getInteger("-y");
        /* if we've overridden the window size/position don't save it on exit */
        if (monitor != null || height != null || width != null || x != null
                || y != null) {
            getWindowConfigurer().getWorkbenchConfigurer().setSaveAndRestore(
                    false);
        }
    }

}
