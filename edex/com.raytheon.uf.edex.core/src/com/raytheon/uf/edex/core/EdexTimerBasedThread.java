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
package com.raytheon.uf.edex.core;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Timer based threading. Allows previous thread based paradigms
 * to hook in to a camel context with minimal work.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 19, 2014 2826       rjpeter     Initial creation.
 * Mar 30, 2017 5937       rjpeter     Updated EdexTimerBasedThread to manage its own threads.
 * Jan 18, 2018 7195       tjensen     Ensure running is set after start
 * </pre>
 *
 * @author rjpeter
 */
public abstract class EdexTimerBasedThread implements IContextStateProcessor {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Current active threads.
     */
    protected final List<Thread> threads = new LinkedList<>();

    protected int threadCount = 1;

    /**
     * Whether the container is running or not.
     */
    protected volatile boolean running = true;

    /**
     * Interval thread should sleep between calls.
     */
    protected int threadSleepInterval = 30000;

    /**
     * The name to use for the threads.
     *
     * @return
     */
    public abstract String getThreadGroupName();

    /**
     * Method to do the work. Should return when done. Run method handles start
     * up/shutdown mechanism.
     *
     * @throws Exception
     */
    public abstract void process() throws Exception;

    /**
     * Can be overridden to do any work to cleanup the thread on shutdown.
     */
    public void dispose() {

    }

    /**
     * Called by camel to do the processing. Will run until the context is
     * shutdown.
     */
    public void run() {
        try {
            while (running) {
                try {
                    process();
                } catch (Exception e) {
                    logger.error("Error occurred during processing", e);
                }

                if (running) {
                    try {
                        /*
                         * use waiter to allow shutdown to wake thread for
                         * immediate shutdown
                         */
                        synchronized (threads) {
                            threads.wait(threadSleepInterval);
                        }
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        } finally {
            synchronized (threads) {
                threads.remove(Thread.currentThread());
                threads.notify();
            }

            dispose();
        }
    }

    @Override
    public void preStart() {
    }

    @Override
    public void postStart() {
        running = true;

        logger.info("Launching " + threadCount + " " + getThreadGroupName()
                + " threads");

        synchronized (threads) {
            for (int i = 1; i <= threadCount; i++) {
                String threadName = getThreadGroupName()
                        + (threadCount > 1 ? "-" + i : "");
                Thread t = new Thread(threadName) {
                    @Override
                    public void run() {
                        EdexTimerBasedThread.this.run();
                    }
                };
                threads.add(t);
                t.start();
            }
        }
    }

    @Override
    public void preStop() {
        running = false;

        synchronized (threads) {
            threads.notifyAll();
        }
    }

    @Override
    public void postStop() {
        synchronized (threads) {
            while (!threads.isEmpty()) {
                logger.info("Waiting for " + threads.size() + " "
                        + getThreadGroupName() + " threads to finish");

                try {
                    threads.wait(10000);
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    public int getThreadSleepInterval() {
        return threadSleepInterval;
    }

    public void setThreadSleepInterval(int threadSleepInterval) {
        this.threadSleepInterval = threadSleepInterval;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }
}
