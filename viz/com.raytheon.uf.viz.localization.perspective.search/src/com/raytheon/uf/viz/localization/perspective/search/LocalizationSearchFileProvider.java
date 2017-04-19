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
package com.raytheon.uf.viz.localization.perspective.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.viz.localization.perspective.view.PathData;
import com.raytheon.uf.viz.localization.perspective.view.PathDataExtManager;

/**
 * 
 * This class is responsible for locating the {@link ILocalizationFile}s that
 * should be searched as part of a {@link LocalizationSearchQuery}. Since the
 * process of locating files may involve communication with a server it can be
 * time consuming so an {@link IProgressMonitor} is used to provide feedback.
 * 
 * The primary function of this class is fulfilled by the
 * {@link #load(IProgressMonitor)} method which will populate this object with
 * the files to search, then the other methods can be used to access the files
 * that need to be searched.
 * 
 * The files to search are grouped by {@link PathData} which is necessary
 * because the UI needs information about the PathData to display a file
 * correctly.
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
public class LocalizationSearchFileProvider implements IRunnableWithProgress {

    private final IPathManager pathManager;

    private final String extension;

    private final String application;

    private final Map<PathData, Collection<ILocalizationFile>> result = new HashMap<>();

    private int fileCount = 0;

    /**
     * @param pathManager
     *            used to find files
     * @param extension
     *            only files that end with this extension will be included, null
     *            is allowed to disable extension filtering
     * @param application
     *            only files in this application will be included, null is
     *            allowed to search all applications. Applications are defined
     *            in the localization perspective by the {@link PathData}
     */
    public LocalizationSearchFileProvider(IPathManager pathManager,
            String extension, String application) {
        this.pathManager = pathManager;
        this.extension = extension;
        this.application = application;
    }

    /**
     * This should be the first method called after creating a new file
     * provider. It will use the {@link IPathManager} and the
     * {@link PathDataExtManager} to find all the {@link ILocalizationFile} that
     * match the criteria provided in the constructor. It is safe to call this
     * method again and it will requery for files.
     */
    @Override
    public void run(IProgressMonitor monitor) {
        result.clear();
        this.fileCount = 0;

        Collection<PathData> allPathData = PathDataExtManager.getPathData();
        Collection<PathData> matchingPathData = new ArrayList<>();

        /*
         * Filtering the PathData first should be very fast and makes it so the
         * progress can be tracked more accurately.
         */
        if (application == null) {
            matchingPathData = allPathData;
        } else {
            for (PathData pathData : allPathData) {
                if (pathData.getApplication().equals(application)) {
                    matchingPathData.add(pathData);
                }
            }
        }

        String taskName = "Finding matching files " + getLabel();
        monitor.beginTask(taskName, matchingPathData.size());

        for (PathData pathData : matchingPathData) {
            monitor.subTask("Looking in " + pathData.getApplication() + " "
                    + pathData.getName());
            String[] filter = mergeExtensions(extension, pathData.getFilter());
            if (filter != null && filter.length == 0) {
                continue;
            }

            String path = pathData.getPath();
            boolean recursive = pathData.isRecursive();
            List<ILocalizationFile> files = new ArrayList<>();
            List<LocalizationType> types = pathData.getTypes();
            for (LocalizationType type : types) {
                if (monitor.isCanceled()) {
                    return;
                }
                LocalizationContext[] contexts = pathManager
                        .getLocalSearchHierarchy(type);
                ILocalizationFile[] filesArr = pathManager.listFiles(contexts,
                        path, filter, recursive, true);
                files.addAll(Arrays.asList(filesArr));
            }
            if (!files.isEmpty()) {
                result.put(pathData, files);
                fileCount += files.size();
            }
            monitor.worked(1);
        }
    }

    /**
     * @return all the {@link PathData} that contains files that should be
     *         searched.
     */
    public Collection<PathData> getPaths() {
        return result.keySet();
    }

    /**
     * return all the files to search for a specific PathData.
     */
    public Collection<ILocalizationFile> getFiles(PathData pathData) {
        return result.get(pathData);
    }

    /**
     * Return a user friendly description of how the files were filtered, this
     * is intended to be used as a suffix in search related labels, for example
     * "found 7 files" + getLabel().
     */
    public String getLabel() {
        StringBuilder label = new StringBuilder();
        if (application != null) {
            label.append("in ").append(application);
        }
        if (extension != null) {
            label.append("(").append(extension).append(")");
        }
        return label.toString();
    }

    /**
     * @return the total number of files that need to be searched.
     */
    public int getFileCount() {
        return fileCount;
    }

    /**
     * Combine the requested search extension with the valid extensions for a
     * specific PathData. This allows the {@link IPathManager} to perform all
     * the extension filtering. It also makes it possible to skip searching a
     * PathData if there are incompatible extensions.
     */
    private static String[] mergeExtensions(String queryExtension,
            String[] pathExtensions) {
        if (queryExtension == null) {
            return pathExtensions;
        }
        if (pathExtensions == null) {
            return new String[] { queryExtension };
        }
        Set<String> newExtensions = new HashSet<>();
        for (String pathExtension : pathExtensions) {
            if (pathExtension.endsWith(queryExtension)) {
                newExtensions.add(pathExtension);
            } else if (queryExtension.endsWith(pathExtension)) {
                newExtensions.add(queryExtension);
            }
        }
        return newExtensions.toArray(new String[0]);
    }

    @Override
    public String toString() {
        return "LocalizationSearchFileProvider [application=" + application
                + ", extension=" + extension + ", fileCount=" + fileCount + "]";
    }

}
