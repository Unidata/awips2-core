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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.viz.localization.perspective.search.ui.LocalizationResourceSelectionDialog;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileEntryData;

/**
 * Provide labels for {@link LocalizationFileEntryData} objects in the main list
 * of the {@link LocalizationResourceSelectionDialog}
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Aug 17, 2017  6359     bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class ResourceSelectionListLabelProvider extends LSRBaseLabelProvider {

    @Override
    public StyledString getStyledText(Object element) {
        if (element instanceof LocalizationFileEntryData) {
            LocalizationFileEntryData data = (LocalizationFileEntryData) element;
            ILocalizationFile file = data.getFile();
            IPath filePath = Path.forPosix(file.getPath());

            String name = filePath.lastSegment();
            StyledString styledName = new StyledString(name);

            String decorated = name + " " + getContextLabel(data);

            StyledCellLabelProvider.styleDecoratedString(decorated,
                    StyledString.QUALIFIER_STYLER, styledName);
            return styledName;
        } else if (element == null) {
            return new StyledString();
        }
        return new StyledString(element.toString());
    }

}
