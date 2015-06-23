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

import com.raytheon.viz.ui.widgets.AbstractVizTreeFilter;

/**
 * Determines which {@link VizLocalizationFileTree} elements should be filtered.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 15, 2015 4401       bkowal      Initial creation
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

public class VizLocalizationTreeFilter extends AbstractVizTreeFilter {

    @Override
    protected boolean shouldFilter(Object element) {
        if (element instanceof VizLocalizationFileTree
                && ((VizLocalizationFileTree) element).isFileNode()) {
            return true;
        }

        return false;
    }
}