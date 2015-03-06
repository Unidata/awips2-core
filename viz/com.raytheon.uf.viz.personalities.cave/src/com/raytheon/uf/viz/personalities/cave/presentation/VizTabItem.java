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
package com.raytheon.uf.viz.personalities.cave.presentation;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.presentations.PresentablePart;
import org.eclipse.ui.internal.presentations.defaultpresentation.DefaultTabItem;
import org.eclipse.ui.internal.presentations.util.PartInfo;

import com.raytheon.viz.ui.IRenameablePart;

/**
 * A tab item that specifically will not show the * in the tab title for a tab
 * that contains a dirty Viz editor. Non-Viz editors (e.g. Eclipse editors) will
 * still show the * on the tab title if they are dirty.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 3, 2015  4204       njensen     Initial creation
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class VizTabItem extends DefaultTabItem {

    public VizTabItem(CTabFolder parent, int index, int flags) {
        super(parent, index, flags);
    }

    @Override
    public void setInfo(PartInfo info) {
        boolean removeStar = false;
        if (info.dirty && getData() instanceof PresentablePart) {
            PresentablePart part = (PresentablePart) getData();
            IWorkbenchPart wbPart = part.getPane().getPartReference()
                    .getPart(false);
            if (wbPart instanceof IRenameablePart) {
                /*
                 * We don't want to show the DIRTY_PREFIX (*) on our editors
                 * that we rename. Normal Eclipse behavior should apply to other
                 * editors.
                 */
                removeStar = true;
            }
        }
        if (removeStar) {
            info.dirty = false;
        }
        super.setInfo(info);
        if (removeStar) {
            info.dirty = true;
        }
    }

}
