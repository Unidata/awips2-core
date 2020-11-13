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
import org.eclipse.e4.ui.model.application.ui.advanced.MArea;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.internal.IWorkbenchConstants;
import org.eclipse.ui.services.IServiceLocator;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
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
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 11, 2007           chammack  Initial Creation.
 * Mar 02, 2015  4204     njensen   Set bundle name to part name
 * Jun 02, 2015  4401     bkowal    It is now also possible to load displays
 *                                  from localization. Renamed class; originally
 *                                  SaveProcedure.
 * Jun 10, 2015  4401     bkowal    Extend {@link
 *                                  AbstractVizPerspectiveLocalizationHandler}.
 * Dec 21, 2015  5191     bsteffen  Updated layout handling for Eclipse 4.
 * Jun 22, 2017  4818     mapeters  Changed setCloseCallback to addCloseCallback
 * Oct 12, 2020  8241     randerso  Removed obsolete tag
 * 
 * </pre>
 *
 * @author chammack
 */
public class SavePerspectiveHandler
        extends AbstractVizPerspectiveLocalizationHandler {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SavePerspectiveHandler.class);

    private PerspectiveFileListDlg saveAsDlg;

    @Override
    public Object execute(final ExecutionEvent event)
            throws ExecutionException {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();
        if (this.saveAsDlg == null || this.saveAsDlg.getShell() == null
                || this.saveAsDlg.isDisposed()) {
            saveAsDlg = new PerspectiveFileListDlg(
                    "Save Perspective Display As...", shell,
                    VizLocalizationFileListDlg.Mode.SAVE, PERSPECTIVES_DIR);
            saveAsDlg.addCloseCallback(new ICloseCallback() {

                @Override
                public void dialogClosed(Object returnValue) {
                    String fn = saveAsDlg.getSelectedFileName();
                    if (fn == null) {
                        return;
                    }

                    if (saveAsDlg.getFileSource() == FILE_SOURCE.LOCALIZATION) {
                        savePerspectiveLocalization(fn, event);
                    } else if (saveAsDlg
                            .getFileSource() == FILE_SOURCE.FILESYSTEM) {
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
        IServiceLocator services = page.getWorkbenchWindow();
        MWindow window = services.getService(MWindow.class);
        EModelService modelService = services.getService(EModelService.class);
        MPerspective perspective = modelService.getActivePerspective(window);
        List<MArea> areas = modelService.findElements(perspective,
                IPageLayout.ID_EDITOR_AREA, MArea.class, null);
        if (areas.size() == 1) {
            MArea editorArea = areas.get(0);
            editorArea = (MArea) modelService.cloneElement(editorArea, window);
            try {
                @SuppressWarnings("restriction")
                IMemento info = layout
                        .createChild(IWorkbenchConstants.TAG_INFO);
                saveLayout(layout, editorArea, info);
            } catch (IllegalStateException e) {
                statusHandler.handle(Priority.WARN,
                        "Unable to save editor layout", e);
            }
        }
        procedure.setLayout(layout);

        List<Bundle> bundleList = new ArrayList<>();

        List<ContainerPart> panes = UiUtil.getActiveDisplayMap();
        for (ContainerPart part : panes) {
            for (Container c : part.containers) {
                IRenderableDisplay[] displayArr = c.displays;
                Bundle b = new Bundle();
                if (displayArr.length > 0) {
                    b.setLoopProperties(
                            displayArr[0].getContainer().getLoopProperties());

                    if (displayArr[0]
                            .getContainer() instanceof IRenameablePart) {
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

                List<AbstractRenderableDisplay> displays = new ArrayList<>();
                for (IRenderableDisplay disp : displayArr) {
                    if (disp instanceof AbstractRenderableDisplay) {
                        displays.add((AbstractRenderableDisplay) disp);
                    }
                }

                if (!displays.isEmpty()) {
                    b.setDisplays(displays.toArray(
                            new AbstractRenderableDisplay[displays.size()]));
                    bundleList.add(b);
                }
            }
        }

        if (!bundleList.isEmpty()) {
            procedure.setBundles(
                    bundleList.toArray(new Bundle[bundleList.size()]));
        }
        return procedure;
    }

    /**
     * Attempt to extract the layout information from a
     * {@link MPartSashContainer} into an {@link IMemento}. The memento is
     * designed to be compatible with Eclipse 3.8 layout mementos. If the
     * container contains elements that are not representable using this format
     * than an {@link IllegalStateException} will be thrown.
     *
     * @param root
     *            The root memento that contains a single "info" memento for
     *            each PartStack in the container hierarchy.
     * @param container
     *            The container for which the layout is being saved.
     * @param leftInfo
     *            A memento, which is a child of root, which will be populated
     *            for the first(or "left") child of the container.
     */
    @SuppressWarnings("restriction")
    private static void saveLayout(IMemento root, MPartSashContainer container,
            IMemento leftInfo) {
        List<MPartSashContainerElement> children = container.getChildren();
        if (children.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot save layout of empty container.");
        } else if (children.size() > 2) {
            throw new IllegalStateException(
                    "Cannot save layout of container with more than two children( "
                            + children.size() + ").");
        }
        IMemento rightInfo = null;
        if (children.size() == 2) {
            /*
             * Because the order of the info elements matter, there must be an
             * info element for both of this containers children before
             * recursion on either side. The left info is passed in so must make
             * one for the right.
             */
            rightInfo = root.createChild(IWorkbenchConstants.TAG_INFO);
        }
        MPartSashContainerElement left = children.get(0);
        if (left instanceof MPartStack) {
            leftInfo.putString(IWorkbenchConstants.TAG_PART,
                    left.getElementId());
        } else if (left instanceof MPartSashContainer) {
            saveLayout(root, (MPartSashContainer) left, leftInfo);
        } else {
            throw new IllegalStateException(
                    "Unrecognized UI element in layout: "
                            + left.getClass().getSimpleName());
        }
        if (rightInfo != null) {
            MPartSashContainerElement right = children.get(1);

            int relationship = container.isHorizontal() ? IPageLayout.RIGHT
                    : IPageLayout.BOTTOM;
            int ratioLeft = Integer.parseInt(left.getContainerData());
            int ratioRight = Integer.parseInt(right.getContainerData());

            rightInfo.putString(IWorkbenchConstants.TAG_RELATIVE,
                    leftInfo.getString(IWorkbenchConstants.TAG_PART));
            rightInfo.putInteger(IWorkbenchConstants.TAG_RELATIONSHIP,
                    relationship);
            rightInfo.putInteger(IWorkbenchConstants.TAG_RATIO_LEFT, ratioLeft);
            rightInfo.putInteger(IWorkbenchConstants.TAG_RATIO_RIGHT,
                    ratioRight);
            {
                /*
                 * This block adds details to the info that is not used in
                 * LoadPerspectiveHandler, it is included for Eclipse 3.8
                 * compatibility. After full migration to eclipse 4 it can be
                 * removed.
                 */
                rightInfo.putFloat(IWorkbenchConstants.TAG_RATIO,
                        ratioLeft / (float) (ratioRight + ratioLeft));
                IMemento folder = rightInfo
                        .createChild(IWorkbenchConstants.TAG_FOLDER);
                folder.putInteger(IWorkbenchConstants.TAG_EXPANDED, 2);
                IMemento presentation = folder
                        .createChild(IWorkbenchConstants.TAG_PRESENTATION);
                presentation.putString(IWorkbenchConstants.TAG_ID,
                        "com.raytheon.uf.viz.personalities.cave.presentation.VizPresentationFactory");
                IMemento part = presentation
                        .createChild(IWorkbenchConstants.TAG_PART);
                part.putInteger(IWorkbenchConstants.TAG_ID, 0);
            }
            if (right instanceof MPartStack) {
                rightInfo.putString(IWorkbenchConstants.TAG_PART,
                        right.getElementId());
            } else if (right instanceof MPartSashContainer) {
                saveLayout(root, (MPartSashContainer) right, rightInfo);
            } else {
                throw new IllegalStateException(
                        "Unrecognized UI element in layout: "
                                + right.getClass().getSimpleName());
            }
        }
    }

}
