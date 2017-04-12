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
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileEntryData;
import com.raytheon.uf.viz.localization.perspective.view.PathData;

/**
 * {@link IStyledLabelProvider} used in the {@link LocalizationSearchResultPage}
 * when the results are displayed as a list.
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
public class LSRListLabelProvider extends LSRBaseLabelProvider {

    public LSRListLabelProvider(LocalizationSearchResultPage page) {
        super(page);
    }

    @Override
    public StyledString getStyledText(Object element) {
        if (element instanceof LocalizationFileEntryData) {
            LocalizationFileEntryData data = (LocalizationFileEntryData) element;
            ILocalizationFile file = data.getFile();
            PathData pathData = data.getPathData();
            IPath filePath = Path.forPosix(file.getPath());
            IPath dataPath = Path.forPosix(pathData.getPath());
            if (dataPath.isAbsolute() && dataPath.segmentCount() != 0) {
                dataPath = dataPath.makeRelative();
            }

            String name = filePath.lastSegment() + " - "
                    + getContextLabel(data);
            StyledString styledName = new StyledString(name);

            IPath path = Path.forPosix(pathData.getApplication());
            path = path.append(pathData.getName());
            if (dataPath.segmentCount() == 0) {
                path = path.append(filePath);
            } else {
                path = path.append(filePath.makeRelativeTo(dataPath));
            }
            path = path.removeLastSegments(0);

            String decorated = name + " " + path.toString();

            StyledCellLabelProvider.styleDecoratedString(decorated,
                    StyledString.QUALIFIER_STYLER, styledName);

            int count = page.getInput().getMatchCount(file);
            if (count > 1) {
                String countInfo = "(" + count + " matches)";
                styledName.append(' ').append(countInfo,
                        StyledString.COUNTER_STYLER);
            }
            return styledName;
        }
        return new StyledString(element.toString());
    }

}
