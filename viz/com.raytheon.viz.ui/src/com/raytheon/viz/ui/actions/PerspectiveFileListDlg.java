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
package com.raytheon.viz.ui.actions;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.viz.ui.dialogs.localization.VizLocalizationFileListDlg;

/**
 * Lists the perspective localization file(s). Also allows users to browse
 * perspective localization files on their local file system.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 2, 2015  4401       bkowal      Initial creation
 * Jun 10, 2015 4401       bkowal      Fix comments.
 * Jun 30, 2015 4401       bkowal      Perspectives are now stored in common static.
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

public class PerspectiveFileListDlg extends VizLocalizationFileListDlg {

    private final String EXT = ".xml";

    public static enum FILE_SOURCE {
        LOCALIZATION, FILESYSTEM
    }

    private FILE_SOURCE fileSource = FILE_SOURCE.LOCALIZATION;

    private Button fileButton;

    /**
     * Constructor.
     * 
     * @param title
     * @param parent
     * @param mode
     * @param localizationDirectory
     */
    public PerspectiveFileListDlg(String title, Shell parent, Mode mode,
            String localizationDirectory) {
        super(title, parent, mode, localizationDirectory, "perspectives",
                LocalizationType.COMMON_STATIC);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.ui.dialogs.localization.VizLocalizationFileListDlg#
     * createButtonComp(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createButtonComp(Composite mainComp) {
        // Add button comp
        Composite buttonComp = new Composite(mainComp, SWT.NONE);
        buttonComp.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(SWT.CENTER, SWT.FILL, false, false);
        buttonComp.setLayoutData(gd);

        this.createOkButton(buttonComp);

        GridData rd = new GridData(80, SWT.DEFAULT);
        fileButton = new Button(buttonComp, SWT.PUSH);
        fileButton.setText("File...");
        fileButton.setLayoutData(rd);
        this.enableBtnArray.add(fileButton);
        fileButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleFileAction();
            }
        });

        this.createCancelButton(buttonComp);
    }

    private void handleFileAction() {
        String selectedFileName = null;
        FileDialog fd = new FileDialog(this.shell, SWT.SAVE);
        fd.setFileName(selectedFileName);
        fd.setFilterExtensions(new String[] { "*.xml" });
        fd.setFilterPath(System.getProperty("user.home"));
        String retVal = fd.open();
        if (retVal == null) {
            return;
        }

        String name = fd.getFileName();
        if (name != null && name.endsWith(EXT) == false) {
            name += EXT;
            fd.setFileName(name);
        }
        selectedFileName = fd.getFilterPath() + File.separator + name;
        if (new File(selectedFileName).exists()) {
            boolean result = MessageDialog.openQuestion(shell,
                    "Confirm Overwrite", "A file named \"" + name
                            + "\" already exists.  Do you want to replace it?");
            if (result == false) {
                return;
            }
        }

        this.fileName = selectedFileName;
        this.fileSource = FILE_SOURCE.FILESYSTEM;
        close();
    }

    /**
     * @return the fileSource
     */
    public FILE_SOURCE getFileSource() {
        return fileSource;
    }
}