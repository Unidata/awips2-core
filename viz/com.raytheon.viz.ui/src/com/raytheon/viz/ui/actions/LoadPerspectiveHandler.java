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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MArea;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.internal.IWorkbenchConstants;
import org.eclipse.ui.services.IServiceLocator;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.DescriptorMap;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.procedures.Bundle;
import com.raytheon.uf.viz.core.procedures.Procedure;
import com.raytheon.uf.viz.core.procedures.ProcedureXmlManager;
import com.raytheon.viz.ui.BundleLoader;
import com.raytheon.viz.ui.UiUtil;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.dialogs.ICloseCallback;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * Handles loading of bundles or procedures
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Apr 06, 2010           mschenke    Initial creation
 * Mar 21, 2013  1638     mschenke    Added method to load procedure to window
 * Oct 22, 2013  2491     bsteffen    Switch serialization to
 *                                    ProcedureXmlManager
 * Aug 11, 2014  3480     bclement    added info logging to execute()
 * Jun 02, 2015  4401     bkowal      It is now also possible to load perspectives from
 *                                    localization. Renamed class; originally LoadSerializedXml.
 * Jun 10, 2015  4401     bkowal      It is now possible to optionally upload a local file system file
 *                                    to localization when loading it.
 * Dec 21, 2015  5191     bsteffen    Updated layout handling for Eclipse 4.
 * Feb 11, 2016  5242     dgilling    Remove calls to deprecated Localization APIs.
 * Jun 22, 2017  4818     mapeters    Changed setCloseCallback to addCloseCallback
 *
 * </pre>
 *
 * @author mschenke
 */
public class LoadPerspectiveHandler
        extends AbstractVizPerspectiveLocalizationHandler {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(LoadPerspectiveHandler.class);

    private OpenPerspectiveFileListDlg dialog;

    @Override
    public Object execute(final ExecutionEvent event)
            throws ExecutionException {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();

        if (dialog == null || dialog.getShell() == null
                || dialog.isDisposed()) {
            dialog = new OpenPerspectiveFileListDlg(shell,
                    SavePerspectiveHandler.PERSPECTIVES_DIR);
            dialog.addCloseCallback(new ICloseCallback() {
                @Override
                public void dialogClosed(Object returnValue) {
                    if (returnValue instanceof LocalizationFile) {
                        loadFromLocalization((LocalizationFile) returnValue);
                    } else if (returnValue instanceof Path) {
                        Path filePath = (Path) returnValue;
                        if (dialog.importIntoLocalization()) {
                            importIntoLocalization(filePath, event);
                        }
                        loadFromFileSystem(filePath);
                    }
                }
            });
            dialog.open();
        } else {
            this.dialog.bringToTop();
        }

        return null;
    }

    private void loadFromLocalization(LocalizationFile localizationFile) {
        statusHandler.info(
                "Loading perspective file: " + localizationFile.getPath());

        Object obj = null;
        try (InputStream is = localizationFile.openInputStream()) {
            obj = ProcedureXmlManager.getInstance().unmarshal(is);
        } catch (Exception e) {
            statusHandler.handle(Priority.CRITICAL,
                    "Failed to deserialize perspective localization file: "
                            + localizationFile.getPath() + ".",
                    e);
            return;
        }
        this.load(obj, localizationFile.getPath());
    }

    private void importIntoLocalization(Path filePath,
            final ExecutionEvent event) {
        statusHandler.info("Importing local perspective file: "
                + filePath.toString() + " into localization.");

        final byte[] fileContents;
        try {
            fileContents = Files.readAllBytes(filePath);
        } catch (IOException e) {
            statusHandler.handle(Priority.CRITICAL,
                    "Failed to read localization file: " + filePath.toString()
                            + " for upload.",
                    e);
            return;
        }

        /*
         * Get the name of the file for localization.
         */
        String fileName = filePath.getFileName().toString();
        this.savePerspectiveLocalization(fileName, fileContents, event, true);
    }

    private void loadFromFileSystem(Path filePath) {
        statusHandler.info("Loading perspective file: " + filePath.toString());

        Object obj = null;
        try {
            obj = ProcedureXmlManager.getInstance().unmarshal(Object.class,
                    filePath.toFile());
        } catch (Exception e) {
            statusHandler.handle(Priority.CRITICAL,
                    "Failed to deserialize perspective file system file: "
                            + filePath.toString() + ".",
                    e);
            return;
        }
        this.load(obj, filePath.toString());
    }

    /**
     * Attempts to load the specified {@link Bundle} or {@link Procedure} for
     * display.
     *
     * @param obj
     *            the specified {@link Bundle} or {@link Procedure}
     * @param source
     *            identifying information used to notify the user if the
     *            {@link Bundle} or {@link Procedure} could not be successfully
     *            loaded.
     */
    private void load(Object obj, final String source) {
        if (obj == null) {
            return;
        }

        try {
            if (obj instanceof Procedure) {
                loadProcedureToScreen((Procedure) obj, false);
            } else if (obj instanceof Bundle) {
                loadBundle((Bundle) obj);
            }
        } catch (VizException e) {
            statusHandler.handle(Priority.CRITICAL,
                    "Failed to load perspective: " + source + ".", e);
        }
    }

    /**
     * @deprecated Use {@link ProcedureXmlManager#unmarshal(Class, String)}
     *             directly instead.
     */
    @Deprecated
    public static Object deserialize(File fileName) {
        Object obj = null;
        try {
            obj = ProcedureXmlManager.getInstance().unmarshal(Object.class,
                    fileName);
        } catch (Exception e) {
            String errMsg = "Error occurred during xml deserialization";
            statusHandler.handle(Priority.CRITICAL, errMsg, e);
        }
        return obj;
    }

    private void loadBundle(Bundle bundle) throws VizException {
        IRenderableDisplay renderableDisplay = bundle.getDisplays()[0];
        IDescriptor bundleDescriptor = renderableDisplay.getDescriptor();
        String bundleEditorId = DescriptorMap
                .getEditorId(bundleDescriptor.getClass().getName());
        AbstractEditor editor = UiUtil.createOrOpenEditor(bundleEditorId,
                bundle.getDisplays());

        BundleLoader.loadTo(editor, bundle);
    }

    public static void loadProcedureToScreen(Procedure procedure,
            boolean ignorePerspective) throws VizException {
        IWorkbenchWindow windowToLoadTo = VizWorkbenchManager.getInstance()
                .getCurrentWindow();
        String perspective = procedure.getPerspective();

        IWorkbenchPage page = null;
        if (perspective != null && !ignorePerspective) {
            try {
                page = PlatformUI.getWorkbench().showPerspective(perspective,
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow());
            } catch (WorkbenchException e) {
                throw new VizException("Opening perspective failed", e);
            }
        } else {
            page = windowToLoadTo.getActivePage();
        }

        loadProcedureToScreen(procedure, page.getWorkbenchWindow());
    }

    public static void loadProcedureToScreen(Procedure procedure,
            IWorkbenchWindow window) throws VizException {
        IWorkbenchPage page = window.getActivePage();

        // close existing containers
        for (IEditorReference ref : page.getEditorReferences()) {
            IEditorPart part = ref.getEditor(false);
            if (part != null) {
                page.closeEditor(part, false);
            }
        }

        if (procedure.getLayout() != null) {
            loadLayout(window, procedure.getLayout());
        }

        Bundle[] bundles = procedure.getBundles();
        for (Bundle b : bundles) {
            // If an editor is specified, or no view part is specified,
            // assume an editor part
            if (b.getView() == null) {
                String editorName = b.getEditor();
                AbstractEditor openedEditor = UiUtil.createEditor(editorName,
                        b.getDisplays());

                if (b.getLayoutId() != null) {
                    handleLayoutId(window, page, openedEditor, b.getLayoutId());
                }

                BundleLoader.loadTo(openedEditor, b);
            } else {
                // There is a view part specified
                IViewPart part = UiUtil.findView(window, b.getView(), false);

                if (part != null && part instanceof IDisplayPaneContainer) {
                    BundleLoader.loadTo((IDisplayPaneContainer) part, b);
                }
            }

        }
    }

    @SuppressWarnings("restriction")
    private static void loadLayout(IServiceLocator services,
            IMemento layoutMemento) {
        EModelService modelService = services.getService(EModelService.class);
        MWindow windowModel = services.getService(MWindow.class);
        MPerspective perspective = modelService
                .getActivePerspective(windowModel);

        List<MArea> areas = modelService.findElements(perspective,
                IPageLayout.ID_EDITOR_AREA, MArea.class, null);
        if (areas.isEmpty()) {
            statusHandler
                    .warn("Unable to restore layout because of missing area with id of: "
                            + IPageLayout.ID_EDITOR_AREA);
            return;
        }
        MArea editorArea = areas.get(0);
        IPresentationEngine presentationEngine = services
                .getService(IPresentationEngine.class);
        for (MUIElement element : editorArea.getChildren()) {
            presentationEngine.removeGui(element);
        }
        editorArea.getChildren().clear();
        IMemento[] infos = layoutMemento
                .getChildren(IWorkbenchConstants.TAG_INFO);
        for (IMemento info : infos) {
            String id = info.getString(IWorkbenchConstants.TAG_PART);
            MPartStack stack = modelService
                    .createModelElement(MPartStack.class);
            stack.setElementId(id);
            String relative = info.getString(IWorkbenchConstants.TAG_RELATIVE);
            if (relative == null) {
                editorArea.getChildren().add(stack);
                continue;
            } else {
                Integer relationship = info
                        .getInteger(IWorkbenchConstants.TAG_RELATIONSHIP);
                String ratioLeft = info
                        .getString(IWorkbenchConstants.TAG_RATIO_LEFT);
                String ratioRight = info
                        .getString(IWorkbenchConstants.TAG_RATIO_RIGHT);

                MUIElement modelRelative = modelService.find(relative,
                        editorArea);
                MElementContainer<MUIElement> parent = modelRelative
                        .getParent();
                /*
                 * Create a new container that will replace the relative and
                 * split the area between relative and the new stack.
                 */
                MPartSashContainer newContainer = modelService
                        .createModelElement(MPartSashContainer.class);
                newContainer.setContainerData(modelRelative.getContainerData());
                newContainer.setHorizontal(relationship == IPageLayout.LEFT
                        || relationship == IPageLayout.RIGHT);
                int index = parent.getChildren().indexOf(modelRelative);
                parent.getChildren().remove(modelRelative);
                /*
                 * left vs. right and top vs. bottom is controlled by order of
                 * children.
                 */
                if (relationship == IPageLayout.RIGHT
                        || relationship == IPageLayout.BOTTOM) {
                    modelRelative.setContainerData(ratioLeft);
                    stack.setContainerData(ratioRight);
                    newContainer.getChildren()
                            .add((MPartSashContainerElement) modelRelative);
                    newContainer.getChildren().add(stack);
                } else {
                    modelRelative.setContainerData(ratioRight);
                    stack.setContainerData(ratioLeft);
                    newContainer.getChildren().add(stack);
                    newContainer.getChildren()
                            .add((MPartSashContainerElement) modelRelative);
                }
                parent.getChildren().add(index, newContainer);
            }
        }
    }

    @SuppressWarnings("restriction")
    private static void handleLayoutId(IServiceLocator services,
            IWorkbenchPage page, IEditorPart editor, String layoutId) {
        EModelService modelService = services.getService(EModelService.class);
        MWindow windowModel = services.getService(MWindow.class);
        MPerspective perspective = modelService
                .getActivePerspective(windowModel);

        MUIElement parent = modelService.find(layoutId, perspective);
        if (parent instanceof MPartStack) {
            MPartStack stack = (MPartStack) parent;
            MPart part = ((org.eclipse.ui.internal.WorkbenchPage) page)
                    .findPart(editor);
            MStackElement e2 = stack.getSelectedElement();
            stack.getChildren().add(part);
            if (e2 == null) {
                stack.setSelectedElement(part);
            } else {
                stack.setSelectedElement(e2);
            }
        }
    }

    /**
     * @deprecated Use {@link BundleLoader} instead
     *
     * @param editor
     *            the container to load to
     * @param b
     *            the bundle
     * @throws VizException
     */
    @Deprecated
    public static void loadTo(final IDisplayPaneContainer container, Bundle b)
            throws VizException {
        new BundleLoader(container, b).run();
    }

}
