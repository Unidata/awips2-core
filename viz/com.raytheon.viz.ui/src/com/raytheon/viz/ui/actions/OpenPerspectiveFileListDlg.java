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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.viz.ui.dialogs.localization.VizOpenLocalizationFileListDlg;

/**
 * Allows users to open perspective files in localization or their local file
 * system.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 2, 2015  4401       bkowal      Initial creation
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

public class OpenPerspectiveFileListDlg extends VizOpenLocalizationFileListDlg {

    public static enum FILE_SOURCE {
        LOCALIZATION, FILESYSTEM
    }

    private FILE_SOURCE fileSource = FILE_SOURCE.LOCALIZATION;

    private Button fileButton;

    /**
     * Constructor.
     * 
     * @param parent
     * @param localizationDirectory
     */
    public OpenPerspectiveFileListDlg(Shell parent, String localizationDirectory) {
        super("Open Perspective Display", parent, localizationDirectory,
                "perspectives");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.ui.dialogs.localization.VizLocalizationFileListDlg#
     * createButtonComp(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createButtonComp(Composite mainComp) {
        // Add buttom comp
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
        FileDialog fd = new FileDialog(this.shell, SWT.OPEN);
        fd.setFileName(selectedFileName);
        fd.setFilterExtensions(new String[] { "*.xml" });
        fd.setFilterPath(System.getProperty("user.home"));
        String retVal = fd.open();
        if (retVal == null) {
            return;
        }

        String name = fd.getFileName();
        if (name != null && name.endsWith(".xml") == false) {
            name += ".xml";
            fd.setFileName(name);
        }
        selectedFileName = fd.getFilterPath() + File.separator + name;
        if (new File(selectedFileName).exists() == false) {
            return;
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