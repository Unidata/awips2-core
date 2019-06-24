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
package com.raytheon.uf.common.derivparam.library;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.measure.Unit;
import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.derivparam.DerivParamFunctionType;
import com.raytheon.uf.common.derivparam.IDerivParamFunctionAdapter;
import com.raytheon.uf.common.derivparam.library.DerivParamMethod.MethodType;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.ILocalizationPathObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

import tec.uom.se.AbstractUnit;

/**
 * Primary public interface for derived parameters. Introspection on the derived
 * parameters available can be done using {@link #getDerParLibrary()}. For
 * actually performing derived parameters calculations the
 * {@link #calculate(DerivedParameterRequest)} method can be used.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- ------------------------------------------
 * Jul 03, 2008  1076     brockwoo    Initial creation
 * Nov 16, 2009  3120     rjpeter     Removed use of LevelNameMappingFile.
 * Nov 20, 2009  3387     jelkins     Use derived script's variableId instead of
 *                                    filename
 * Nov 21, 2009  3576     rjpeter     Refactored DerivParamDesc.
 * Jun 04, 2013  2041     bsteffen    Switch derived parameters to use
 *                                    concurrent python for threading.
 * Nov 19, 2013  2361     njensen     Only shutdown if initialized
 * Jan 14, 2014  2661     bsteffen    Shutdown using uf.viz.core.Activator
 * Jan 30, 2014  2725     ekladstrup  Refactor to remove dependencies on eclipse
 *                                    runtime and support some configuration
 *                                    through spring
 * Mar 27, 2014  2945     bsteffen    Recursively find definitions in
 *                                    subdirectories.
 * Jul 21, 2014  3373     bclement    JAXB manager API changes
 * Jul 22, 2015  4672     bsteffen    Create notification task to avoid
 *                                    notifying newly added listeners at
 *                                    startup.
 * Mar 24, 2016  5439     bsteffen    Do not throw exceptions after logging
 *                                    error that adapter is not registered
 * Oct 05, 2016  5891     bsteffen    Allow functions in subdirectories
 * Apr 15, 2019  7596     lsingh      Updated units framework to JSR-363.
 * 
 * </pre>
 * 
 * @author brockwoo
 */
public class DerivedParameterGenerator implements ILocalizationPathObserver {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(DerivedParameterGenerator.class);

    public static final String FUNCTIONS = "functions";

    public static final String DEFINITIONS = "definitions";

    public static final String DERIV_PARAM_DIR = "derivedParameters";

    public static final String FUNCTIONS_DIR = DERIV_PARAM_DIR + File.separator
            + FUNCTIONS;

    public static final String XML_DIR = DERIV_PARAM_DIR + File.separator
            + DEFINITIONS;

    public static interface DerivParamUpdateListener {
        public void updateDerParLibrary(
                Map<String, DerivParamDesc> derParLibrary);
    }

    private static DerivedParameterGenerator instance;

    // TODO: Handle multiple function types (python mixed with
    // gsl/cuda/anything)
    private IDerivParamFunctionAdapter adapter;

    private Set<DerivParamUpdateListener> listeners = new HashSet<>();

    private Map<String, DerivParamDesc> derParLibrary;

    private boolean needsLibInit = true;

    private String extension = null;

    protected static List<DerivParamFunctionType> functionTypes = new ArrayList<>(
            1);

    protected ExecutorService execService = null;

    public static synchronized DerivedParameterGenerator getInstance() {
        if (instance == null) {
            instance = new DerivedParameterGenerator();
        }
        return instance;
    }

    /**
     * Create a function type from the adapter and add it to the function type
     * list
     * 
     * @param adapter
     * @return the adapter
     */
    public static IDerivParamFunctionAdapter addFunctionAdapter(
            IDerivParamFunctionAdapter adapter) {
        DerivParamFunctionType functionType = new DerivParamFunctionType();
        functionType.setName(adapter.getName());
        functionType.setExtension(adapter.getExtension());
        functionType.setAdapter(adapter);
        functionTypes.add(functionType);
        return adapter;
    }

    public static DerivParamFunctionType[] getFunctionTypes() {
        return functionTypes.toArray(new DerivParamFunctionType[0]);
    }

    public static synchronized Map<String, DerivParamDesc> getDerParLibrary() {
        return getInstance().getLibrary();
    }

    public static void registerUpdateListener(DerivParamUpdateListener listener) {
        DerivedParameterGenerator instance = getInstance();
        synchronized (instance.listeners) {
            instance.listeners.add(listener);
        }
    }

    private DerivedParameterGenerator() {
        /*
         * We shouldn't ever be running more than one job at a time anyway, but
         * use an executor service just in case we want to tweak things later.
         */
        execService = Executors.newSingleThreadExecutor();

        DerivParamFunctionType[] functionTypes = getFunctionTypes();

        if (functionTypes == null || functionTypes.length == 0) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error creating derived parameter function type,"
                            + " derived paramters will not be available");
            this.derParLibrary = new HashMap<>();
            this.needsLibInit = false;
            return;
        }

        this.adapter = functionTypes[0].getAdapter();
        this.extension = functionTypes[0].getExtension();
        PathManagerFactory.getPathManager().addLocalizationPathObserver(
                DERIV_PARAM_DIR, this);

        initLibrary();
    }

    /**
     * Adds a task to the list of derived parameter requests.
     * 
     * @param task
     *            A derived parameter request
     * @return boolean indicating if the request was put into queue
     */
    public static List<IDataRecord> calculate(DerivedParameterRequest task)
            throws ExecutionException {
        return getInstance().adapter.executeFunction(task.getMethod(),
                Arrays.asList(task.getArgumentRecords()));
    }

    private synchronized void initLibrary() {
        if (needsLibInit) {
            long start = System.currentTimeMillis();
            Set<String> derivParamFiles = new HashSet<>();
            Map<String, DerivParamDesc> derParLibrary = new HashMap<>();
            IPathManager pm = PathManagerFactory.getPathManager();

            /* get all localization levels derived params and combine them */
            LocalizationContext[] contexts = pm
                    .getLocalSearchHierarchy(LocalizationType.COMMON_STATIC);
            ILocalizationFile[] xmlFiles = pm.listFiles(contexts, XML_DIR,
                    new String[] { ".xml" }, true, true);
            SingleTypeJAXBManager<DerivParamDesc> jaxbMan;
            try {
                jaxbMan = new SingleTypeJAXBManager<>(true,
                        DerivParamDesc.class);
            } catch (JAXBException e1) {
                statusHandler
                        .handle(Priority.CRITICAL,
                                "DerivedParameters failed to load, no derived parameters will be available",
                                e1);
                return;
            }

            for (ILocalizationFile file : xmlFiles) {
                try (InputStream is = file.openInputStream()) {
                    DerivParamDesc desc = jaxbMan.unmarshalFromInputStream(is);
                    if (derParLibrary.containsKey(desc.getAbbreviation())) {
                        DerivParamDesc oldDesc = derParLibrary.get(desc
                                .getAbbreviation());
                        oldDesc.merge(desc);
                    } else {
                        derParLibrary.put(desc.getAbbreviation(), desc);
                    }
                } catch (Exception e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "An error was encountered while creating the DerivedParameter from "
                                    + file.toString(), e);
                    continue;
                }
            }

            ILocalizationFile[] functions = pm.listStaticFiles(FUNCTIONS_DIR,
                    new String[] { "." + extension }, true, true);
            for (ILocalizationFile file : functions) {
                String path = file.getPath();
                path = path.substring(FUNCTIONS_DIR.length() + 1);
                derivParamFiles.add("func:" + path);
            }

            // Set the correct units on every field */
            for (DerivParamDesc desc : derParLibrary.values()) {
                if (desc.getMethods() == null) {
                    continue;
                }
                for (DerivParamMethod method : desc.getMethods()) {
                    for (IDerivParamField ifield : method.getFields()) {
                        if (ifield instanceof DerivParamField) {
                            DerivParamField field = (DerivParamField) ifield;
                            DerivParamDesc fDesc = derParLibrary.get(field
                                    .getParam());
                            if (fDesc != null && field.getUnit() == AbstractUnit.ONE) {
                                field.setUnit(fDesc.getUnit());
                            }
                        }
                    }
                    if (method.getFrameworkMethod() == null) {
                        if (derivParamFiles.contains("func:"
                                + method.getName().split("[.]")[0] + "."
                                + extension)) {
                            method.setMethodType(MethodType.PYTHON);
                        } else {
                            method.setMethodType(MethodType.OTHER);
                        }
                    }
                }
            }
            this.derParLibrary = derParLibrary;
            adapter.init();
            
            Runnable notifyTask;
            synchronized (listeners) {
                notifyTask = new NotifyTask(new ArrayList<>(listeners),
                        derParLibrary);
            }
            execService.execute(notifyTask);

            System.out.println("Time to init derived parameters: "
                    + (System.currentTimeMillis() - start) + "ms");
            needsLibInit = false;
        }
    }

    public Map<String, DerivParamDesc> getLibrary() {
        initLibrary();
        return derParLibrary;
    }

    @Override
    public void fileChanged(ILocalizationFile file) {
        needsLibInit = true;
        initLibrary();
    }

    public static synchronized void shutdown() {
        if (instance != null) {
            getInstance().adapter.shutdown();
            instance = null;
        }
    }

    private static class NotifyTask implements Runnable {

        final Collection<DerivParamUpdateListener> listeners;

        final Map<String, DerivParamDesc> derParLibrary;

        public NotifyTask(Collection<DerivParamUpdateListener> listeners,
                Map<String, DerivParamDesc> derParLibrary) {
            this.listeners = listeners;
            this.derParLibrary = derParLibrary;
        }

        @Override
        public void run() {
            Collection<DerivParamUpdateListener> l = null;
            synchronized (listeners) {
                l = new ArrayList<>(listeners);
            }
            for (DerivParamUpdateListener listener : l) {
                listener.updateDerParLibrary(derParLibrary);
            }
        }
    }

}
