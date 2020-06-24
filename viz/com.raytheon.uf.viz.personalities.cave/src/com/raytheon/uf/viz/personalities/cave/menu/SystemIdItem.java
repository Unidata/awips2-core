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
package com.raytheon.uf.viz.personalities.cave.menu;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.actions.CompoundContributionItem;

import com.raytheon.uf.common.util.SystemUtil;

/**
 * Command that displays the system id (hostname and pid)
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 10, 2011           mschenke  Initial creation
 * Oct 01, 2014  3678     njensen   Removed usage of JMX, clarified pid
 * Jun 24, 2020  8187     randerso  Changed pid to long to match
 *                                  ProcessHandle.getPid() in
 *                                  SystemUtil.getPid()
 *
 * </pre>
 *
 * @author mschenke
 */

public class SystemIdItem extends CompoundContributionItem {

    private static final String HOST = SystemUtil.getHostName();

    private static final String PID = Long.toString(SystemUtil.getPid());

    @Override
    protected IContributionItem[] getContributionItems() {
        return new IContributionItem[] {
                new ActionContributionItem(new Action(HOST + "   pid:" + PID) {
                    @Override
                    public boolean isEnabled() {
                        return false;
                    }
                }) };
    }

}
