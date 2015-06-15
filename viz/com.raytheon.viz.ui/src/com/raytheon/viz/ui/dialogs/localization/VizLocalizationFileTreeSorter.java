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
package com.raytheon.viz.ui.dialogs.localization;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * 
 * Displays a list of localization files in a tree view.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ???                                 Initial creation
 * 02 Jun 2015  4401       bkowal      Re-factored for reuse.
 * 
 * </pre>
 * 
 * @author unknown
 * @version 1.0
 */

public class VizLocalizationFileTreeSorter extends ViewerSorter {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        if (e1 instanceof VizLocalizationFileTree && e2 instanceof VizLocalizationFileTree) {
            VizLocalizationFileTree lhs = (VizLocalizationFileTree) e1;
            VizLocalizationFileTree rhs = (VizLocalizationFileTree) e2;
            int comp = lhs.getText().compareToIgnoreCase(rhs.getText());
            if (comp < 0) {
                return -1;
            } else if (comp > 0) {
                return 1;
            } else {
                return 0;
            }
        }
        return -1;
    }

}
