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
package com.raytheon.uf.viz.productbrowser.pref;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.raytheon.uf.viz.productbrowser.Activator;
import com.raytheon.uf.viz.productbrowser.ProductBrowserPreference;

/**
 * 
 * {@link IPropertyChangeListener} which automatically updates a
 * {@link ProductBrowserPreference} whenever the value in the
 * {@link IPreferenceStore} changes. This is a WeakReference so that when the
 * preference is no longer referenced the listener will be removed from the
 * store.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Jun 04, 2015  4153     bsteffen  Initial creation
 * Aug 12, 2015  4717     mapeters  Break after each case in propertyChange's switch statement
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class ProductBrowserPreferenceListener extends
        WeakReference<ProductBrowserPreference> implements
        IPropertyChangeListener {

    private static final String ARRAY_SEPARAT0R = ",";

    private static final ReferenceQueue<ProductBrowserPreference> referenceQueue = new ReferenceQueue<>();

    private final String propertyName;

    public ProductBrowserPreferenceListener(String displayName,
            ProductBrowserPreference preference) {
        super(preference, referenceQueue);
        this.propertyName = preference.getLabel() + displayName;

        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        switch (preference.getPreferenceType()) {
        case BOOLEAN:
            store.setDefault(propertyName, (Boolean) preference.getValue());
            preference.setValue(store.getBoolean(propertyName));
            break;
        case EDITABLE_STRING:
            store.setDefault(propertyName, (String) preference.getValue());
            preference.setValue(store.getString(propertyName));
            break;
        case STRING_ARRAY:
            String[] items = (String[]) preference.getValue();
            String temp = "";
            for (int i = 0; i < items.length; i++) {
                if (temp.isEmpty()) {
                    temp += items[i];
                } else {
                    temp += ARRAY_SEPARAT0R + items[i];
                }
            }
            store.setDefault(propertyName, temp);
            preference.setValue(store.getString(propertyName).split(
                    ARRAY_SEPARAT0R));
            break;
        }

        store.addPropertyChangeListener(this);
        /* Clean up references to avoid leaking listeners. */
        Reference<? extends ProductBrowserPreference> ref = referenceQueue
                .poll();
        while (ref != null) {
            if (ref instanceof ProductBrowserPreferenceListener) {
                store.removePropertyChangeListener((ProductBrowserPreferenceListener) ref);
            }
            ref = referenceQueue.poll();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        ProductBrowserPreference pref = this.get();
        if (pref == null) {
            store.removePropertyChangeListener(this);
        }
        if (propertyName.equals(event.getProperty())) {
            switch (pref.getPreferenceType()) {
            case BOOLEAN:
                pref.setValue(store.getBoolean(propertyName));
                break;
            case EDITABLE_STRING:
                pref.setValue(store.getString(propertyName));
                break;
            case STRING_ARRAY:
                pref.setValue(store.getString(propertyName).split(
                        ARRAY_SEPARAT0R));
                break;
            }
        }
    }

}