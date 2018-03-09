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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.raytheon.uf.viz.core.DescriptorMap;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.procedures.Bundle;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.viz.ui.BundleLoader;
import com.raytheon.viz.ui.BundleLoader.BundleInfoType;
import com.raytheon.viz.ui.BundleProductLoader;
import com.raytheon.viz.ui.UiUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * Handler for eclipse command that loads a bundle file to the display. This
 * handler can be used from plugin.xml by using command parameters to specify
 * what to load. It can also be used directly by configuring it using a
 * constructor.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Aug 30, 2013  2310     bsteffen    Initial creation
 * Mar 09, 2018  6731     bsteffen    Close empty editors when data is not loaded.
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class LoadBundleHandler extends AbstractHandler {

    private final String bundleFile;

    private final Map<String, String> variableSubstitutions;

    private final String editorType;

    private final Boolean fullBundleLoad;

    public LoadBundleHandler() {
        this(null);
    }

    public LoadBundleHandler(String bundleFile) {
        this(bundleFile, null, null, null);
    }

    public LoadBundleHandler(String bundleFile,
            Map<String, String> variableSubstitutions, String editorType,
            Boolean fullBundleLoad) {
        this.bundleFile = bundleFile;
        this.variableSubstitutions = variableSubstitutions;
        this.editorType = editorType;
        this.fullBundleLoad = fullBundleLoad;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            Bundle bundle = BundleLoader.getBundle(getBundleFile(event),
                    getVariableSubstitutions(event),
                    BundleInfoType.FILE_LOCATION);
            boolean hasData = hasDataResource(bundle);

            AbstractEditor editor = UiUtil.createOrOpenEditor(
                    getEditorType(event, bundle), bundle.getDisplays());
            if (hasData) {
                /*
                 * If the editor is newly created from the bundle then the
                 * editor will have the exact same displays as the bundle.
                 */
                boolean newEditor = true;
                IRenderableDisplay[] editorDisplays = UiUtil
                        .getDisplaysFromContainer(editor);
                IRenderableDisplay[] bundleDisplays = bundle.getDisplays();
                if (editorDisplays.length == bundleDisplays.length) {
                    for (int i = 0; i < editorDisplays.length; i += 1) {
                        if (editorDisplays[i] != bundleDisplays[i]) {
                            newEditor = false;
                        }
                    }
                } else {
                    newEditor = false;
                }
                /*
                 * If the bundle is supposed to load data, and the editor is a
                 * new editor, and the data resources weren't loaded, then close
                 * the editor since data didn't load.
                 */
                if (newEditor && !hasDataResource(bundle)) {
                    editor.getSite().getPage().closeEditor(editor, false);
                }
            }

            BundleLoader loader;
            if (isFullBundleLoad(event)) {
                loader = new BundleLoader(editor, bundle);
            } else {
                loader = new BundleProductLoader(editor, bundle);
            }
            loader.schedule();
            return event;
        } catch (VizException e) {
            throw new ExecutionException("Unable to load bundle", e);
        }
    }

    protected String getBundleFile(ExecutionEvent event) {
        if (this.bundleFile != null) {
            return bundleFile;
        } else if (event != null) {
            return event.getParameter("bundleFile");
        } else {
            throw new IllegalStateException(
                    "LoadBundleHandler requires a bundle file.");
        }
    }

    protected Map<String, String> getVariableSubstitutions(
            ExecutionEvent event) {
        if (this.variableSubstitutions != null) {
            return variableSubstitutions;
        } else if (event != null) {
            Map<String, String> variableSubstitutions = new HashMap<>();
            Map<?, ?> parameters = event.getParameters();
            for (Entry<?, ?> parameter : parameters.entrySet()) {
                if (parameter.getKey() instanceof String
                        && parameter.getValue() instanceof String) {
                    variableSubstitutions.put((String) parameter.getKey(),
                            (String) parameter.getValue());
                }
            }
            return variableSubstitutions;
        } else {
            return null;
        }
    }

    protected String getEditorType(ExecutionEvent event, Bundle bundle) {
        if (this.editorType != null) {
            return editorType;
        } else if (event != null) {
            String editorType = event.getParameter("editorType");
            if (editorType != null) {
                return editorType;
            }
        }
        String editorType = bundle.getEditor();
        if (editorType == null) {
            for (IRenderableDisplay display : bundle.getDisplays()) {
                String descEditorType = DescriptorMap.getEditorId(
                        display.getDescriptor().getClass().getName());
                if (descEditorType != null) {
                    if (editorType == null) {
                        editorType = descEditorType;
                    } else if (!editorType.equals(descEditorType)) {
                        // If this happens there are no reasonable guesses, just
                        // let UIUtil figure it out.
                        return null;
                    }
                }
            }
        }
        return editorType;
    }

    protected boolean isFullBundleLoad(ExecutionEvent event) {
        if (this.fullBundleLoad != null) {
            return fullBundleLoad;
        } else if (event != null) {
            return Boolean.valueOf(event.getParameter("fullBundleLoad"));
        } else {
            return false;
        }
    }

    /**
     * Determine if the bundle contains any data resources. For this function a
     * data resources is considered any resource that is not flagged as a system
     * resource or a map resource.
     * 
     * @param bundle
     *            the bundle to check
     * @return true if there are any data resources.
     */
    private boolean hasDataResource(Bundle bundle) {
        for (IRenderableDisplay display : bundle.getDisplays()) {
            for (ResourcePair resourcePair : display.getDescriptor()
                    .getResourceList()) {
                ResourceProperties props = resourcePair.getProperties();
                if (props != null) {
                    if (!props.isMapLayer() && !props.isSystemResource()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
