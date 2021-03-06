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
package com.raytheon.viz.ui.perspectives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener4;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.viz.ui.VizWorkbenchManager;

/**
 * Perspective listener, handles saving of editors and reloading of editors for
 * each perspective TODO: One instance of VizPerspectiveListener per
 * WorkbenchWindow
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 21, 2010            mschenke    Initial creation
 * Mar 21, 2013       1638 mschenke    Added method to get managed perspectives
 * Aug 11, 2014 3480       bclement    added log message in perspectiveOpened()
 * Oct 20, 2015 4749       dgilling    Fix bug in perspectiveClosed that caused
 *                                     unwanted GFE startup dialog to pop up.
 * Dec 14, 2015 5193       bsteffen    Eclipse 4: update editor hide/restore on
 *                                     perspective switch.
 * Jan 15, 2015 5054       randerso    Fix NullPointerException when called from outside
 *                                     a CAVE environment (e.g. prototype, unit test)
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class VizPerspectiveListener implements IPerspectiveListener4 {

    private static final IUFStatusHandler log = UFStatus
            .getHandler(VizPerspectiveListener.class);

    private static final String PERSPECTIVE_MANAGER_EXTENSION = "com.raytheon.viz.ui.perspectiveManager";

    private static final String PERSPECTIVE_ID = "perspectiveId";

    private static final String NAME_ID = "name";

    private static final String CLASS_ID = "class";

    private static final List<IConfigurationElement> configurationElements = new ArrayList<IConfigurationElement>();

    private static final Set<String> managedPerspectives = new LinkedHashSet<String>();

    static {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        if (registry != null) {
            IExtensionPoint point = registry
                    .getExtensionPoint(PERSPECTIVE_MANAGER_EXTENSION);
            if (point != null) {
                IExtension[] extensions = point.getExtensions();

                for (IExtension ext : extensions) {
                    for (IConfigurationElement element : ext
                            .getConfigurationElements()) {
                        configurationElements.add(element);
                        managedPerspectives.add(element
                                .getAttribute(PERSPECTIVE_ID));
                    }
                }
            }
        }
    }

    /** The perspective map */
    private Map<String, AbstractVizPerspectiveManager> managerMap = null;
    
    private AbstractVizPerspectiveManager activeManager;
    
    private static Map<IWorkbenchWindow, VizPerspectiveListener> instanceMap = new HashMap<IWorkbenchWindow, VizPerspectiveListener>();

    public VizPerspectiveListener(IWorkbenchWindow window,
            IStatusLineManager statusLine) {
        managerMap = new HashMap<String, AbstractVizPerspectiveManager>();
        for (IConfigurationElement cfg : configurationElements) {
            try {
                String name = cfg.getAttribute(NAME_ID);
                String perspective = cfg.getAttribute(PERSPECTIVE_ID);
                AbstractVizPerspectiveManager mgr = (AbstractVizPerspectiveManager) cfg
                        .createExecutableExtension(CLASS_ID);
                mgr.setPerspectiveWindow(window);
                mgr.setStatusLineManager(statusLine);
                mgr.setPerspectiveId(perspective);
                managerMap.put(perspective, mgr);
            } catch (CoreException e) {
                throw new RuntimeException(
                        "Error loading perspective managers for "
                                + cfg.getAttribute("class"),
                        e);
            }
        }
        instanceMap.put(window, this);
    }

    /**
     * Get the perspecive manager for the active window's active perspective
     * 
     * @return
     */
    public static AbstractVizPerspectiveManager getCurrentPerspectiveManager() {
        VizPerspectiveListener listener = getInstance();
        if (listener != null) {
            return listener.getActivePerspectiveManager();
        }
        return null;
    }

    /**
     * Returns all perspectives that are managed by an
     * {@link AbstractVizPerspectiveManager}
     * 
     * @return
     */
    public static Collection<String> getManagedPerspectives() {
        return new ArrayList<String>(managedPerspectives);
    }

    /**
     * Get the perspective listener for the active window
     * 
     * @return the listener
     */
    public static VizPerspectiveListener getInstance() {
        return getInstance(VizWorkbenchManager.getInstance().getCurrentWindow());
    }

    /**
     * Get the perspective listener for the given window
     * 
     * @param window
     * @return
     */
    public static VizPerspectiveListener getInstance(IWorkbenchWindow window) {
        return instanceMap.get(window);
    }

    /**
     * Get the current perspective manager on the active window
     * 
     * @return the manager or null if none exists
     */
    public AbstractVizPerspectiveManager getActivePerspectiveManager() {
        return activeManager;
    }

    /**
     * Get the perspective manager for the given perspective id
     * 
     * @param perspectiveId
     * @return
     */
    public AbstractVizPerspectiveManager getPerspectiveManager(
            String perspectiveId) {
        return managerMap.get(perspectiveId);
    }

    @Override
    public void perspectiveClosed(IWorkbenchPage page,
            IPerspectiveDescriptor perspective) {
        String pid = perspective.getId();
        AbstractVizPerspectiveManager manager = managerMap.get(pid);
        if (manager != null) {
            if(manager == activeManager){
                activeManager = null;
            }
            manager.close();
        }
    }

    @Override
    public void perspectiveActivated(IWorkbenchPage page,
            IPerspectiveDescriptor perspective) {
        String pid = perspective.getId();
        AbstractVizPerspectiveManager manager = managerMap.get(pid);
        if (manager != null) {
            if (activeManager != manager) {
                if (activeManager != null) {
                    /*
                     * Eclipse 4 does not consistently call
                     * perspectiveDeactivated so always deactivate before
                     * activating.
                     */
                    activeManager.deactivate();
                    activeManager = null;
                }
                activeManager = manager;
                manager.activate();                
            }
        }
    }

    @Override
    public void perspectiveDeactivated(IWorkbenchPage page,
            IPerspectiveDescriptor perspective) {
        AbstractVizPerspectiveManager manager = managerMap
                .get(perspective.getId());
        if (manager != null) {
            if(activeManager == manager){
                activeManager = null;
            }
            manager.deactivate();
        }
    }

    
    // Noop functions
    
    @Override
    public void perspectiveOpened(IWorkbenchPage page,
            IPerspectiveDescriptor perspective) {
        log.info("Opened perspective: " + perspective.getId());
    }
    
    @Override
    public void perspectivePreDeactivate(IWorkbenchPage page,
            IPerspectiveDescriptor perspective) {
        /*
         * As of eclipse 4.5.1 this method is never
         * called(https://bugs.eclipse.org/bugs/show_bug.cgi?id=408309).
         */
    }

    @Override
    public void perspectiveSavedAs(IWorkbenchPage page,
            IPerspectiveDescriptor oldPerspective,
            IPerspectiveDescriptor newPerspective) {

    }

    @Override
    public void perspectiveChanged(IWorkbenchPage page,
            IPerspectiveDescriptor perspective,
            IWorkbenchPartReference partRef, String changeId) {
    }

    @Override
    public void perspectiveChanged(IWorkbenchPage page,
            IPerspectiveDescriptor perspective, String changeId) {
    }

}
