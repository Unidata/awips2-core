package com.raytheon.uf.viz.localization.perspective.search.ui;

import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.viz.localization.perspective.search.LocalizationSearchFileProvider;
import com.raytheon.uf.viz.localization.perspective.search.ui.result.FileTreeEntryDataComparator;
import com.raytheon.uf.viz.localization.perspective.search.ui.result.ResourceSelectionDetailsLabelProvider;
import com.raytheon.uf.viz.localization.perspective.search.ui.result.ResourceSelectionListLabelProvider;
import com.raytheon.uf.viz.localization.perspective.view.FileTreeEntryData;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileEntryData;
import com.raytheon.uf.viz.localization.perspective.view.PathData;

/**
 * A dialog for selecting a localization file by filtering all the available
 * files in the localization perspective by name.
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
public class LocalizationResourceSelectionDialog
        extends FilteredItemsSelectionDialog {

    private final LocalizationSearchFileProvider fileProvider;

    private final Job providerLoadJob = new Job("Loading file for search") {

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            fileProvider.run(monitor);
            return Status.OK_STATUS;
        }

    };

    public LocalizationResourceSelectionDialog(Shell shell) {
        super(shell);
        setTitle("Open Localization File");
        setListLabelProvider(new ResourceSelectionListLabelProvider());
        setDetailsLabelProvider(new ResourceSelectionDetailsLabelProvider());

        fileProvider = new LocalizationSearchFileProvider(
                PathManagerFactory.getPathManager(), null, null);
        /*
         * This set of file names doesn't change and can take some time to
         * initialize so get it started as soon as the dialog is opened.
         */
        providerLoadJob.schedule();
    }

    @Override
    protected Control createExtendedContentArea(Composite parent) {
        return null;
    }

    @Override
    protected IDialogSettings getDialogSettings() {
        return new DialogSettings(getClass().getSimpleName());
    }

    @Override
    protected IStatus validateItem(Object item) {
        return Status.OK_STATUS;
    }

    @Override
    protected ItemsFilter createFilter() {
        return new ItemsFilter() {

            @Override
            public boolean matchItem(Object item) {
                ILocalizationFile file = ((LocalizationFileEntryData) item)
                        .getFile();
                String path = file.getPath();
                if (patternMatcher.getPattern().contains(IPathManager.SEPARATOR)
                        && matches(path)) {
                    return true;
                }
                String[] splits = path.split(IPathManager.SEPARATOR);
                return matches(splits[splits.length - 1]);
            }

            @Override
            public boolean isConsistentItem(Object item) {
                return item instanceof LocalizationFileEntryData;
            }

        };
    }

    @Override
    protected Comparator<FileTreeEntryData> getItemsComparator() {
        return new FileTreeEntryDataComparator();
    }

    @Override
    protected void fillContentProvider(AbstractContentProvider contentProvider,
            ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
            throws CoreException {
        try {
            providerLoadJob.join();
        } catch (InterruptedException e) {
            throw new CoreException(new Status(IStatus.ERROR,
                    "com.raytheon.uf.viz.localization.search",
                    "Unexpected interruption", e));
        }

        progressMonitor.beginTask("Searching", fileProvider.getFileCount());
        for (PathData pathData : fileProvider.getPaths()) {
            for (ILocalizationFile file : fileProvider.getFiles(pathData)) {
                if (progressMonitor.isCanceled()) {
                    return;
                }
                LocalizationFileEntryData entry = new LocalizationFileEntryData(
                        pathData, (LocalizationFile) file, false);
                contentProvider.add(entry, itemsFilter);
                progressMonitor.worked(1);
            }
        }
    }

    @Override
    public String getElementName(Object item) {
        return item.toString();
    }

    @Override
    public boolean close() {
        if (super.close()) {
            providerLoadJob.cancel();
            return true;
        }
        return false;
    }

}
