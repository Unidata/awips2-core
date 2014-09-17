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
package com.raytheon.uf.viz.core.reflect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Utility for loading a class through a bundle by matching the beginning of a
 * bundle's symbolic name with the beginning of the class name. Makes the
 * assumption that package names follow the standard of beginning with the
 * plugin name.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 3, 2014  3356       njensen     Initial creation
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class BundleNameClassLocator {

    protected BundleContext context;

    protected Comparator<Bundle> comp;

    public BundleNameClassLocator(BundleContext context) {
        this.context = context;
        this.comp = Collections.reverseOrder(new BundleNameLengthComparator());
    }

    /**
     * Loads a class by searching through the bundle context for bundles that
     * may possibly contain the class.
     * 
     * @param name
     *            the fully qualified name of the Java class
     * @return the Class instance corresponding to the name
     * @throws ClassNotFoundException
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        List<Bundle> matches = new ArrayList<Bundle>(3);
        for (Bundle b : context
                .getBundles()) {
            String symName = b.getSymbolicName();
            if (name.startsWith(symName)) {
                matches.add(b);
            }
        }
        
        if (!matches.isEmpty()) {
            Collections.sort(matches, comp);
            for (Bundle b : matches) {
                try {
                    Class<?> clazz = b.loadClass(name);
                    return clazz;
                } catch (ClassNotFoundException e) {
                    /*
                     * intentionally ignore and keep trying with other matching
                     * bundles
                     */
                }
            }
        }
        
        throw new ClassNotFoundException("Unable to find class " + name
                + " in bundle context of "
                + context.getBundle().getSymbolicName());
    }

    /**
     * Compares the length of a bundle's symbolic name. Used above, the longer a
     * bundle name implies a better match and more likely to contain the class
     * we're looking for.
     */
    private static final class BundleNameLengthComparator implements
            Comparator<Bundle> {

        @Override
        public int compare(Bundle b1, Bundle b2) {
            return Integer.compare(b1.getSymbolicName().length(), b2
                    .getSymbolicName().length());
        }

    }

}
