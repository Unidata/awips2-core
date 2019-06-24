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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.raytheon.uf.common.python.PythonInterpreter;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

import jep.JepException;

/**
 * Creates new threads named according to what python task they were created
 * for. Based nearly identically off of {@link ThreadFactory}
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 31, 2013            mnash       Initial creation
 * Jun 04, 2013 2041       bsteffen    Improve exception handling for concurrent
 *                                     python.
 * Dec 10, 2015 4816       dgilling    Make classes package private.
 * Jun 03, 2019 7852       dgilling    Update code for jep 3.8.
 *
 * </pre>
 *
 * @author mnash
 * @version 1.0
 */

class PythonThreadFactory<P extends PythonInterpreter> implements ThreadFactory {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PythonThreadFactory.class);

    private static final AtomicInteger poolNumber = new AtomicInteger(1);

    private final ThreadGroup group;

    private final AtomicInteger threadNumber;

    private final String namePrefix;

    private final ThreadLocal<P> threadLocal;

    /**
     * Default constructor.
     *
     * @param scriptFactory
     *            {@code  IPythonInterpreterFactory} instance used to build the
     *            thread-specific {@code PythonInterpreter} instance for each
     *            thread.
     * @param name
     *            The name prefix to attach to each thread in the pool. A number
     *            suffix will be appended to ensure each thread is uniquely
     *            named.
     */
    PythonThreadFactory(final PythonInterpreterFactory<P> scriptFactory,
            String name) {
        this.threadLocal = new ThreadLocal<P>() {
            @Override
            protected P initialValue() {
                try {
                    return scriptFactory.createPythonScript();
                } catch (JepException e) {
                    throw new ScriptCreationException(e);
                }
            };
        };

        SecurityManager s = System.getSecurityManager();
        this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread()
                .getThreadGroup();
        this.namePrefix = name.toLowerCase() + "-pool-"
                + poolNumber.getAndIncrement() + "-thread-";
        this.threadNumber = new AtomicInteger(1);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new PythonThread(group, r, namePrefix
                + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon()) {
            t.setDaemon(false);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }

    public ThreadLocal<P> getThreadLocal() {
        return threadLocal;
    }

    private class PythonThread extends Thread {

        PythonThread(ThreadGroup group, Runnable target, String name,
                long stackSize) {
            super(group, target, name, stackSize);
        }

        @Override
        public void run() {
            try {
                super.run();
            } finally {
                try {
                    threadLocal.get().dispose();
                } catch (JepException e) {
                    statusHandler.debug("Failed to dispose script instance.",
                            e);
                }
                threadLocal.remove();
            }
        }

    };
}
