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
package com.raytheon.uf.viz.localization.perspective.search.ui.result;

import java.util.Comparator;

import com.raytheon.uf.viz.localization.perspective.view.FileTreeEntryData;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileGroupData;

/**
 * Sort {@link FileTreeEntryData} so that
 * directories appear before files({@link LocalizationFileGroupData}) and
 * everything else is sorted alphabetically, case insensitive.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 06, 2017  6188     bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class FileTreeEntryDataComparator
        implements Comparator<FileTreeEntryData> {

    @Override
    public int compare(FileTreeEntryData o1, FileTreeEntryData o2) {
        if (o1 instanceof LocalizationFileGroupData) {
            if (!(o2 instanceof LocalizationFileGroupData)) {
                return 1;
            }
        } else if (o2 instanceof LocalizationFileGroupData) {
            return -1;
        }
        return o1.getName().compareToIgnoreCase(o2.getName());

    }

}
