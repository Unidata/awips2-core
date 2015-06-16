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
package com.raytheon.viz.ui.actions;

import com.raytheon.viz.ui.dialogs.localization.VizLocalizationFileTree;

/**
 * A node in the {@link VizLocalizationFileTree} representative of a local file
 * on the file system.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 16, 2015 4401       bkowal      Initial creation
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

public class LocalPerspectiveFileTree extends VizLocalizationFileTree {

    /**
     * Constructor.
     * 
     * @param text
     *            the name of the local file system file.
     */
    public LocalPerspectiveFileTree(String text) {
        super(text, null);
    }

    @Override
    public boolean isFileNode() {
        return true;
    }
}