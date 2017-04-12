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
package com.raytheon.uf.viz.localization.perspective.search.ui;

import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.viz.localization.perspective.search.LocalizationSearchFileProvider;
import com.raytheon.uf.viz.localization.perspective.search.LocalizationSearchQuery;
import com.raytheon.uf.viz.localization.perspective.view.PathData;
import com.raytheon.uf.viz.localization.perspective.view.PathDataExtManager;

/**
 * The starting point of a localization file search. The user enters a search
 * term and some other information to kick off the search.
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
public class LocalizationSearchPage extends DialogPage implements ISearchPage {

    private static final String ALL_LABEL = "<ALL>";

    /*
     * Saved state so that each time the page is created, the contents will be
     * the same as the last search performed.
     */
    private static volatile LocalizationSearchPageState lastState;

    private ISearchPageContainer container;

    private Text searchText;

    private Button caseSensitive;

    private Text extText;

    private Combo appCombo;

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));

        Label label = new Label(composite, SWT.NONE);
        label.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING,
                true, false, 2, 1));
        label.setText("Containing text:");
        searchText = new Text(composite, SWT.BORDER);
        searchText.setLayoutData(
                new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        searchText.addModifyListener(e -> validate());
        caseSensitive = new Button(composite, SWT.CHECK);
        caseSensitive.setText("Case Sensitive");
        caseSensitive.setSelection(true);

        label = new Label(composite, SWT.NONE);
        label.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING,
                true, false, 2, 1));
        label.setText("File Extension:");
        extText = new Text(composite, SWT.BORDER);
        extText.setLayoutData(
                new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        extText.setToolTipText(
                "Leave this blank to search all files or provide an extension to limit the search to only files that end with the specified extension. ");
        // Filler for grid layout.
        label = new Label(composite, SWT.NONE);

        label = new Label(composite, SWT.NONE);
        label.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING,
                true, false, 2, 1));
        label.setText("(For example: .xml)");

        label = new Label(composite, SWT.NONE);
        label.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING,
                true, false, 2, 1));
        label.setText("Application:");
        appCombo = new Combo(composite, SWT.READ_ONLY);

        SortedSet<String> applications = new TreeSet<>();
        for (PathData pathData : PathDataExtManager.getPathData()) {
            applications.add(pathData.getApplication());
        }
        applications.add(ALL_LABEL);

        appCombo.setItems(applications.toArray(new String[0]));
        appCombo.select(applications.headSet(ALL_LABEL).size());

        label = new Label(composite, SWT.NONE);

        if (lastState != null) {
            searchText.setText(lastState.getSearchTerm());
            caseSensitive.setSelection(lastState.isCaseSensitive());
            extText.setText(lastState.getExtensions());
            appCombo.setText(lastState.getApplication());
        }

        setControl(composite);
    }

    protected void validate() {
        if (searchText.getText().isEmpty()) {
            setErrorMessage("Enter search text.");
            container.setPerformActionEnabled(false);
        } else {
            setErrorMessage(null);
            container.setPerformActionEnabled(true);
        }
    }

    @Override
    public boolean performAction() {
        IPathManager pathManager = PathManagerFactory.getPathManager();

        String application = appCombo.getText();
        String extension = extText.getText();
        if (application.equals(ALL_LABEL)) {
            application = null;
        }
        if (extension.isEmpty()) {
            extension = null;
        }
        LocalizationSearchFileProvider fileProvider = new LocalizationSearchFileProvider(
                pathManager, extension, application);
        LocalizationSearchQuery query = new LocalizationSearchQuery(
                fileProvider, searchText.getText(),
                caseSensitive.getSelection());
        NewSearchUI.runQueryInBackground(query);

        LocalizationSearchPageState state = new LocalizationSearchPageState();
        state.setSearchTerm(searchText.getText());
        state.setCaseSensitive(caseSensitive.getSelection());
        state.setExtensions(extText.getText());
        state.setApplication(appCombo.getText());
        lastState = state;

        return true;
    }

    @Override
    public void setContainer(ISearchPageContainer container) {
        this.container = container;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        /*
         * During createControl the container does not always update the enabled
         * state in validate so it is necessary to wait to validate until the
         * page becomes visible.
         */
        if (visible) {
            validate();
        }
    }

}
