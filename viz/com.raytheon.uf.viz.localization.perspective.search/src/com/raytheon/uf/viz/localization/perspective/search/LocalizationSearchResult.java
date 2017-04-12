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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorInput;

/**
 * 
 * The results of a {@link LocalizationSearchQuery}.
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
public class LocalizationSearchResult extends AbstractTextSearchResult
        implements IEditorMatchAdapter {

    private final LocalizationSearchQuery query;

    public LocalizationSearchResult(LocalizationSearchQuery query) {
        this.query = query;
    }

    @Override
    public String getLabel() {
        return "'" + query.getSearchTerm() + "' - " + getMatchCount()
                + " matches " + query.getFileProvider().getLabel();
    }

    @Override
    public String getTooltip() {
        return query.getLabel();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        /*
         * This image would be used in the search history to differentiate this
         * type of result from other results. I'm not creative enough to come up
         * with something useful here.
         */
        return null;
    }

    @Override
    public LocalizationSearchQuery getQuery() {
        return query;
    }

    @Override
    public boolean isShownInEditor(Match match, IEditorPart editor) {
        IEditorInput ei = editor.getEditorInput();
        if (ei instanceof LocalizationEditorInput) {
            LocalizationEditorInput li = (LocalizationEditorInput) ei;
            return match.getElement().equals(li.getLocalizationFile());
        }
        return false;
    }

    @Override
    public Match[] computeContainedMatches(AbstractTextSearchResult result,
            IEditorPart editor) {
        IEditorInput ei = editor.getEditorInput();
        if (ei instanceof LocalizationEditorInput) {
            LocalizationEditorInput li = (LocalizationEditorInput) ei;
            return getMatches(li.getLocalizationFile());
        }
        return new Match[0];
    }

    @Override
    public IEditorMatchAdapter getEditorMatchAdapter() {
        return this;
    }

    @Override
    public IFileMatchAdapter getFileMatchAdapter() {
        /*
         * Used to keep search results in sync as files are changed. This is not
         * currently implemented because it is difficult to convert from an
         * IFile to an ILocalizationFile. It is theoretically possible, but
         * doesn't seem worthwhile at this time.
         */
        return null;
    }

}
