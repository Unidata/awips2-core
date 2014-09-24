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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.reflections.scanners.SubTypesScanner;

import com.raytheon.uf.common.serialization.reflect.ISubClassLocator;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.Activator;

/**
 * Loads all subclasses of any class using all installed OSGi bundles and the
 * Reflections package. Results are cached using a {@link BundleClassCache}. to
 * save time on startup.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- -----------------------------------------
 * Oct 18, 2013  2491     bsteffen    Initial creation
 * Dec 10, 2013  2602     bsteffen    Add null checks to detect unloaded
 *                                    bundles.
 * Feb 03, 2013  2764     bsteffen    Use OSGi API to get dependencies.
 * Apr 17, 2014  3018     njensen     Synchronize against BundleRepository
 * Aug 13, 2014  3500     bclement    uses BundleSynchronizer
 * Aug 22, 2014  3500     bclement    removed sync on OSGi internals
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class SubClassLocator implements ISubClassLocator {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(SubClassLocator.class);

    private static final String CACHE_FILENAME = "subclassCache.txt";

    private final Map<String, BundleReflections> reflectionLookup = new HashMap<String, BundleReflections>();

    private final Map<String, Bundle> bundleLookup = new HashMap<String, Bundle>();

    private final Map<String, Collection<Bundle>> requiredBundles = new HashMap<String, Collection<Bundle>>();

    private final BundleClassCache cache;

    /**
     * Create a new SubClassLocator.
     */
    public SubClassLocator() {
        Bundle[] bundles = Activator.getDefault().getBundle()
                .getBundleContext().getBundles();
        for (Bundle b : bundles) {
            bundleLookup.put(b.getSymbolicName(), b);
        }
        File stateDir = Activator.getDefault().getStateLocation().toFile();
        cache = new BundleClassCache(new File(stateDir, CACHE_FILENAME));
    }

    /**
     * Locate all subclasses in all bundles of a given class
     * 
     * @param base
     * @return
     */
    @Override
    public Collection<Class<?>> locateSubClasses(Class<?> base) {
        Map<String, Set<Class<?>>> recursiveClasses = new HashMap<String, Set<Class<?>>>(
                bundleLookup.size(), 1.0f);
        Set<Class<?>> result = new HashSet<Class<?>>(512);
        for (Bundle bundle : bundleLookup.values()) {
            result.addAll(lookupRecursiveSubClasses(base, bundle, true,
                    recursiveClasses));
        }
        return result;
    }

    /**
     * Store the cache to disk.
     */
    @Override
    public void save() {
        cache.save();
    }

    /**
     * The lookup must occur recursively because otherwise sub classes of sub
     * classes of base types are not correctly located.
     * 
     * @param base
     *            base class
     * @param bundle
     *            bundle to search
     * @param includeRequiredSubclasses
     *            when false only subclasses of base found in this bundle are
     *            returned, when true subclasses found in other bundles required
     *            by this bundle are also returned.
     * @param recursiveClasses
     *            map of already searched bundles to avoid recursing the same
     *            bundles multiple time.
     * @return the sub classes contained in the bundle.
     */
    private Set<Class<?>> lookupRecursiveSubClasses(Class<?> base,
            Bundle bundle, boolean includeRequiredSubclasses,
            Map<String, Set<Class<?>>> recursiveClasses) {
        String bundleName = bundle.getSymbolicName();
        if (bundleName.startsWith("org.eclipse")) {
            /*
             * org.eclipse.osgi has no class loader and must be skipped,
             * skipping the rest of org.eclipse just saves time.
             */
            return Collections.emptySet();
        }

        if (bundle.getState() == Bundle.UNINSTALLED) {
            /*
             * We won't be able to get a class loader for uninstalled bundles so
             * don't process them.
             */
            return Collections.emptySet();
        }

        if (includeRequiredSubclasses) {
            /* Short circut if we already did this. */
            Set<Class<?>> result = recursiveClasses.get(bundleName);
            if (result != null) {
                return result;
            }
        }

        String[] ownedNames = cache.getTypes(bundle, base.getName());
        if (ownedNames == null) {
            Set<Class<?>> dependencies = getRequiredSubclasses(base, bundle,
                    recursiveClasses);
            /* Must pass dependencies in so type heirarchy is complete. */
            Set<Class<?>> owned = loadSubClassesReflectively(bundle,
                    dependencies);
            /* populate the cache */
            ownedNames = new String[owned.size()];
            int index = 0;
            for (Class<?> clazz : owned) {
                ownedNames[index++] = clazz.getName();
            }
            cache.putTypes(bundle, base.getName(), ownedNames);
            Set<Class<?>> all = new HashSet<Class<?>>(dependencies);
            all.addAll(owned);
            recursiveClasses.put(bundleName, all);
            if (includeRequiredSubclasses) {
                return all;
            } else {
                return owned;
            }
        } else {
            Set<Class<?>> owned = loadClassesFromCache(bundle,
                    Arrays.asList(ownedNames));
            if (includeRequiredSubclasses) {
                Set<Class<?>> dependencies = getRequiredSubclasses(base,
                        bundle, recursiveClasses);
                Set<Class<?>> all = new HashSet<Class<?>>(dependencies);
                all.addAll(owned);
                recursiveClasses.put(bundleName, all);
                return all;
            } else {
                return owned;
            }
        }
    }

    /**
     * Locate all subclasses of base that are found in the bundles required by
     * this bundle.
     * 
     * @param base
     *            base class
     * @param bundle
     *            bundle to search
     * @param recursiveClasses
     *            map of already searched bundles to avoid recursing the same
     *            bundles multiple time.
     * @return the sub classes contained in required bundles.
     */
    private Set<Class<?>> getRequiredSubclasses(Class<?> base, Bundle bundle,
            Map<String, Set<Class<?>>> recursiveClasses) {
        Set<Class<?>> dependencies = new HashSet<Class<?>>();
        dependencies.add(base);
        for (Bundle reqBundle : getRequiredBundles(bundle)) {
            dependencies.addAll(lookupRecursiveSubClasses(base, reqBundle,
                    true, recursiveClasses));
        }
        return dependencies;
    }

    /**
     * Load all subclasses of a set of classes that are found within a bundle.
     * 
     * @param bundle
     *            bundle to search
     * @param baseClasses
     *            all base classes
     * @return
     */
    private Set<Class<?>> loadSubClassesReflectively(Bundle bundle,
            Collection<Class<?>> baseClasses) {
        String bundleName = bundle.getSymbolicName();

        try {
            BundleReflections reflections = reflectionLookup.get(bundleName);
            if (reflections == null) {
                reflections = new BundleReflections(bundle,
                        new SubTypesScanner());
                reflectionLookup.put(bundleName, reflections);
            }
            return reflections.getSubTypesOf(baseClasses
                    .toArray(new Class<?>[0]));
        } catch (Throwable e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error loading classes in bundle(" + bundleName
                            + "), some procedures may not load.", e);
        }
        return Collections.emptySet();
    }

    /**
     * Load classes by name using a specific bundles class loader
     * 
     * @param bundle
     *            the bundle to get a class loader from
     * @param classNames
     *            names of classes to load.
     * @return
     */
    private Set<Class<?>> loadClassesFromCache(Bundle bundle,
            Collection<String> classNames) {
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring == null) {
            return Collections.emptySet();
        }

        ClassLoader loader = bundleWiring.getClassLoader();
        if (loader == null) {
            return Collections.emptySet();
        }

        HashSet<Class<?>> result = new HashSet<Class<?>>(classNames.size(),
                1.0f);
        for (String className : classNames) {
            try {
                result.add(Class.forName(className, false, loader));
            } catch (ClassNotFoundException e) {
                statusHandler.handle(Priority.PROBLEM, "Error loading class("
                        + className + "), some procedures may not load.", e);
            }
        }
        return result;
    }

    /**
     * Get back all the bundles this bundle depends on.
     * 
     * @param bundle
     *            the bundle
     * @return bundles required by bundle.
     */
    private Collection<Bundle> getRequiredBundles(Bundle bundle) {
        String bundleName = bundle.getSymbolicName();
        Collection<Bundle> required = requiredBundles.get(bundleName);
        if (required == null) {
            required = new HashSet<Bundle>();
            BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            if (bundleWiring != null) {
                /* Get Required bundles */
                for (BundleWire bw : bundleWiring
                        .getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE)) {
                    required.add(bw.getProviderWiring().getBundle());
                }
                /* Get Bundles through import package */
                for (BundleWire bw : bundleWiring
                        .getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)) {
                    required.add(bw.getProviderWiring().getBundle());
                }
            }
            /* Avoid recursion */
            required.remove(bundle);
            requiredBundles.put(bundleName, required);

        }
        return required;
    }

}
