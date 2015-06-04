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
package com.raytheon.uf.viz.productbrowser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.productbrowser.ProductBrowserPreference.PreferenceType;
import com.raytheon.uf.viz.productbrowser.pref.PreferenceBasedDataDefinition;

/**
 * Builds each data type's preference specifically for product browser
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * May 16, 2011           mnash     Initial creation
 * Jun 02, 2015  4153     bsteffen  Use an interface to get preferences from
 *                                  data definition.
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */

public class DataTypePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private static final transient IUFStatusHandler statusHandler = UFStatus.getHandler(DataTypePreferencePage.class);

    private static IExtension[] extensions;

    private List<PreferenceBasedDataDefinition> prods = null;

    private List<FieldEditor> editors = null;

    private static boolean saved = false;

    public DataTypePreferencePage() {
        super(GRID);
        prods = new ArrayList<PreferenceBasedDataDefinition>();
        editors = new ArrayList<FieldEditor>();
        setDescription("Specify Product Browser Settings...");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
        saved = false;
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Data Type Preferences");
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(ProductBrowserUtils.DATA_DEFINITION_ID);
        if (point != null) {
            extensions = point.getExtensions();
        } else {
            extensions = new IExtension[0];
        }

        for (IExtension ext : extensions) {
            IConfigurationElement[] config = ext.getConfigurationElements();
            for (IConfigurationElement element : config) {
                try {
                    ProductBrowserDataDefinition prod = (ProductBrowserDataDefinition) element
                            .createExecutableExtension("class");
                    if (prod instanceof PreferenceBasedDataDefinition) {
                        prods.add((PreferenceBasedDataDefinition) prod);
                    }
                } catch (CoreException e) {
                    statusHandler.error("Unable to create a data definition for the product browser.", e);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        boolean tf = super.performOk();
        for (IViewReference reference : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .getViewReferences()) {
            if (!saved) {
                if (ProductBrowserView.ID.equals(reference.getId())) {
                    ((ProductBrowserView) reference.getPart(true)).populateInitialProductTree();
                    saved = true;
                }
            }
        }
        return tf;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    @Override
    protected void performApply() {
        for (FieldEditor editor : editors) {
            editor.store();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors
     * ()
     */
    @Override
    protected void createFieldEditors() {
        getPreferences(getProduct(getTitle()), getTitle());
        for (FieldEditor editor : editors) {
            this.addField(editor);
        }
    }

    private PreferenceBasedDataDefinition getProduct(String name) {
        for (PreferenceBasedDataDefinition prod : prods) {
            for (ProductBrowserLabel label : prod.getLabels(new String[0])) {
                if (name.equals(label.getName())) {
                    return prod;
                }
            }
        }
        return null;
    }

    /**
     * Per each type of product, goes in and decides what preferences it needs
     * to display
     * 
     * @param prod
     */
    private void getPreferences(final PreferenceBasedDataDefinition prod, final String displayName) {
        List<ProductBrowserPreference> prefs = prod.getPreferences();
        for (ProductBrowserPreference pref : prefs) {
            String prefName = pref.getLabel() + displayName;
            if (pref.getPreferenceType() == PreferenceType.EDITABLE_STRING) {
                StringFieldEditor editor = new StringFieldEditor(prefName, pref.getLabel(),
                        getFieldEditorParent());
                editors.add(editor);
            } else if (pref.getPreferenceType() == PreferenceType.BOOLEAN) {
                BooleanFieldEditor editor = new BooleanFieldEditor(prefName, pref.getLabel(),
                        getFieldEditorParent());
                editors.add(editor);
            } else if (pref.getPreferenceType() == PreferenceType.STRING_ARRAY) {
                final String[] values = ((String[]) pref.getValue());
                ListEditor editor = new ListEditor(prefName, pref.getLabel(),
                        getFieldEditorParent()) {

                    @Override
                    protected String[] parseString(String stringList) {
                        return new String[0];
                    }

                    @Override
                    protected String getNewInputObject() {
                        return null;
                    }

                    @Override
                    protected String createList(String[] items) {
                        String temp = "";
                        for (int i = 0; i < items.length; i++) {
                            if (temp.isEmpty()) {
                                temp += items[i];
                            } else {
                                temp += "," + items[i];
                            }
                        }
                        return temp;
                    }

                    @Override
                    protected void doFillIntoGrid(Composite parent, int numColumns) {
                        super.doFillIntoGrid(parent, numColumns);
                        for (int i = 0; i < values.length; i++) {
                            getList().add(values[i]);
                        }
                    }
                };
                editor.getButtonBoxControl(getFieldEditorParent()).getChildren()[0].setVisible(false);
                editor.getButtonBoxControl(getFieldEditorParent()).getChildren()[1].setVisible(false);
                editor.getButtonBoxControl(getFieldEditorParent()).layout();
                editors.add(editor);
            }
        }
    }
}