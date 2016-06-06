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
package com.raytheon.uf.viz.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * The ContextManager adds a layer in front of org.eclipse.ui.contexts
 * declarations in plugin.xml files to simplify activating and deactivating
 * contexts based on the active perspective, editor, or view. It uses the
 * extension point of com.raytheon.uf.viz.core.classContext to determine which
 * Eclipse contexts to activate/deactivate based on the active part
 * (IWorkbenchPart).
 * 
 * For example, a classContext declaration in a plugin.xml file will refer to a
 * single class such as a View. There can then be 1 to N contextIDs (Eclipse
 * contexts) associated with the classContext. When that View class is the
 * active part, those Eclipse contexts will be activated. When that View class
 * is not the active part, those Eclipse contexts will be deactivated (unless
 * another active part or code also activates that context).
 * 
 * This becomes harder to manage when you have an Eclipse context that should be
 * active even if the part (view or editor) is not active. A common example is
 * when a view is intrinsically tied to an editor. If the view is the active
 * part, the editor's contexts could be deactivated, and vice versa. In this
 * example, keybindings or mouse actions that should be active can become
 * inactive, causing undesired behavior. To solve this problem, you can get the
 * ContextManager instance or IContextService and explicitly activate or
 * deactivate contexts. Search for code/references that use
 * ContextManager.getInstance() or IContextService for examples of these
 * techniques where the contexts must be active/inactive in conjunction with
 * more than one part.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 21, 2009            mschenke    Initial creation
 * Jun 03, 2016            njensen     Added javadoc  
 * 
 * 
 * </pre>
 * 
 * @author mschenke
 */

public class ContextManager {

    private static class Context {
        int refCount = 0;

        IContextActivation activation = null;
    }

    private static Map<String, String[]> contexts = null;

    private static Map<Class<?>, String[]> contextCache = new HashMap<>();

    private static final String EXTENSION_POINT = "com.raytheon.uf.viz.core.classContext";

    private static Map<IServiceLocator, ContextManager> instanceMap = new HashMap<>();

    private static synchronized String[] getContextsForClass(Class<?> clazz) {
        if (contexts == null) {
            loadContexts();
        }
        String[] cons = contexts.get(clazz.getName());
        if (cons == null) {
            cons = new String[] {};
        }
        return cons;
    }

    private static void loadContexts() {
        contexts = new HashMap<>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        if (registry == null) {
            return;
        }
        IExtensionPoint point = registry.getExtensionPoint(EXTENSION_POINT);

        IExtension[] extensions = point.getExtensions();

        for (IExtension ext : extensions) {
            IConfigurationElement[] config = ext.getConfigurationElements();

            for (IConfigurationElement cfg : config) {
                String name = cfg.getAttribute("class");
                Set<String> ids = new HashSet<>();
                String[] current = contexts.get(name);
                if (current != null) {
                    for (String curr : current) {
                        ids.add(curr);
                    }
                }
                IConfigurationElement[] children = cfg.getChildren("contextId");
                for (IConfigurationElement child : children) {
                    ids.add(child.getAttribute("id"));
                }
                contexts.put(name, ids.toArray(new String[ids.size()]));
            }
        }
    }

    private static String[] getAllContextsForClass(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            // Base case, no contexts
            return new String[0];
        }

        String[] ids = contextCache.get(clazz);
        if (ids == null) {
            Set<String> contexts = new HashSet<>();

            ids = getContextsForClass(clazz);
            for (String id : ids) {
                contexts.add(id);
            }

            for (Class<?> interfaze : clazz.getInterfaces()) {
                // Get contexts for each interface
                ids = getAllContextsForClass(interfaze);
                for (String id : ids) {
                    contexts.add(id);
                }
            }

            ids = getAllContextsForClass(clazz.getSuperclass());
            for (String id : ids) {
                contexts.add(id);
            }

            ids = contexts.toArray(new String[contexts.size()]);
            contextCache.put(clazz, ids);
        }
        return ids;
    }

    public static synchronized ContextManager getInstance(
            IServiceLocator locator) {
        ContextManager manager = instanceMap.get(locator);
        if (manager == null) {
            manager = new ContextManager(locator);
            instanceMap.put(locator, manager);
        }
        return manager;
    }

    private Set<Object> activeObjects;

    private Map<String, ContextManager.Context> activeMap;

    private IContextService service;

    private ContextManager(IServiceLocator locator) {
        service = (IContextService) locator.getService(IContextService.class);
        activeObjects = new HashSet<>();
        activeMap = new HashMap<>();
    }

    public void activateContexts(Object obj) {
        synchronized (service) {
            if (obj == null) {
                return;
            }
            if (activeObjects.contains(obj)) {
                // we already activated the contexts
                return;
            }

            String[] ids = ContextManager
                    .getAllContextsForClass(obj.getClass());
            for (String context : ids) {
                ContextManager.Context ctx = activeMap.get(context);
                if (ctx == null) {
                    ctx = new ContextManager.Context();
                    ctx.activation = service.activateContext(context);
                    activeMap.put(context, ctx);
                }
                ctx.refCount++;
            }

            activeObjects.add(obj);
        }
    }

    public void deactivateContexts(Object obj) {
        synchronized (service) {
            if (obj == null) {
                return;
            }

            if (activeObjects.contains(obj) == false) {
                // don't deactive what was not activated
                return;
            }

            String[] ids = ContextManager
                    .getAllContextsForClass(obj.getClass());
            for (String context : ids) {
                ContextManager.Context ctx = activeMap.get(context);
                if (ctx != null) {
                    ctx.refCount--;
                    if (ctx.refCount <= 0) {
                        service.deactivateContext(ctx.activation);
                        activeMap.remove(context);
                    }
                }
            }
            activeObjects.remove(obj);
        }
    }
}
