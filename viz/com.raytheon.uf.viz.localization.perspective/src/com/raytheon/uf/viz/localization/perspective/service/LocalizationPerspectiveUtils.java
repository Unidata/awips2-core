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
package com.raytheon.uf.viz.localization.perspective.service;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.viz.ui.VizWorkbenchManager;

/**
 * Utility class for the localization perspective
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 02, 2010           mschenke  Initial creation
 * Nov 27, 2013           mschenke  Moved editor based utility methods to
 *                                  perspective project
 * Jun 03, 2016  4907     mapeters  Attempt to restore views when getting
 *                                  service, allow activation of view that
 *                                  contributes service
 * Aug 11, 2016  5816     randerso  Added openLocalizationFile() (moved from GFE
 *                                  specific code)
 * 
 * </pre>
 * 
 * @author mschenke
 */

public class LocalizationPerspectiveUtils {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalizationPerspectiveUtils.class);

    public static final String ID_PERSPECTIVE = "com.raytheon.uf.viz.ui.LocalizationPerspective"; //$NON-NLS-1$

    /**
     * Get a localization service object from the active window
     * 
     * @return the localization service or null if none available
     */
    public static ILocalizationService getService() {
        return getService(VizWorkbenchManager.getInstance().getCurrentWindow());
    }

    /**
     * Get a localization service object from the given window
     * 
     * @param window
     *            the window to check
     * @return the localization service or null if none available
     */
    public static ILocalizationService getService(IWorkbenchWindow window) {
        return getService(window != null ? window.getActivePage() : null);
    }

    /**
     * Get a localization service object from the specified workbench page
     * 
     * @param page
     *            the page to search
     * @return the localization service or null if none available
     */
    public static ILocalizationService getService(IWorkbenchPage page) {
        return getService(page, false);
    }

    /**
     * Get a localization service object from the specified workbench page
     * 
     * @param page
     *            the page to search
     * @param activateServicePart
     *            whether or not the part providing the localization service
     *            should be activated
     * @return the localization service or null if none available
     */
    private static ILocalizationService getService(IWorkbenchPage page,
            boolean activateServicePart) {
        if (page != null) {
            for (IViewReference ref : page.getViewReferences()) {
                IViewPart part = ref.getView(true);
                if (part != null) {
                    ILocalizationService service = (ILocalizationService) part
                            .getAdapter(ILocalizationService.class);
                    if (service != null) {
                        if (activateServicePart) {
                            page.activate(part);
                        }
                        return service;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Function to make the localization perspective active
     * 
     * @return the localization service or null if none available
     */
    public static ILocalizationService changeToLocalizationPerspective() {
        try {
            IWorkbenchPage page = PlatformUI.getWorkbench().showPerspective(
                    ID_PERSPECTIVE,
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow());
            // Get the service, activating the part that contributes the service
            return getService(page, true);
        } catch (WorkbenchException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error switching to localization perspective", e);
        }

        return null;
    }

    /**
     * Open a file in the localization perspective
     * 
     * @param file
     *            the file to open
     */
    public static void openLocalizationFile(LocalizationFile file) {
        ILocalizationService service = changeToLocalizationPerspective();
        if (service != null) {
            service.refresh(file);
            service.openFile(file);
            service.selectFile(file);
        }
    }

}
