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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.internal.WorkbenchPage;

import com.raytheon.uf.common.status.UFStatus.Priority;
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
 *    Jun 10, 2015  4401       bkowal      Extend {@link AbstractVizPerspectiveLocalizationHandler}.
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1
 */
@SuppressWarnings("restriction")
public class SavePerspectiveHandler extends
        AbstractVizPerspectiveLocalizationHandler {

    private PerspectiveFileListDlg saveAsDlg;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands
     * .ExecutionEvent)
     */
    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();
        if (this.saveAsDlg == null || this.saveAsDlg.getShell() == null
                || this.saveAsDlg.isDisposed()) {
            saveAsDlg = new PerspectiveFileListDlg(
                    "Save Bundle", shell,
                    VizLocalizationFileListDlg.Mode.SAVE, PERSPECTIVES_DIR);
            saveAsDlg.setCloseCallback(new ICloseCallback() {

                @Override
                public void dialogClosed(Object returnValue) {
                    String fn = saveAsDlg.getSelectedFileName();
                    if (fn == null) {
                        return;
                    }

                    if (saveAsDlg.getFileSource() == FILE_SOURCE.LOCALIZATION) {
                        savePerspectiveLocalization(fn, event);
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

    private void savePerspectiveLocalization(final String fileName,
            final ExecutionEvent event) {
        String xml = null;
        Procedure procedure = getCurrentProcedure();
        try {
            xml = procedure.toXML();
        } catch (VizException e) {
            final String errMsg = "Error occurred during procedure save.";
            statusHandler.handle(Priority.CRITICAL, errMsg, e);
            return;
        }

        this.savePerspectiveLocalization(fileName, xml.getBytes(), event);
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

        if (bundleList.isEmpty() == false) {
            procedure.setBundles(bundleList.toArray(new Bundle[bundleList
                    .size()]));
        }
        return procedure;
    }
}
