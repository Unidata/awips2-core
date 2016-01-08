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
package com.raytheon.uf.viz.ui.menus.widgets;

import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.menus.MenuUtil;

import com.raytheon.uf.common.menus.xml.CommonAbstractMenuContribution;
import com.raytheon.uf.common.menus.xml.VariableSubstitution;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.VariableSubstitutionUtil;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.jobs.JobPool;
import com.raytheon.uf.viz.ui.menus.xml.IContribItemProvider;
import com.raytheon.uf.viz.ui.menus.xml.MenuXMLMap;

/**
 * Provides a submenu capability. The current implementation uses a background
 * thread to load the submenu contents. When the parent menu item of the submenu
 * is first displayed the background job is started and will populate the
 * submenu. Generally it is populated before a user can possibly open the menu
 * however the performance depends greatly on the complexity of the contribution
 * items in the submenu.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- -----------------------------------------
 * Mar 26, 2009           chammack    Initial creation
 * May 08, 2013  1978     bsteffen    Perform variable substitution on subMenu
 *                                    IDs.
 * Dec 11, 2013  2602     bsteffen    Update MenuXMLMap.
 * Dec 21, 2015  5194     bsteffen    Restructure threading
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */
public class SubmenuContributionItem extends MenuManager {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(SubmenuContributionItem.class);

    private static JobPool getContributionItemsJob = new JobPool(
            "Preparing Menus", 1, true);

    private CommonAbstractMenuContribution[] contribs;

    protected VariableSubstitution[] subs;

    protected Set<String> removals;

    /*
     * Track if addContributedItems is done, the menu service should not be used
     * until after addContributedItems is done
     */
    protected volatile boolean doneAddingContribs;

    /* Track if addMenuServiceItems has run, it should not be run twice. */
    protected volatile boolean doneAddingMenuService;

    /**
     * 
     * @param includeSubstitutions
     * @param name
     * @param ci
     * @param removals
     * @param mListener
     */
    public SubmenuContributionItem(VariableSubstitution[] includeSubstitutions,
            String id, String name, CommonAbstractMenuContribution[] ci,
            Set<String> removals) {
        super(processSubstitution(includeSubstitutions, name),
                processSubstitution(includeSubstitutions, id));
        this.subs = includeSubstitutions;
        this.contribs = ci;
        this.removals = removals;
        this.addMenuListener(new IMenuListener() {

            @Override
            public void menuAboutToShow(IMenuManager manager) {
                if (doneAddingMenuService || !doneAddingContribs) {
                    return;
                }
                doneAddingMenuService = true;
                addMenuServiceItems();
            }

        });
    }

    private static String processSubstitution(
            VariableSubstitution[] includeSubstitutions, String name) {
        if (name != null && includeSubstitutions != null
                && includeSubstitutions.length > 0) {
            Map<String, String> map = VariableSubstitution
                    .toMap(includeSubstitutions);
            try {
                name = VariableSubstitutionUtil.processVariables(name, map);
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error during menu substitution", e);
            }
        }
        return name;
    }

    /**
     * Method to fill the parent menu with a cascading menu item that will
     * create this submenu.
     */
    @Override
    public void fill(Menu parent, int index) {
        if (!doneAddingContribs) {
            getContributionItemsJob.schedule(new ContributeItemsRunnable());
        }
        super.fill(parent, index);
    }

    /**
     * Process contribs and add new IContributionItems to this. This will be run
     * in a background thread.
     */
    protected void addContributedItems() {
        for (CommonAbstractMenuContribution contrib : contribs) {
            try {
                IContribItemProvider provider = MenuXMLMap
                        .getProvider(contrib.getClass());
                IContributionItem[] items = provider
                        .getContributionItems(contrib, subs, removals);
                for (IContributionItem item : items) {
                    add(item);
                }

            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error creating menu with id " + contrib.id, e);
            }
        }
    }

    /**
     * Use an {@link IMenuService} to add any dynamic contributions to the
     * submenu. This must be called on the main UI thread.
     */
    protected void addMenuServiceItems() {
        IWorkbenchWindow window = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow();
        String uri = MenuUtil.menuUri(getId());
        IMenuService menuService = window
                .getService(IMenuService.class);
        menuService.populateContributionManager(this, uri);
    }

    @Override
    public boolean isVisible() {
        /*
         * super will return false if there are no children, this will have no
         * children until the async task runs but should be visible anyway.
         */
        return visible;
    }

    private class ContributeItemsRunnable implements Runnable {

        @Override
        public void run() {
            synchronized (SubmenuContributionItem.this) {
                if (doneAddingContribs) {
                    return;
                }
                try {
                    addContributedItems();
                } finally {
                    doneAddingContribs = true;
                }

            }
        }
    }

}
