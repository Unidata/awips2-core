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

package com.raytheon.viz.ui.cmenu;

import com.raytheon.uf.viz.core.rsc.DisplayType;

/**
 * Action to load a display as contours
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 *                                    Initial creation
 * Mar 13, 2018  6815     njensen     Renamed Graphics to Contours
 * 
 * 
 * </pre>
 * 
 */
public class LoadAsContoursAction extends LoadAsDisplayTypeAction {

    @Override
    public String getText() {
        return "Load as Contours";
    }

    @Override
    protected DisplayType getDisplayType() {
        return DisplayType.CONTOUR;
    }

}
