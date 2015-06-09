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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.internal.WorkbenchPage;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.LocalizationFileOutputStream;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.exception.LocalizationOpFailedException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.procedures.Bundle;
import com.raytheon.uf.viz.core.procedures.Procedure;
import com.raytheon.viz.ui.IRenameablePart;
import com.raytheon.viz.ui.UiUtil;
import com.raytheon.viz.ui.UiUtil.ContainerPart;
import com.raytheon.viz.ui.UiUtil.ContainerPart.Container;
import com.raytheon.viz.ui.actions.PerspectiveFileListDlg.FILE_SOURCE;
import com.raytheon.viz.ui.dialogs.ICloseCallback;
import com.raytheon.viz.ui.dialogs.localization.VizLocalizationFileListDlg;

/**
 * SavePerspectiveHandler
 * 
 * Save a procedure that currently maps the current state of the screen
 * including the editor and any side views.
 * 
 * <pre>
 * 
 *    SOFTWARE HISTORY
 *   
 *    Date         Ticket#     Engineer    Description
 *    ------------ ----------  ----------- --------------------------
 *    Sep 11, 2007             chammack    Initial Creation.
 *    Mar 02, 2015  4204       njensen     Set bundle name to part name
 *    Jun 02, 2015  4401       bkowal      It is now also possible to load displays from
 *                                         localization. Renamed class; originally SaveProcedure.
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1
 */
public class SavePerspectiveHandler extends AbstractHandler {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SavePerspectiveHandler.class);

    public static final String PERSPECTIVES_DIR = "/perspectives";

    private PerspectiveFileListDlg saveAsDlg;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands
     * .ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();
        if (this.saveAsDlg == null || this.saveAsDlg.getShell() == null
                || this.saveAsDlg.isDisposed()) {
            saveAsDlg = new PerspectiveFileListDlg(
                    "Save Perspective Display As...", shell,
                    VizLocalizationFileListDlg.Mode.SAVE, PERSPECTIVES_DIR);
            saveAsDlg.setCloseCallback(new ICloseCallback() {

                @Override
                public void dialogClosed(Object returnValue) {
                    String fn = saveAsDlg.getSelectedFileName();
                    if (fn == null) {
                        return;
                    }

                    if (saveAsDlg.getFileSource() == FILE_SOURCE.LOCALIZATION) {
                        saveProcedureLocalization(fn);
                    } else if (saveAsDlg.getFileSource() == FILE_SOURCE.FILESYSTEM) {
                        saveProcedureFileSystem(fn);
                    }
                }

            });
            saveAsDlg.open();
        } else {
            this.saveAsDlg.bringToTop();
        }

        return null;
    }

    private void saveProcedureLocalization(final String fileName) {
        String xml = null;
        Procedure procedure = getCurrentProcedure();
        try {
            xml = procedure.toXML();
        } catch (VizException e) {
            final String errMsg = "Error occurred during procedure save.";
            statusHandler.handle(Priority.CRITICAL, errMsg, e);
            return;
        }

        IPathManager pm = PathManagerFactory.getPathManager();

        LocalizationContext context = pm.getContext(
                LocalizationType.CAVE_STATIC, LocalizationLevel.USER);

        LocalizationFile localizationFile = pm.getLocalizationFile(context,
                PERSPECTIVES_DIR + File.separator + fileName);

        LocalizationFileOutputStream lfos = null;
        boolean writeSuccessful = false;
        try {
            lfos = localizationFile.openOutputStream();
            lfos.write(xml.getBytes());
            writeSuccessful = true;
        } catch (Exception e) {
            statusHandler.handle(Priority.CRITICAL,
                    "Failed to write localization file: " + fileName + ".", e);
        } finally {
            if (lfos != null) {
                try {
                    if (writeSuccessful) {
                        lfos.closeAndSave();
                    } else {
                        lfos.close();
                    }
                } catch (Exception e) {
                    statusHandler.handle(Priority.CRITICAL,
                            "Failed to save localization file: " + fileName
                                    + ".", e);
                }
            }
        }
    }

    private void saveProcedureFileSystem(final String fileName) {
        final Path procedurePath = Paths.get(fileName);
        try (BufferedWriter br = Files.newBufferedWriter(procedurePath,
                Charset.defaultCharset())) {
            Procedure procedure = getCurrentProcedure();
            String xml = procedure.toXML();
            br.write(xml);
        } catch (Exception e) {
            statusHandler.handle(Priority.CRITICAL,
                    "Failed to write perspective file: " + fileName + ".", e);
        }
    }

    public static Procedure getCurrentProcedure() {
        Procedure procedure = new Procedure();

        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();
        String perspectiveId = page.getPerspective().getId();

        procedure.setPerspective(perspectiveId);
        IMemento layout = XMLMemento.createWriteRoot("perspectiveLayout");
        ((WorkbenchPage) page).getEditorPresentation().saveState(layout);
        procedure.setLayout(layout);

        List<Bundle> bundleList = new ArrayList<Bundle>();

        List<ContainerPart> panes = UiUtil.getActiveDisplayMap();
        for (ContainerPart part : panes) {
            for (Container c : part.containers) {
                IRenderableDisplay[] displayArr = c.displays;
                Bundle b = new Bundle();
                if (displayArr.length > 0) {
                    b.setLoopProperties(displayArr[0].getContainer()
                            .getLoopProperties());

                    if (displayArr[0].getContainer() instanceof IRenameablePart) {
                        String partName = ((IRenameablePart) displayArr[0]
                                .getContainer()).getPartName();
                        if (partName != null) {
                            b.setName(partName);
                        }
                    }
                }
                String key = part.id;
                b.setLayoutId(c.layoutId);
                if (UiUtil.isEditor(key)) {
                    b.setEditor(key);
                } else if (UiUtil.isView(key)) {
                    b.setView(key);
                }

                List<AbstractRenderableDisplay> displays = new ArrayList<AbstractRenderableDisplay>();
                for (IRenderableDisplay disp : displayArr) {
                    if (disp instanceof AbstractRenderableDisplay) {
                        displays.add((AbstractRenderableDisplay) disp);
                    }
                }

                if (displays.size() > 0) {
                    b.setDisplays(displays
                            .toArray(new AbstractRenderableDisplay[displays
                                    .size()]));
                    bundleList.add(b);
                }
            }
        }

        if (bundleList.size() > 0) {
            procedure.setBundles(bundleList.toArray(new Bundle[bundleList
                    .size()]));
        }
        return procedure;
    }
}
