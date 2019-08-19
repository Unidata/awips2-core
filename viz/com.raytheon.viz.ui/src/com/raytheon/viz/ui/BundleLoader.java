package com.raytheon.viz.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorPart;

import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.AbstractTimeMatcher;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.globals.VizGlobalsManager;
import com.raytheon.uf.viz.core.procedures.Bundle;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.viz.ui.editor.IMultiPaneEditor;

/**
 * 
 * Loads a bundle to a container. Replaces contents of bundle on the container
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 8, 2013             mschenke    Initial creation
 * Feb 25, 2013 1640       bsteffen    Dispose old display in BundleLoader
 * Mar 22, 2013 1638       mschenke    Made not throw errors when no time matcher
 * Mar 02, 2015 4204       njensen     Loading bundles potentially schedules a part rename
 * Jun 05, 2015 4495       njensen     Prevent NPE on file not found
 * Oct 12, 2015 4932       njensen     ensureOneToOne() removes all panes when nPanes < nDisplays
 *                                      getLoadItems() attempts to reuse 4-panel maps across all panes
 * Jan 31, 2018 6737       njensen     Move most of loadBundleToContainer() onto the UI thread
 * 
 * </pre>
 * 
 * @author mschenke
 */
public class BundleLoader extends Job {

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(BundleLoader.class);

    public static enum BundleInfoType {
        FILE_LOCATION, XML
    }

    protected static class LoadItem {

        public final IDisplayPane loadTo;

        public final IRenderableDisplay loadFrom;

        public LoadItem(IDisplayPane loadTo, IRenderableDisplay loadFrom) {
            this.loadTo = loadTo;
            this.loadFrom = loadFrom;
        }

    }

    private class InstantiationTask implements Runnable {

        private final IDescriptor descriptor;

        private InstantiationTask(LoadItem loadItem) {
            IDisplayPane loadTo = loadItem.loadTo;
            IRenderableDisplay loadFrom = loadItem.loadFrom;
            if (loadTo != loadFrom.getDescriptor()) {
                load(loadTo, loadFrom);
            }
            descriptor = loadTo.getDescriptor();
        }

        @Override
        public void run() {
            descriptor.getResourceList().instantiateResources(descriptor, true);
        }

    }

    protected final IDisplayPaneContainer container;

    private final Bundle bundle;

    public BundleLoader(IDisplayPaneContainer container, Bundle bundle) {
        this("Bundle Loader", container, bundle);
    }

    protected BundleLoader(String name, IDisplayPaneContainer container,
            Bundle bundle) {
        super(name);
        this.container = container;
        this.bundle = bundle;
        final String bundleName = bundle.getName();
        if (bundleName != null && !bundleName.isEmpty()
                && container instanceof IRenameablePart) {
            VizApp.runAsync(new Runnable() {
                @Override
                public void run() {
                    ((IRenameablePart) BundleLoader.this.container)
                            .setPartName(bundleName);
                }

            });
        }
    }

    /**
     * Runs the loading synchronously.
     */
    public final void run() {
        run(new NullProgressMonitor());
    }

    @Override
    protected final IStatus run(IProgressMonitor monitor) {
        long t0 = System.currentTimeMillis();
        try {
            loadBundleToContainer(container, bundle);
            if (bundle.getLoopProperties() != null) {
                container.setLoopProperties(bundle.getLoopProperties());
            }

            /** refresh the editor */
            container.refresh();

            if (container instanceof IEditorPart) {
                /** update the history list */
                HistoryList.getInstance().refreshLatestBundle(
                        HistoryList.prepareHistoryEntry(container));
            }

            if (container instanceof IEditorPart) {
                VizApp.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        VizGlobalsManager.getCurrentInstance()
                                .updateUI(container);
                    }
                });
            }
        } catch (VizException e) {
            return new Status(IStatus.ERROR, UiPlugin.PLUGIN_ID,
                    "Error loading bundle", e);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("Total bundle retrieval: " + (t2 - t0));
        return Status.OK_STATUS;
    }

    /**
     * Loads a {@link Bundle} onto an {@link IDisplayPaneContainer}
     * 
     * @param container
     * @param bundle
     * @throws VizException
     */
    private final void loadBundleToContainer(IDisplayPaneContainer container,
            Bundle bundle) throws VizException {
        /*
         * Most of this code must be on the UI thread to prevent the UI thread
         * from changing things out underneath this. For example, swapping to a
         * side pane will be on the UI thread and if some of these steps occur
         * asynchronously on a separate thread then the UI thread can screw up
         * these steps. By putting this on the UI thread, there's no way
         * swapping or other UI actions such as switching from 1 to 4 panes can
         * interfere with this initialization.
         */
        final List<Thread> threads = new ArrayList<>();
        VizApp.runSync(new Runnable() {
            @Override
            public void run() {
                LoadItem[] items = null;
                try {
                    items = getLoadItems(container, bundle);
                } catch (VizException e) {
                    statusHandler.error(e.getLocalizedMessage(), e);
                    items = new LoadItem[0];
                }

                int numItems = items.length;
                if (numItems > 0) {
                    LoadItem first = items[0];
                    IRenderableDisplay loadFrom = first.loadFrom;
                    IDisplayPane loadTo = first.loadTo;

                    AbstractTimeMatcher destTimeMatcher = loadTo.getDescriptor()
                            .getTimeMatcher();
                    if (destTimeMatcher != null) {
                        AbstractTimeMatcher srcTimeMatcher = loadFrom
                                .getDescriptor().getTimeMatcher();
                        if (srcTimeMatcher != null) {
                            destTimeMatcher.copyFrom(srcTimeMatcher);
                        }
                        /*
                         * resetMultiload() must come before the
                         * load(IDisplayPane, IRenderableDisplay) call that is
                         * currently in the InstantiationTask constructor
                         */
                        destTimeMatcher.resetMultiload();
                    }
                    InstantiationTask task = new InstantiationTask(first);
                    Thread threadZero = new Thread(task);
                    threadZero.start();
                    threads.add(threadZero);

                    for (int i = 1; i < numItems; i++) {
                        Thread t = new Thread(new InstantiationTask(items[i]));
                        threads.add(t);
                    }
                }
            }
        });

        /*
         * the first thread must be allowed to finish before the others are
         * started
         */
        if (!threads.isEmpty()) {
            try {
                Thread threadZero = threads.get(0);
                threadZero.join();
            } catch (InterruptedException e) {
                statusHandler.error(
                        "Thread interrupted while waiting for first bundle to finish loading",
                        e);
            }
        }

        // start the other threads, if any
        for (int i = 1; i < threads.size(); i++) {
            threads.get(i).start();
        }

        // wait for all threads to finish before returning
        for (int i = 1; i < threads.size(); i++) {
            try {
                threads.get(i).join();
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    /**
     * Gets the pairing of display->pane loading that should occur. Each item
     * will have {@link #load(IDisplayPane, IRenderableDisplay)} called on it
     * 
     * @param container
     * @param bundle
     * @return
     * @throws VizException
     */
    protected LoadItem[] getLoadItems(IDisplayPaneContainer container,
            Bundle bundle) throws VizException {
        IDisplayPane[] containerPanes = container.getDisplayPanes();
        AbstractRenderableDisplay[] bundleDisplays = bundle.getDisplays();

        if (containerPanes.length != bundleDisplays.length) {
            boolean success = ensureOneToOne(container, bundle);
            containerPanes = container.getDisplayPanes();
            if (!success) {
                throw new VizException("Unable to load " + bundleDisplays.length
                        + " displays onto container with "
                        + containerPanes.length + " panes");
            }
        } else if (containerPanes.length > 1) {
            /*
             * Special case of loading a bundle/set of multiple panes into an
             * existing set of panes where the two sets have the same number of
             * panes. As we are loading the new panes we need to attempt to keep
             * their map resource properties in sync.
             */
            AbstractRenderableDisplay firstDisplay = bundleDisplays[0];
            List<ResourcePair> mapsOnFirst = new ArrayList<>();
            for (ResourcePair rp : firstDisplay.getDescriptor()
                    .getResourceList()) {
                if (rp.getProperties().isMapLayer()) {
                    mapsOnFirst.add(rp);
                }
            }

            for (int i = 1; i < bundleDisplays.length; i++) {
                ResourceList rlist = bundleDisplays[i].getDescriptor()
                        .getResourceList();
                for (ResourcePair rp : rlist) {
                    if (rp.getProperties().isMapLayer()) {
                        for (ResourcePair original : mapsOnFirst) {
                            if (rp.getResourceData()
                                    .equals(original.getResourceData())) {
                                /*
                                 * map is a match, reuse the reference to keep
                                 * map properties in sync
                                 */
                                rlist.remove(rp);
                                rlist.add(original);
                                break;
                            }
                        }
                    }
                }
            }

        }

        int numPanes = containerPanes.length;
        LoadItem[] items = new LoadItem[numPanes];

        List<AbstractRenderableDisplay> orderedDisplays = Arrays
                .asList(bundleDisplays);
        for (int i = 0; i < numPanes; ++i) {
            IDescriptor desc = bundleDisplays[i].getDescriptor();
            if (desc.getTimeMatcher() != null) {
                orderedDisplays = desc.getTimeMatcher()
                        .getDisplayLoadOrder(orderedDisplays);
                for (AbstractRenderableDisplay d : orderedDisplays) {
                    d.getDescriptor().synchronizeTimeMatching(desc);
                }
                break;
            }
        }
        if (orderedDisplays.size() != numPanes) {
            throw new VizException(
                    "Error ordering bundle displays. Number of displays returned not same as passed in");
        }

        int j = 0;
        for (AbstractRenderableDisplay display : orderedDisplays) {
            for (int i = 0; i < numPanes; ++i) {
                if (display == bundleDisplays[i]) {
                    items[j] = new LoadItem(containerPanes[i],
                            bundleDisplays[i]);
                }
            }
            ++j;
        }

        return items;
    }

    /**
     * Ensures there is a one to one relationship for number of panes on
     * container to number of displays in bundle
     * 
     * @param container
     * @param bundle
     * @return true of mapping is 1-1, false otherwise
     */
    protected boolean ensureOneToOne(IDisplayPaneContainer container,
            Bundle bundle) {
        IDisplayPane[] containerPanes = container.getDisplayPanes();
        AbstractRenderableDisplay[] bundleDisplays = bundle.getDisplays();

        // Attempt to match 1-1 pane to display
        if (container instanceof IMultiPaneEditor) {
            final IMultiPaneEditor mpe = (IMultiPaneEditor) container;
            final int numPanes = containerPanes.length;
            final int numDisplays = bundleDisplays.length;
            final IDisplayPane[] cPanes = containerPanes;
            final AbstractRenderableDisplay[] bDisplays = bundleDisplays;
            VizApp.runSync(new Runnable() {
                @Override
                public void run() {
                    if (numPanes < numDisplays) {
                        /*
                         * fewer panes than displays, remove the panes to ensure
                         * we don't keep any of their state around
                         */
                        for (int i = numPanes - 1; i > -1; i--) {
                            mpe.removePane(cPanes[i]);
                        }

                        // now add in the displays
                        for (int i = 0; i < numDisplays; ++i) {
                            mpe.addPane(bDisplays[i]);
                        }
                    } else {
                        // fewer displays than panes
                        for (int i = numDisplays; i < numPanes; ++i) {
                            mpe.removePane(cPanes[i]);
                        }
                    }
                }
            });
        }
        containerPanes = container.getDisplayPanes();
        return containerPanes.length == bundleDisplays.length;
    }

    /**
     * Loads the renderable display onto the pane. Must be called on the UI
     * thread.
     * 
     * @param loadTo
     * @param loadFrom
     */
    protected void load(final IDisplayPane loadTo,
            final IRenderableDisplay loadFrom) {
        IRenderableDisplay oldDisplay = loadTo.getRenderableDisplay();
        loadTo.setRenderableDisplay(loadFrom);
        if (oldDisplay != null && oldDisplay != loadFrom) {
            oldDisplay.dispose();
        }

    }

    /**
     * Gets a bundle object from bundle text, text type is specified by
     * {@link BundleInfoType} passed in
     * 
     * @param bundleText
     * @param variables
     * @param type
     * @return a bundle
     * @throws VizException
     */
    public static Bundle getBundle(String bundleText,
            Map<String, String> variables, BundleInfoType type)
            throws VizException {
        /** Make sure bundle text is not null */
        if (bundleText == null) {
            throw new IllegalArgumentException("Bundle text cannot be null");
        }

        Bundle b = null;
        /** Is the bundle location the bundle xml or a file with the xml? */
        if (type == BundleInfoType.FILE_LOCATION) {
            /** File with xml */
            File file = PathManagerFactory.getPathManager()
                    .getStaticFile(bundleText);
            if (file == null || !file.exists()) {
                throw new VizException("Cannot find bundle file: " + bundleText,
                        new FileNotFoundException(
                                file != null ? file.getPath() : "null"));
            }
            b = Bundle.unmarshalBundle(file, variables);
        } else {
            /** bundleLocation variable contains the xml */
            b = Bundle.unmarshalBundle(bundleText, variables);
        }

        return b;
    }

    /**
     * Schedules a {@link BundleLoader} to run to load the bundle on the
     * container
     * 
     * @param container
     * @param b
     */
    public static void loadTo(IDisplayPaneContainer container, Bundle b) {
        new BundleLoader(container, b).schedule();
    }
}
