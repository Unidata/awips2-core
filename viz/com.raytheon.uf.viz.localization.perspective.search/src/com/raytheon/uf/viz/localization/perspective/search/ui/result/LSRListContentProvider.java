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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.search.ui.text.Match;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.viz.localization.perspective.search.LocalizationSearchMatch;
import com.raytheon.uf.viz.localization.perspective.search.LocalizationSearchResult;
import com.raytheon.uf.viz.localization.perspective.view.FileTreeEntryData;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileEntryData;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileGroupData;
import com.raytheon.uf.viz.localization.perspective.view.PathData;

/**
 * {@link IStructuredContentProvider} used in the
 * {@link LocalizationSearchResultPage} when the results are displayed as a
 * list.
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
public class LSRListContentProvider extends LSRBaseContentProvider {

    @Override
    public Object[] getElements(LocalizationSearchResult searchResult) {
        Map<LocalizationFileGroupData, Set<ILocalizationFile>> groupMap = groupMatchingFiles(
                searchResult);

        List<LocalizationFileEntryData> result = new ArrayList<>();
        for (Entry<LocalizationFileGroupData, Set<ILocalizationFile>> entry : groupMap
                .entrySet()) {
            PathData pathData = entry.getKey().getPathData();
            Set<ILocalizationFile> files = entry.getValue();
            result.addAll(createEntryData(pathData, files));
        }

        return result.toArray(new FileTreeEntryData[0]);
    }

    protected Map<LocalizationFileGroupData, Set<ILocalizationFile>> groupMatchingFiles(
            LocalizationSearchResult searchResult) {
        Map<LocalizationFileGroupData, Set<ILocalizationFile>> groupMap = new LinkedHashMap<>();
        for (Object element : searchResult.getElements()) {
            for (Match match : searchResult.getMatches(element)) {
                LocalizationSearchMatch lMatch = (LocalizationSearchMatch) match;
                PathData pathData = lMatch.getPathData();
                String path = lMatch.getElement().getPath();
                LocalizationFileGroupData groupData = new LocalizationFileGroupData(
                        pathData, path);
                Set<ILocalizationFile> files = groupMap.get(groupData);
                if (files == null) {
                    files = new HashSet<>();
                    groupMap.put(groupData, files);
                }
                files.add(lMatch.getElement());
            }
        }
        return groupMap;
    }

}
