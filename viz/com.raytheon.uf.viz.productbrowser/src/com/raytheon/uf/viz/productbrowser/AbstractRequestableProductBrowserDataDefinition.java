package com.raytheon.uf.viz.productbrowser;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;

import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.DescriptorMap;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.RecordFactory;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.NoPluginException;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.procedures.Bundle;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.raytheon.uf.viz.core.rsc.AbstractRequestableResourceData;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.ResourceType;
import com.raytheon.uf.viz.productbrowser.pref.ProductBrowserPreferenceConstants;
import com.raytheon.viz.ui.BundleProductLoader;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;

/**
 * @deprecated The design of this class makes excessive use of internal state
 *             that make it inherently thread-unsafe and confusing to implement.
 *             New development of data definitions should be done by
 *             implementing {@link ProductBrowserDataDefinition} directly
 *             instead of extending this class.
 * 
 *             <pre>
 * 
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * May 03, 2010           mnash     Initial creation
 * May 02, 2013  1949     bsteffen  Switch Product Browser from uengine to
 *                                  DbQueryRequest.
 * May 13, 2014  3135     bsteffen  Remove ISerializableObject.
 * Jun 02, 2015  4153     bsteffen  Extract interface and deprecate.
 * Aug 13, 2015  4717     mapeters  Update order when its preference store value changes
 * Sep 03, 2015  4717     mapeters  Get preference store order value using 
 *                                  ProductBrowserPreferenceConstants.getOrder()
 * Nov 03, 2015  5030     mapeters  Quietly handle CAVE & EDEX plugins being out of sync
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 * @param <T>
 */
@Deprecated
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractRequestableProductBrowserDataDefinition<T extends AbstractRequestableResourceData>
        extends AbstractProductBrowserDataDefinition<T> {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractRequestableProductBrowserDataDefinition.class);

    // for requestable products, pluginName must be part of the request
    public String PLUGIN_NAME = "pluginName";

    // name of the product for the request constraints
    public String productName;

    // order that you want the data to be in the tree, must correspond to
    // request constraints
    public String[] order = null;

    /** Use {@link ProductBrowserPreferenceConstants#ORDER} instead */
    @Deprecated
    protected static final String ORDER = ProductBrowserPreferenceConstants.ORDER;

    /**
     * First population when product browser is opened, decides what data types
     * are available
     * 
     * @return
     */
    @Override
    public String populateInitial() {
        if (!isEnabled()) {
            return null;
        }
        Object[] parameters = null;
        if (order.length >= 1) {
            try {
                DbQueryRequest request = new DbQueryRequest();
                request.setEntityClass(RecordFactory.getInstance()
                        .getPluginClass(productName));
                request.setLimit(1);
                DbQueryResponse response = (DbQueryResponse) ThriftClient
                        .sendRequest(request);
                parameters = response.getEntityObjects(Object.class);
            } catch (NoPluginException e) {
                String msg = "Unable to display "
                        + displayName
                        + " data in Product Browser because the server does not support the "
                        + productName + " plugin";
                statusHandler.debug(msg);
            } catch (VizException e) {
                statusHandler.handle(Priority.ERROR,
                        "Unable to populate initial product tree", e);
            }

            if (parameters != null && parameters.length != 0) {
                return displayName;
            } else {
                return null;
            }
        } else {
            return displayName;
        }
    }

    /**
     * Populates the tree each time the tree is expanded using the selections
     * 
     * @param selection
     * @return
     */
    @Override
    public List<ProductBrowserLabel> populateData(String[] selection) {
        long time = System.currentTimeMillis();
        List<ProductBrowserLabel> parameters = null;
        boolean product = false;
        String param = order[selection.length - 1];
        HashMap<String, RequestConstraint> queryList = getProductParameters(
                selection, order);
        product = selection.length == order.length;

        String[] temp = queryData(param, queryList);
        if (temp != null) {
            if ((Boolean) getPreference(FORMAT_DATA).getValue()) {
                parameters = formatData(param, temp);
            } else {
                parameters = super.formatData(param, temp);
            }
        }

        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                parameters.get(i).setProduct(product);
            }
        }

        System.out.println("Time to query for "
                + selection[selection.length - 1] + ": "
                + (System.currentTimeMillis() - time) + "ms");
        return parameters;
    }

    /**
     * 
     * @param param
     * @param queryList
     * @return
     */
    protected String[] queryData(String param,
            Map<String, RequestConstraint> queryList) {
        try {
            DbQueryRequest request = new DbQueryRequest();
            request.setEntityClass(RecordFactory.getInstance().getPluginClass(
                    productName));
            request.setConstraints(queryList);
            request.addRequestField(param);
            request.setDistinct(true);
            DbQueryResponse response = (DbQueryResponse) ThriftClient
                    .sendRequest(request);
            Object[] paramObjs = response.getFieldObjects(param, Object.class);
            if (paramObjs != null) {
                String[] params = new String[paramObjs.length];
                for (int i = 0; i < params.length; i += 1) {
                    if (paramObjs[i] != null) {
                        params[i] = paramObjs[i].toString();
                    }
                }
                return params;
            }
        } catch (VizException e) {
            statusHandler
                    .handle(Priority.PROBLEM, "Unable to perform query", e);
        }
        return null;
    }

    @Override
    public void constructResource(String[] selection, ResourceType type) {
        resourceData = getResourceData();
        (resourceData).setMetadataMap(getProductParameters(selection, order));
        if (type != null) {
            loadProperties.setResourceType(type);
        }
        constructResource();
    }

    public void constructResource(
            HashMap<String, RequestConstraint> requestConstraints) {
        resourceData = getResourceData();
        resourceData.setMetadataMap(requestConstraints);
        constructResource();
    }

    protected void constructResource() {
        ResourcePair pair = new ResourcePair();
        pair.setResourceData(resourceData);
        pair.setLoadProperties(loadProperties);
        pair.setProperties(new ResourceProperties());
        constructResource(Arrays.asList(pair));
    }

    protected void constructResource(List<ResourcePair> pairs) {
        // retrieves the correct editor
        getEditor();
        IDisplayPaneContainer container = getEditor();
        if (container == null) {
            return;
        }
        AbstractRenderableDisplay display = (AbstractRenderableDisplay) container
                .getActiveDisplayPane().getRenderableDisplay();
        display = (AbstractRenderableDisplay) display.createNewDisplay();
        for (ResourcePair pair : pairs) {
            display.getDescriptor().getResourceList().add(pair);
        }
        Bundle b = new Bundle();
        b.setDisplays(new AbstractRenderableDisplay[] { display });
        new BundleProductLoader(EditorUtil.getActiveVizContainer(), b)
                .schedule();
    }

    /**
     * @return
     */
    @Override
    public abstract T getResourceData();

    @Override
    public List<String> buildProductList(final List<String> historyList) {
        historyList.add(displayName);
        Map<String, RequestConstraint> queryList = new HashMap<String, RequestConstraint>();
        RequestConstraint contstraint = new RequestConstraint(productName);
        queryList.put(PLUGIN_NAME, contstraint);
        for (int i = 0; i < order.length; i++) {
            String[] items = queryData(order[i], queryList);
            if (items != null) {
                List<ProductBrowserLabel> labels = formatData(order[i], items);
                if (labels != null) {
                    for (int j = 0; j < labels.size(); j++) {
                        historyList.add(labels.get(j).getName());
                    }
                }
            }
        }
        return historyList;
    }

    /**
     * Getting the map of request constraints for populating the resource data
     * 
     * @param selection
     * @param order
     * @return
     */
    public HashMap<String, RequestConstraint> getProductParameters(
            String[] selection, String[] order) {
        HashMap<String, RequestConstraint> queryList = new HashMap<String, RequestConstraint>();
        queryList.put(PLUGIN_NAME, new RequestConstraint(productName));

        String[] usedSelection = realignSelection(selection);
        for (int i = 0; i < usedSelection.length; i++) {
            queryList.put(order[i], new RequestConstraint(usedSelection[i]));
        }
        return queryList;
    }

    /**
     * Reorder the selection so that it lines up with the order
     * 
     * @param selection
     * @return
     */
    protected final String[] realignSelection(String[] selection) {
        String[] usedSelection = new String[selection.length - 1];
        for (int i = 1; i < selection.length; i++) {
            usedSelection[i - 1] = selection[i];
        }
        return usedSelection;
    }

    public ResourceType getResourceType() {
        return ResourceType.PLAN_VIEW;
    }

    /**
     * Knowledge of this is for what kind of product it is, should be overridden
     * by child class to send back what are the possible ResourceTypes and the
     * names they should be given in the menu
     * 
     * @return
     */
    public List<ResourceType> getProductTypes() {
        return null;
    }

    protected IDisplayPaneContainer getEditor() {
        String id = DescriptorMap.getEditorId(getDescriptorClass().getName());
        IEditorPart editorPart = EditorUtil.getActiveEditor();
        if (editorPart != null && id.equals(editorPart.getEditorSite().getId())) {
            return (AbstractEditor) editorPart;
        }
        editorPart = EditorUtil.findEditor(id);
        if (editorPart != null) {
            return (AbstractEditor) editorPart;
        }
        return openNewEditor(id);
    }

    protected IDisplayPaneContainer openNewEditor(String editorId) {
        IWorkbenchWindow window = VizWorkbenchManager.getInstance()
                .getCurrentWindow();
        AbstractVizPerspectiveManager mgr = VizPerspectiveListener.getInstance(
                window).getActivePerspectiveManager();
        if (mgr != null) {
            AbstractEditor editor = mgr.openNewEditor();
            if (editor == null) {
                return null;
            } else if (editorId.equals(editor.getEditorSite().getId())) {
                return editor;
            } else {
                window.getActivePage().closeEditor(editor, false);
            }
        }
        return null;
    }

    protected Class<? extends IDescriptor> getDescriptorClass() {
        return MapDescriptor.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.productbrowser.IProductBrowserPreferences#
     * configurePreferences()
     */
    @Override
    protected List<ProductBrowserPreference> configurePreferences() {
        List<ProductBrowserPreference> widgets = super.configurePreferences();
        widgets.add(ProductBrowserPreferenceConstants
                .createOrderPreference(order));
        widgets.add(ProductBrowserPreferenceConstants.createFormatPreference());
        return widgets;
    }

    /**
     * @return the order
     */
    protected String[] getOrder() {
        String[] order = ProductBrowserPreferenceConstants
                .getOrder(displayName);
        if (order != null) {
            this.order = order;
        }
        return this.order;
    }

    @Override
    public List<ProductBrowserLabel> getLabels(String[] selection) {
        if (selection.length == 0) {
            ProductBrowserLabel label = new ProductBrowserLabel(displayName,
                    productName);
            label.setData(productName);
            label.setProduct(order.length == 0);
            return Collections.singletonList(label);

        }
        return super.getLabels(selection);
    }

    @Override
    public String getProductInfo(String[] selection) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(PLUGIN_NAME + " = " + productName);

        for (int i = 1; i < selection.length; i++) {
            stringBuilder.append("\n");
            stringBuilder.append(order[i - 1] + " = " + selection[i]);
        }
        return stringBuilder.toString();
    }

    @Override
    public List<ProductBrowserPreference> getPreferences() {
        if (preferences == null) {
            IPreferenceStore store = Activator.getDefault()
                    .getPreferenceStore();
            store.addPropertyChangeListener(new IPropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent event) {
                    if (event.getProperty().equals(
                            ProductBrowserPreferenceConstants.ORDER
                                    + displayName)) {
                        // Update order from preference store
                        getOrder();
                    }
                }
            });
        }
        return super.getPreferences();
    }
}
