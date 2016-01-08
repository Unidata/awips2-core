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
package com.raytheon.viz.ui.views;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * @deprecated all functionality that was previously available through this
 *             class is now available from the {@link IWorkbenchPage} and
 *             {@link DetachPart}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 11, 2012            bsteffen     Initial creation
 * Aug 26, 2014 3539       bclement     fixed NPE when floatView() is called for a fast view
 * Jan 07, 2016 5190       bsteffen     Deprecate
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
@Deprecated
public class CaveWorkbenchPageManager {

    private static Map<IWorkbenchPage, CaveWorkbenchPageManager> instanceMap = new HashMap<IWorkbenchPage, CaveWorkbenchPageManager>();

    public static CaveWorkbenchPageManager getInstance(IWorkbenchPage page) {
        synchronized (instanceMap) {
            CaveWorkbenchPageManager instance = instanceMap.get(page);
            if (instance == null) {
                instance = new CaveWorkbenchPageManager(page);
                instanceMap.put(page, instance);
            }
            return instance;
        }
    }

    public static CaveWorkbenchPageManager getActiveInstance() {
        synchronized (instanceMap) {
            return getInstance(PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage());
        }
    }

    private final IWorkbenchPage workbenchPage;

    private CaveWorkbenchPageManager(IWorkbenchPage workbenchPage) {
        this.workbenchPage = workbenchPage;
    }

    public IWorkbenchPage getWorkbenchPage() {
        return workbenchPage;
    }

    @Deprecated
    public IViewReference[] getViewReferences() {
        return workbenchPage.getViewReferences();
    }

    @Deprecated
    public IViewPart showView(String viewID) throws PartInitException {
        return showView(viewID, null, IWorkbenchPage.VIEW_ACTIVATE);
    }

    @Deprecated
    public IViewPart showView(String viewId, String secondaryId, int mode)
            throws PartInitException {
        return workbenchPage.showView(viewId, secondaryId, mode);
    }

    @Deprecated
    public void activate(IViewPart view) {
        workbenchPage.activate(view);
    }

    @Deprecated
    public void bringToTop(IViewPart view) {
        workbenchPage.bringToTop(view);
    }

    @Deprecated
    public IViewReference findViewReference(String viewId) {
        return workbenchPage.findViewReference(viewId);
    }

    @Deprecated
    public IViewReference findViewReference(String viewId, String secondaryId) {
        return workbenchPage.findViewReference(viewId, secondaryId);
    }

    @Deprecated
    public void hideView(IViewReference view) {
        workbenchPage.hideView(view);
    }


    @Deprecated
    public void floatView(IViewPart viewPart) {
        DetachPart.detach(viewPart);
    }

    @Deprecated
    public void dockView(IViewPart viewPart) {
        DetachPart.attach(viewPart);
    }

}
