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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.viz.localization.perspective.search.LocalizationSearchResult;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileEntryData;
import com.raytheon.uf.viz.localization.perspective.view.PathData;

/**
 * Base {@link IStructuredContentProvider} for
 * {@link LocalizationSearchResultPage}. This contains any functionality which
 * is shared between the content providers of the tree and list views.
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
public abstract class LSRBaseContentProvider
        implements IStructuredContentProvider {

    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof LocalizationSearchResult) {
            return getElements((LocalizationSearchResult) inputElement);
        }
        throw new IllegalStateException("Unexpected root element of type: "
                + inputElement.getClass().getSimpleName());
    }

    public abstract Object[] getElements(LocalizationSearchResult searchResult);

    /**
     * Convert some {@link ILocalizationFile}s to
     * {@link LocalizationFileEntryData}s. The returned list is sorted correctly
     * for the display.
     */
    protected List<LocalizationFileEntryData> createEntryData(PathData pathData,
            Collection<ILocalizationFile> files) {
        Set<LocalizationLevel> levels = new HashSet<>();
        Set<LocalizationLevel> redundantLevels = new HashSet<>();
        for (ILocalizationFile file : files) {
            LocalizationLevel level = file.getContext().getLocalizationLevel();
            if (!levels.add(level)) {
                redundantLevels.add(level);
            }
        }

        List<ILocalizationFile> sortedFiles = new ArrayList<>(files);
        sortedFiles.sort((o1, o2) -> o1.getContext().getLocalizationLevel()
                .compareTo(o2.getContext().getLocalizationLevel()));

        List<LocalizationFileEntryData> result = new ArrayList<>(files.size());
        for (ILocalizationFile file : sortedFiles) {
            LocalizationLevel level = file.getContext().getLocalizationLevel();
            result.add(new LocalizationFileEntryData(pathData,
                    (LocalizationFile) file, redundantLevels.contains(level)));
        }
        return result;
    }

}
