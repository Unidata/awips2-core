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
package com.raytheon.uf.common.python.concurrent;

import java.util.concurrent.Callable;

import com.raytheon.uf.common.python.PythonInterpreter;

/**
 * Special implementation of {@link Callable} to pass the
 * {@link IPythonExecutor} and, if provided, {@link IPythonJobListener} to the
 * thread of execution in the {@link PythonInterpreterThreadPoolExecutor}. This
 * is necessary because the executor will not have access to the thread-specific
 * {@link PythonInterpreter} until just before execution.
 * 
 * <p>
 * This class' {@link call} method is not actually callable. Instead this class
 * acts as a container until the ExecutorService can build a proper
 * {@link Callable} when it finally has access to the interpreter instance.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 11, 2015  #4816     dgilling     Initial creation
 * 
 * </pre>
 * 
 * @author dgilling
 * @version 1.0
 */

final class PythonScriptPseudoCallable<P extends PythonInterpreter, V>
        implements Callable<V> {

    private final IPythonExecutor<P, V> python;

    private final IPythonJobListener<V> listener;

    /**
     * Construct the pseudo-callable object.
     * 
     * @param python
     *            {@code IPythonExecutor} instance.
     * @param listener
     *            Optional {@code IPythonJobListener} instance to fire once
     *            executor job has been run.
     */
    PythonScriptPseudoCallable(IPythonExecutor<P, V> python,
            IPythonJobListener<V> listener) {
        this.python = python;
        this.listener = listener;
    }

    @Override
    public V call() throws Exception {
        throw new AssertionError(String.format(
                "Do not call the call method on the %s class.", getClass()
                        .getName()));
    }

    public IPythonExecutor<P, V> getPython() {
        return python;
    }

    public IPythonJobListener<V> getListener() {
        return listener;
    }
}
