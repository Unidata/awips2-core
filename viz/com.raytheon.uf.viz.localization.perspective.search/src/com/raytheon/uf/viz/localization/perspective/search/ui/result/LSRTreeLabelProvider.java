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

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.viz.localization.perspective.search.LocalizationSearchMatch;
import com.raytheon.uf.viz.localization.perspective.view.FileTreeEntryData;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileEntryData;

/**
 * {@link IStyledLabelProvider} used in the {@link LocalizationSearchResultPage}
 * when the results are displayed as a tree.
 * 
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
public class LSRTreeLabelProvider extends LSRBaseLabelProvider {

    /* The number of characters to display before and after a search result. */
    private static final int RESULT_CONTEXT_SIZE = 64;

    private static final String LINE_CUT_INDICATOR = " ... ";

    public LSRTreeLabelProvider(LocalizationSearchResultPage page) {
        super(page);
    }

    @Override
    public StyledString getStyledText(Object element) {
        if (element instanceof ApplicationTreeNode) {
            String name = ((ApplicationTreeNode) element).getName();
            return new StyledString(name);
        } else if (element instanceof LocalizationFileEntryData) {
            LocalizationFileEntryData entryData = (LocalizationFileEntryData) element;
            ILocalizationFile file = entryData.getFile();

            String name = getContextLabel(entryData);

            StyledString styledName = new StyledString(name);

            int count = page.getInput().getMatchCount(file);
            if (count > 1) {
                String countInfo = "(" + count + " matches)";
                styledName.append(' ').append(countInfo,
                        StyledString.COUNTER_STYLER);
            }
            return styledName;
        } else if (element instanceof FileTreeEntryData) {
            String name = ((FileTreeEntryData) element).getName();
            return new StyledString(name);
        } else if (element instanceof LocalizationSearchMatch) {
            LocalizationSearchMatch match = (LocalizationSearchMatch) element;
            StyledString str = new StyledString(match.getLineNumber() + ": ",
                    StyledString.QUALIFIER_STYLER);
            String line = match.getLine();
            int startIndex = match.getLineOffset();
            for (int i = 0; i < startIndex + 1; i += 1) {
                if (!Character.isWhitespace(line.charAt(i))) {
                    startIndex -= i;
                    line = line.substring(i);
                    break;
                }
            }

            int endIndex = startIndex + match.getLength();
            if (startIndex > RESULT_CONTEXT_SIZE) {
                str.append(LINE_CUT_INDICATOR);
                str.append(line.substring(startIndex - RESULT_CONTEXT_SIZE,
                        startIndex));
            } else {
                str.append(line.substring(0, startIndex));
            }
            str.append(line.substring(startIndex, endIndex), HIGHLIGHT_STYLE);
            if (line.length() - endIndex > RESULT_CONTEXT_SIZE) {
                str.append(line.substring(endIndex,
                        endIndex + RESULT_CONTEXT_SIZE));
                str.append(LINE_CUT_INDICATOR);
            } else {
                str.append(line.substring(endIndex));
            }
            return str;
        }
        return new StyledString(element.toString());
    }

}
