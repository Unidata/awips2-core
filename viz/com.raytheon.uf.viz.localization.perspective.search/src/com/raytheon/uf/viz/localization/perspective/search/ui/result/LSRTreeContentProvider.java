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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.search.ui.text.Match;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.viz.localization.perspective.search.LocalizationSearchMatch;
import com.raytheon.uf.viz.localization.perspective.search.LocalizationSearchResult;
import com.raytheon.uf.viz.localization.perspective.view.FileTreeEntryData;
import com.raytheon.uf.viz.localization.perspective.view.FileTreeView;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileEntryData;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileGroupData;
import com.raytheon.uf.viz.localization.perspective.view.PathData;

/**
 * {@link IStructuredContentProvider} used in the
 * {@link LocalizationSearchResultPage} when the results are displayed as a
 * tree. Tries to reuse as many objects as Possible from the
 * {@link FileTreeView}, specifically {@link FileTreeEntryData} and it's
 * subclasses.
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
public class LSRTreeContentProvider extends LSRBaseContentProvider
        implements ITreeContentProvider {

    private LocalizationSearchResult searchResult;

    @Override
    public Object[] getElements(LocalizationSearchResult searchResult) {
        this.searchResult = searchResult;
        Set<String> appLabels = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (LocalizationSearchMatch match : getAllMatches()) {
            appLabels.add(match.getPathData().getApplication());
        }

        List<ApplicationTreeNode> result = new ArrayList<>();
        for (String appLabel : appLabels) {
            result.add(new ApplicationTreeNode(appLabel));
        }
        return result.toArray();
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof ApplicationTreeNode) {
            return getChildrenInternal((ApplicationTreeNode) parentElement);
        } else if (parentElement instanceof LocalizationFileGroupData) {
            return getChildrenInternal(
                    (LocalizationFileGroupData) parentElement);
        } else if (parentElement instanceof LocalizationFileEntryData) {
            return getChildrenInternal(
                    (LocalizationFileEntryData) parentElement);
        } else if (parentElement instanceof FileTreeEntryData) {
            return getChildrenInternal((FileTreeEntryData) parentElement);
        } else if (parentElement instanceof Match) {
            return new Object[0];
        } else if (parentElement == null) {
            throw new IllegalStateException("Unexpected null element.");
        } else {
            throw new IllegalStateException("Unexpected element of type: "
                    + parentElement.getClass().getSimpleName());
        }
    }

    protected Object[] getChildrenInternal(ApplicationTreeNode parentElement) {
        String appLabel = parentElement.getName();

        Collection<LocalizationSearchMatch> matches = getMatches(
                m -> m.getPathData().getApplication().equals(appLabel));
        Set<PathData> pathDataSet = new HashSet<>();
        for (LocalizationSearchMatch match : matches) {
            pathDataSet.add(match.getPathData());
        }
        List<FileTreeEntryData> result = new ArrayList<>();
        for (PathData pathData : pathDataSet) {
            FileTreeEntryData data = new FileTreeEntryData(pathData,
                    pathData.getPath(), true);
            data.setName(pathData.getName());
            result.add(data);
        }
        FileTreeEntryData[] resultArr = result
                .toArray(new FileTreeEntryData[0]);
        Arrays.sort(resultArr, new FileTreeEntryDataComparator());
        return resultArr;
    }

    protected Object[] getChildrenInternal(FileTreeEntryData parentElement) {
        PathData pathData = parentElement.getPathData();
        Collection<LocalizationSearchMatch> matches = getMatches(
                m -> m.getPathData() == pathData);

        IPath parentPath = Path.forPosix(parentElement.getPath());
        if (parentPath.isAbsolute() && parentPath.segmentCount() != 0) {
            parentPath = parentPath.makeRelative();
        }
        Set<FileTreeEntryData> result = new HashSet<>();
        for (LocalizationSearchMatch match : matches) {
            ILocalizationFile file = match.getElement();
            IPath path = Path.forPosix(file.getPath());
            if (parentPath.isPrefixOf(path) || parentPath.segmentCount() == 0) {
                if (path.segmentCount() == parentPath.segmentCount() + 1) {
                    result.add(new LocalizationFileGroupData(pathData,
                            file.getPath()));
                } else {
                    path = parentPath
                            .append(path.segment(parentPath.segmentCount()));
                    result.add(
                            new FileTreeEntryData(pathData, path.toString()));
                }

            }
        }
        FileTreeEntryData[] resultArr = result
                .toArray(new FileTreeEntryData[0]);
        Arrays.sort(resultArr, new FileTreeEntryDataComparator());
        return resultArr;
    }

    protected Object[] getChildrenInternal(
            LocalizationFileGroupData parentElement) {
        Collection<LocalizationSearchMatch> matches = getMatches(
                m -> m.getElement().getPath().equals(parentElement.getPath()));

        Set<ILocalizationFile> files = new HashSet<>();
        for (LocalizationSearchMatch match : matches) {
            files.add(match.getElement());
        }
        PathData pathData = parentElement.getPathData();
        List<LocalizationFileEntryData> result = createEntryData(pathData,
                files);

        return result.toArray();
    }

    protected Object[] getChildrenInternal(
            LocalizationFileEntryData parentElement) {
        return this.searchResult.getMatches(parentElement.getFile());
    }

    protected Collection<LocalizationSearchMatch> getAllMatches() {
        return getMatches(m -> true);
    }

    protected Collection<LocalizationSearchMatch> getMatches(
            Predicate<LocalizationSearchMatch> testFunction) {
        List<LocalizationSearchMatch> result = new ArrayList<>();
        for (Object element : this.searchResult.getElements()) {
            for (Match match : this.searchResult.getMatches(element)) {
                LocalizationSearchMatch lMatch = (LocalizationSearchMatch) match;
                if (testFunction.test(lMatch)) {
                    result.add(lMatch);
                }
            }
        }
        return result;
    }

    @Override
    public Object getParent(Object element) {
        if (element instanceof LocalizationSearchResult) {
            return null;
        } else if (element instanceof ApplicationTreeNode) {
            return searchResult;
        } else if (element instanceof LocalizationFileEntryData) {
            LocalizationFileEntryData elementData = (LocalizationFileEntryData) element;
            return new LocalizationFileGroupData(elementData.getPathData(),
                    elementData.getPath());
        } else if (element instanceof FileTreeEntryData) {
            FileTreeEntryData elementData = (FileTreeEntryData) element;
            if (elementData.isRoot()) {
                return new ApplicationTreeNode(
                        elementData.getPathData().getApplication());
            } else {
                IPath parentPath = Path.forPosix(elementData.getPath())
                        .removeLastSegments(1);
                if (parentPath.equals(
                        Path.forPosix(elementData.getPathData().getPath()))) {
                    return new FileTreeEntryData(elementData.getPathData(),
                            parentPath.toString(), true);
                } else {
                    return new FileTreeEntryData(elementData.getPathData(),
                            parentPath.toString());
                }
            }
        } else if (element instanceof LocalizationSearchMatch) {
            LocalizationSearchMatch match = (LocalizationSearchMatch) element;
            return new LocalizationFileEntryData(match.getPathData(),
                    (LocalizationFile) match.getElement(), false);
        } else if (element == null) {
            throw new IllegalStateException("Unexpected null element.");
        } else {
            throw new IllegalStateException("Unexpected element of type: "
                    + element.getClass().getSimpleName());
        }
    }

    @Override
    public boolean hasChildren(Object element) {
        return !(element instanceof Match);
    }

}
