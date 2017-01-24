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

import jep.JepConfig;
import jep.JepException;

/**
 * A PythonScript for evaluating python statements.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 05, 2011           njensen   Initial creation
 * Sep 19, 2012  1091     randerso  Changed to extend PythonScript to allow
 *                                  access to both eval and execute methods
 * Jan 05, 2017  5959     njensen   Use JepConfig in constructor
 * Jan 24, 2017  6092     randerso  Add runScript method
 *
 * </pre>
 *
 * @author njensen
 */

public class PythonEval extends PythonScript {

    /**
     * Constructor
     *
     * @param config
     *            the jep config to use with the interpreter
     * @throws JepException
     */
    public PythonEval(JepConfig config) throws JepException {
        super(config);
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
    public PythonEval(JepConfig config, List<String> preEvals)
            throws JepException {
        super(config, preEvals);
    }

    /**
     * Constructor
     *
     * @deprecated use PythonEval(JepConfig) instead
     *
     * @param includePath
     * @param classLoader
     * @throws JepException
     */
    @Deprecated
    public PythonEval(String includePath, ClassLoader classLoader)
            throws JepException {
        this(new JepConfig().setIncludePath(includePath)
                .setClassLoader(classLoader));
    }

    /**
     * Constructor
     *
     * @deprecated Use PythonEval(JepConfig, List<String) instead
     *
     * @param includePath
     * @param classLoader
     * @param preEvals
     * @throws JepException
     */
    @Deprecated
    public PythonEval(String includePath, ClassLoader classLoader,
            List<String> preEvals) throws JepException {
        this(new JepConfig().setIncludePath(includePath)
                .setClassLoader(classLoader), preEvals);
    }

    public void eval(String eval) throws JepException {
        this.jep.eval(eval);
    }

    public Object getValue(String name) throws JepException {
        return this.jep.getValue(name);
    }

    /**
     * Runs a Python script.
     *
     * @param script
     *            a <code>String</code> absolute path to script file.
     * @exception JepException
     *                if an error occurs
     */
    public void runScript(String script) throws JepException {
        jep.runScript(script);
    }

}
