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

import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultPage;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorInput;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorUtils;
import com.raytheon.uf.viz.localization.perspective.search.LocalizationSearchResult;
import com.raytheon.uf.viz.localization.perspective.service.LocalizationPerspectiveUtils;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileEntryData;

/**
 * {@link ISearchResultPage} for {@link LocalizationSearchResult}.
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
public class LocalizationSearchResultPage extends AbstractTextSearchViewPage {

    @Override
    protected void elementsChanged(Object[] objects) {
        getViewer().refresh();
    }

    @Override
    protected void clear() {
    }

    @Override
    public void setInput(ISearchResult newSearch, Object viewState) {
        super.setInput(newSearch, viewState);
        getViewer().setInput(newSearch);
    }

    @Override
    protected void configureTableViewer(TableViewer viewer) {
        viewer.setContentProvider(new LSRListContentProvider());
        viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
                new LSRListLabelProvider(this)));
    }

    @Override
    protected void configureTreeViewer(TreeViewer viewer) {
        viewer.setContentProvider(new LSRTreeContentProvider());
        viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
                new LSRTreeLabelProvider(this)));
    }

    @Override
    protected void handleOpen(OpenEvent event) {
        Object element = ((IStructuredSelection) event.getSelection())
                .getFirstElement();
        if (element instanceof LocalizationFileEntryData) {
            LocalizationFile file = ((LocalizationFileEntryData) element)
                    .getFile();
            IWorkbenchPage page = getSite().getPage();
            LocalizationPerspectiveUtils.getService(page).openFile(file);
            return;
        }
        super.handleOpen(event);
    }

    @Override
    protected void showMatch(Match match, int currentOffset, int currentLength,
            boolean activate) throws PartInitException {
        LocalizationFile file = (LocalizationFile) match.getElement();
        IWorkbenchPage page = getSite().getPage();
        LocalizationPerspectiveUtils.getService(page).openFile(file);

        IEditorPart editor = LocalizationEditorUtils.getEditorForFile(page,
                file);

        /*
         * The ability to highlight a match was adapted from
         * org.eclipse.search.internal.ui.text.EditorOpener. That class cannot
         * be used directly because it works with IFiles instead of
         * ILocalizationFiles and it cannot be extended because it is internal.
         */
        if (editor instanceof ITextEditor) {
            ITextEditor textEditor = (ITextEditor) editor;
            textEditor.selectAndReveal(currentOffset, currentLength);
        } else if (editor != null) {
            IFile efile = ((LocalizationEditorInput) editor.getEditorInput())
                    .getFile();
            try {
                IMarker marker = efile.createMarker(NewSearchUI.SEARCH_MARKER);
                HashMap<String, Integer> attributes = new HashMap<>(4);
                attributes.put(IMarker.CHAR_START, new Integer(currentOffset));
                attributes.put(IMarker.CHAR_END,
                        new Integer(currentOffset + currentLength));
                marker.setAttributes(attributes);
                IDE.gotoMarker(editor, marker);
            } catch (CoreException e) {
                throw new PartInitException("Error loading marker", e);
            }
        }
    }

    @Override
    public Match[] getDisplayedMatches(Object element) {
        if (element instanceof Match) {
            return new Match[] { (Match) element };
        } else if (element instanceof LocalizationFileEntryData) {
            return getInput().getMatches(
                    ((LocalizationFileEntryData) element).getFile());
        }
        return super.getDisplayedMatches(element);
    }

    @Override
    public int getDisplayedMatchCount(Object element) {
        if (element instanceof Match) {
            return getInput().getMatchCount(((Match) element).getElement());
        }
        return super.getDisplayedMatchCount(element);
    }

}
