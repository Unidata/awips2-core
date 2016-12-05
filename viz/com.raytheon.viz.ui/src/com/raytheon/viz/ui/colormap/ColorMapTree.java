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
package com.raytheon.viz.ui.colormap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.raytheon.uf.common.colormap.ColorMapLoader;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.viz.core.VizApp;

/**
 * ColorMapTree represents the directory structure of colormaps directory. The
 * levels of a Tree can represent a {@link LocalizationLevel}, a
 * {@link LocalizationContext} or a localization directory.
 * 
 * Many of the methods require information about {@link ILocalizationFile}s and
 * might need to get information from a remote server. After the information is
 * loaded it is kept within the tree and future uses of the tree will be fast.
 * When ever accessing the tree on the UI thread it may be necessary to use
 * {@link #isReady()} and {@link #prepareAsync(Optional)} to ensure that UI is
 * not paused waiting on a remote server.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 18, 2013  2421     bsteffen  Initial creation
 * Sep 11, 2014  3516     rferrel   file updates now inform the factory.
 *                                  getName() no longer returns a null.
 *                                  FileChangeListener now only gets colormaps
 *                                  changes.
 * Aug 24, 2015  4393     njensen   Updates for observer changes
 * Nov 18, 2015  4834     njensen   API updates due to removal of
 *                                  LocalizationNotificationObserver
 * Jan 13, 2016  5242     kbisanz   Replaced calls to deprecated
 *                                  LocalizationFile methods
 * Dec 01, 2016  5990     bsteffen  Ensure menu population does not pause UI
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class ColorMapTree {

    private final String path;

    private final IPathManager pathManager;

    private final LocalizationLevel level;

    private final LocalizationContext context;

    private final Object filesLock = new Object();

    private ILocalizationFile[] files;

    private final Object subTreesLock = new Object();

    private List<ColorMapTree> subTrees;

    private boolean ready = false;

    private List<Runnable> prepareCallbacks = new ArrayList<>();

    /**
     * Create a tree for the given path and context. The tree will represent the
     * colormap files that exist at the path within the context.
     */
    public ColorMapTree(IPathManager pathManager, LocalizationContext context,
            String path) {
        this.path = path;
        this.pathManager = pathManager;
        this.level = null;
        this.context = context;
    }

    /**
     * Create a tree for the given path and level. The tree will have the same
     * name as the level and will have a subtree for each context that exists at
     * that level. Each context tree will represent the colormap files that
     * exist at the path.
     */
    public ColorMapTree(IPathManager pathManager, LocalizationLevel level,
            String path) {
        this.path = path;
        this.pathManager = pathManager;
        this.level = level;
        this.context = null;
    }

    /**
     * For a tree based only on a {@link LocalizationLevel} this returns the
     * name of the level, for a tree at the root directory of a context this
     * will be the name of the {@link LocalizationContext} and for a tree
     * representing a subdirectory this will be the directory name.
     */
    public String getName() {
        if (context == null) {
            return level.name();
        } else {
            int start = path.lastIndexOf(IPathManager.SEPARATOR);
            if (start <= 0) {
                String name = context.getContextName();
                if (name == null) {
                    name = context.getLocalizationLevel().name();
                }
                return name;
            }
            return path.substring(start + 1);
        }
    }

    public boolean isLevel() {
        return level != null;
    }

    /**
     * For a tree based on a {@link LocalizationLevel} this returns a tree for
     * each context at the level. Otherwise it returns a tree for each
     * subdirectory of this tree.
     */
    public List<ColorMapTree> getSubTrees() {
        synchronized (subTreesLock) {
            if (subTrees == null) {
                subTrees = new ArrayList<>();
                if (context == null) {
                    for (String context : pathManager.getContextList(level)) {
                        LocalizationContext ctx = pathManager.getContext(
                                LocalizationType.COMMON_STATIC, level);
                        ctx.setContextName(context);
                        subTrees.add(new ColorMapTree(pathManager, ctx, path));
                    }
                } else {
                    for (ILocalizationFile file : requestFiles()) {
                        if (file.isDirectory()
                                && !path.equals(file.getPath())) {
                            subTrees.add(new ColorMapTree(pathManager, context,
                                    file.getPath()));
                        }
                    }
                }
            }
            return new ArrayList<>(subTrees);
        }
    }

    /**
     * 
     * @return all color map files within this level of the tree.
     */
    public List<ILocalizationFile> getColorMapFiles() {
        if (context == null) {
            return Collections.emptyList();
        } else {
            List<ILocalizationFile> result = new ArrayList<>();
            for (ILocalizationFile file : requestFiles()) {
                if (!file.isDirectory()) {
                    result.add(file);
                }
            }
            return result;
        }
    }

    /**
     * 
     * @return true if this tree does not contain any color map files or any
     *         subtrees which contain color map files(recursively).
     */
    public boolean isEmpty() {
        if (getColorMapFiles().isEmpty()) {
            ColorMapTree optimistic = getOptimisticSubtree();
            if (optimistic != null && !optimistic.isEmpty()) {
                return false;
            }
            for (ColorMapTree tree : getSubTrees()) {
                if (!tree.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Optimize the internal structure so future calls to {@link #getSubTrees()}
     * , {@link #getColorMapFiles()}, and {@link #isEmpty()} are fast. isEmpty()
     * in particular is a slow operation on trees with many empty subtrees, so
     * this can be called in the background to enable faster calls to isEmpty
     * when it is needed. In cases where isEmpty does not need extra data or is
     * already optimized this call should complete very quickly. Intended for
     * use on non-UI thread.
     */
    public void prepare() {
        if (context == null) {
            ColorMapTree optimistic = getOptimisticSubtree();
            if (optimistic == null || optimistic.isEmpty()) {
                requestFilesForLevel();
            }
        }

        /*
         * Simplest way to ensure isEmpty() is fast is just to call isEmpty, it
         *  will load whatever it needs.
         */
        isEmpty();

        synchronized (prepareCallbacks) {
            ready = true;
            prepareCallbacks.forEach(VizApp::runAsync);
            prepareCallbacks.clear();
        }
    }

    /**
     * If the tree {@link #isReady()} and the callback is present then it will
     * be called immediately. Otherwise the tree will be scheduled to
     * {@link #prepare()} itself and the callback will be run on the UI thread
     * when the tree becomes ready.
     * 
     * @param callback
     */
    public void prepareAsync(Optional<Runnable> callback) {
        synchronized (prepareCallbacks) {
            if (ready) {
                callback.ifPresent(VizApp::runSync);
            } else {
                callback.ifPresent(prepareCallbacks::add);
                PrepareColorMapTreeJob.add(this, callback.isPresent());
            }
        }
    }

    /**
     * @return true if all data is loaded for {@link #getSubTrees()},
     *         {@link #getColorMapFiles()}, and {@link #isEmpty()} so that no
     *         Localization requests are necessary.
     * 
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * For a level based tree this will return the subtree for the currently
     * active context for the level. This level is the most likely to be not
     * empty so checking this subtree first can speed up {@link #isEmpty()}
     */
    private ColorMapTree getOptimisticSubtree() {
        if (context == null) {
            LocalizationContext context = pathManager
                    .getContext(LocalizationType.COMMON_STATIC, level);
            for (ColorMapTree tree : getSubTrees()) {
                if (tree.getName().equals(context.getContextName())) {
                    return tree;
                }
            }
        }
        return null;
    }

    private ILocalizationFile[] requestFiles() {
        synchronized (filesLock) {
            if (files == null) {
                files = pathManager.listFiles(context, path,
                        new String[] { ColorMapLoader.EXTENSION }, false,
                        false);
            }
            return files;
        }
    }

    /**
     * Many levels have alot of contexts that they must check and most of those
     * contexts tend to be empty. To avoid making too many separate localization
     * requests for each context this will query all the contexts at once.
     */
    private void requestFilesForLevel() {
        synchronized (filesLock) {
            Map<LocalizationContext, ColorMapTree> sortedTrees = new HashMap<>();
            for (ColorMapTree tree : getSubTrees()) {
                if (!tree.hasFiles()) {
                    LocalizationContext context = pathManager
                            .getContext(LocalizationType.COMMON_STATIC, level);
                    context.setContextName(tree.getName());
                    sortedTrees.put(context, tree);
                }
            }
            if (sortedTrees.isEmpty()) {
                return;
            }
            LocalizationContext[] contexts = sortedTrees.keySet()
                    .toArray(new LocalizationContext[0]);
            ILocalizationFile[] files = pathManager.listFiles(contexts, path,
                    new String[] { ColorMapLoader.EXTENSION }, false, false);
            Map<LocalizationContext, List<ILocalizationFile>> sortedFiles = new HashMap<>();
            for (ILocalizationFile file : files) {
                LocalizationContext context = file.getContext();
                List<ILocalizationFile> fileList = sortedFiles.get(context);
                if (fileList == null) {
                    fileList = new ArrayList<>();
                    sortedFiles.put(context, fileList);
                }
                fileList.add(file);
            }
            for (LocalizationContext context : sortedTrees.keySet()) {
                ColorMapTree tree = sortedTrees.get(context);
                List<ILocalizationFile> fileList = sortedFiles.get(context);
                if (fileList == null) {
                    fileList = Collections.emptyList();
                }
                tree.setFiles(fileList.toArray(new ILocalizationFile[0]));
            }
        }
    }

    /**
     * 
     * @return true if the tree already has localization files and does not need
     *         them to be requested again.
     */
    protected boolean hasFiles() {
        return files != null;
    }

    /**
     * This will be called by the parent tree if the context is requested along
     * with other contexts in {@link #requestFilesForLevel()}
     */
    protected void setFiles(ILocalizationFile[] files) {
        synchronized (filesLock) {
            if (this.files == null) {
                this.files = files;
            }
        }
    }

}
