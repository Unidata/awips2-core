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
package com.raytheon.uf.common.python;

import java.util.List;
import java.util.Set;

import jep.Jep;
import jep.JepConfig;
import jep.JepException;
import jep.NamingConventionClassEnquirer;

/**
 * Interfaces to a native Python interpreter with Jep.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 07, 2009            njensen     Initial creation
 * Sep 05, 2013  #2307     dgilling    Remove constructor without explicit
 *                                     ClassLoader.
 * Apr 27, 2015   4259     njensen     Update for new JEP API
 * Apr 28, 2016   5236     njensen     Use Jep redirectOutput for python prints
 * Jan 04, 2017   5959     njensen     All constructors now use JepConfig
 *                                     Add numpy as a shared module
 * Feb 17, 2017   5959     njensen     Add scipy modules as shared modules
 * Mar 16, 2017   5959     njensen     Add _strptime as shared module
 * Sep 25, 2017   6457     randerso    Add scipy.constants as shared module
 * Dec 19, 2017   7149     njensen     Get shared modules from config file
 * Jun 03, 2019   7852     dgilling    Update code for jep 3.8.
 *
 * </pre>
 *
 * @author njensen
 */

public abstract class PythonInterpreter implements AutoCloseable {

    private static final String CLEANUP = "def cleanup():\n"
            + "   g = globals()\n" + "   for i in g:\n"
            + "      if not i.startswith('__') and not i == 'cleanup':\n"
            + "         g[i] = None\n\n";

    protected Jep jep;

    /**
     * Constructor
     *
     * @param config
     *            the jep config to use with the interpreter
     * @throws JepException
     */
    public PythonInterpreter(JepConfig config) throws JepException {
        this(config, null, null);
    }

    /**
     * Constructor
     *
     * @param config
     *            the jep config to use with the interpreter
     * @param filePath
     *            the path to a python script to run immediately after
     *            interpreter initialization
     * @throws JepException
     */
    public PythonInterpreter(JepConfig config, String filePath)
            throws JepException {
        this(config, filePath, null);
    }

    /**
     * Constructor
     *
     * @param config
     *            the jep config to use with the interpreter
     * @param preEvals
     *            String statements to be run by the python interpreter
     *            immediately
     * @throws JepException
     */
    public PythonInterpreter(JepConfig config, List<String> preEvals)
            throws JepException {
        this(config, null, preEvals);
    }

    /**
     * Constructor
     *
     * @param config
     *            the jep config to use with the interpreter
     * @param filePath
     *            the path to a python script to run immediately after
     *            interpreter initialization
     * @param preEvals
     *            String statements to be run by the python interpreter before
     *            the file at filePath
     * @throws JepException
     */
    public PythonInterpreter(JepConfig config, String filePath,
            List<String> preEvals) throws JepException {
        config.setClassEnquirer(new NamingConventionClassEnquirer())
                .setRedirectOutputStreams(true);

        /*
         * require numpy and _strptime to prevent issues and memory leaks when
         * disposing interpreters. It's ok to have duplicates from the
         * JepConfig, the Set will take care of those.
         */
        Set<String> shared = PythonSharedModulesUtil.getSharedModules();
        for (String module : shared) {
            config.addSharedModules(module);
        }

        jep = new Jep(config);
        initializeJep(filePath, preEvals);
    }

    /**
     * Runs the preEvals and the script if provided
     *
     * @param filePath
     *            the path to the python script or null if no script is to be
     *            run
     * @param preEvals
     *            String statements to be run by the python interpreter before
     *            the file at filePath
     *
     * @throws JepException
     *
     */
    private void initializeJep(String filePath, List<String> preEvals)
            throws JepException {
        if (preEvals != null) {
            for (String statement : preEvals) {
                jep.eval(statement);
            }
        }

        if (filePath != null) {
            jep.runScript(filePath);
        }
    }

    /**
     * Evaluates an argument in the python interpreter. Should be overridden by
     * subclasses where the python scripts wants python objects, not just
     * references to Java objects.
     *
     * @param argName
     *            the name the argument will receive in the python interpreter
     * @param argValue
     *            the value of the argument
     * @throws JepException
     */
    protected void evaluateArgument(String argName, Object argValue)
            throws JepException {
        jep.set(argName, argValue);
    }

    /**
     * Disposes of the jep instance. Should be called whenever the system no
     * longer needs this PythonScript instance to free memory.
     *
     * @throws JepException
     */
    public void dispose() throws JepException {
        cleanupGlobals();
        jep.close();
    }

    public void cleanupGlobals() {
        try {
            jep.eval(CLEANUP);
            jep.eval("cleanup()");
            jep.eval("cleanup = None");
            jep.eval("import gc");
            jep.eval("uncollected = gc.collect(2)");
            jep.eval("uncollected = None");
            jep.eval("gc = None");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void close() throws JepException {
        this.dispose();
    }

}
