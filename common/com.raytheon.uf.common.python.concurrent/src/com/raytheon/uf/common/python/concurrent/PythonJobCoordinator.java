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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.raytheon.uf.common.python.PythonInterpreter;

/**
 * Interface to get to an {@link ExecutorService}. Allows multiple thread pools
 * to be created in a single JVM, by passing in a different application name.
 * 
 * This class will be used in this way:
 * 
 * 
 * <pre>
 * 
 *       AbstractPythonScriptFactory<PythonInterpreter, Object> factory = new CAVEPythonFactory();
 *       PythonJobCoordinator coordinator = PythonJobCoordinator
 *               .newInstance(2, "CAVEPython", factory);
 *       IPythonExecutor<PythonInterpreter, Object> executor = new CAVEExecutor(
 *               args);
 *       try {
 *           coordinator.submitJobWithCallback(executor, listener);
 *       } catch (Exception e) {
 *           e.printStackTrace();
 *       }
 * 
 * }
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 31, 2013            mnash       Initial creation
 * Jun 04, 2013 2041       bsteffen    Improve exception handling for concurrent
 *                                     python.
 * Mar 21, 2014 2868       njensen     Changed getInstance() from throwing
 *                                     RuntimeException to IllegalArgumentException
 *                                     Added refCount
 * Dec 10, 2015 4816       dgilling    Remove static Map of pools from this class.
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */
public class PythonJobCoordinator<P extends PythonInterpreter> {

    private ExecutorService execService;

    /**
     * Creates a new thread pool instance with the specified number of threads.
     * At any time, up to {@code numTheads} threads can be executing. If
     * additional tasks are submitted when all threads are active, they will
     * wait in the queue until a thread is available. If any thread terminates
     * due to a failure during execution prior to shutdown, a new one will take
     * its place if needed to execute subsequent tasks. The threads in the pool
     * will exist until it is explicitly shutdown.
     * 
     * @param numThreads
     *            Number of threads to allocate to this thread pool.
     * @param name
     *            Name to assign to this thread pool. Will be used as a name
     *            prefix attached to each thread in the thread pool.
     * @param scriptFactory
     *            {@code AbstractPythonScriptFactory} instance to build
     *            {@code PythonInterpreter} objects used to run jobs.
     * @return The newly created thread pool
     */
    public PythonJobCoordinator(int numThreads, String name,
            final PythonInterpreterFactory<P> scriptFactory) {
        this.execService = new PythonInterpreterThreadPoolExecutor<>(
                numThreads, new PythonThreadFactory<>(scriptFactory, name));
    }

    /**
     * Creates a new thread pool instance with the specified number of threads.
     * At any time, up to {@code numTheads} threads can be executing. If
     * additional tasks are submitted when all threads are active, they will
     * wait in the queue until a thread is available. However, this queue can
     * only hold up to {@code workQueueSize} tasks before rejecting any further
     * submitted tasks. If any thread terminates due to a failure during
     * execution prior to shutdown, a new one will take its place if needed to
     * execute subsequent tasks. The threads in the pool will exist until it is
     * explicitly shutdown.
     * 
     * @param numThreads
     *            Number of threads to allocate to this thread pool.
     * @param name
     *            Name to assign to this thread pool. Will be used as a name
     *            prefix attached to each thread in the thread pool.
     * @param workQueueSize
     *            the maximum size for the work queue of tasks to be executed
     * @param scriptFactory
     *            {@code AbstractPythonScriptFactory} instance to build
     *            {@code PythonInterpreter} objects used to run jobs.
     * @return The newly created thread pool
     */
    public PythonJobCoordinator(int numThreads, String name, int workQueueSize,
            final PythonInterpreterFactory<P> scriptFactory) {
        this.execService = new PythonInterpreterThreadPoolExecutor<>(
                numThreads, workQueueSize, new PythonThreadFactory<>(
                        scriptFactory, name));
    }

    /**
     * Submits a job to the {@link ExecutorService}. Fires a listener back after
     * it is done. This should be used for asynchronous operations.
     * 
     * @param executor
     *            {@code IPythonExecutor} instance to run.
     * @param listener
     *            {@code IPythonJobListener} instance to fire once job is run.
     * @return A {@code Future} representing pending completion of the job.
     * @throws IllegalArgumentException
     *             If a null listener is supplied.
     * @throws RejectedExecutionException
     *             If the job cannot be scheduled for execution.
     */
    public <R> Future<R> submitJobWithCallback(IPythonExecutor<P, R> executor,
            IPythonJobListener<R> listener) {
        // fail if the listener is null, bad things happen then
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        Callable<R> job = new PythonScriptPseudoCallable<>(executor, listener);
        return execService.submit(job);
    }

    /**
     * Submits a job for execution and returns a Future representing the pending
     * results of the job. The Future's <tt>get</tt> method will return the
     * task's result upon successful completion.
     * 
     * <p>
     * If you would like to immediately block waiting for a task, you can use
     * constructions of the form
     * <tt>result = exec.submitJob(aPyExecutor).get();</tt>
     * 
     * @param executor
     *            {@code IPythonExecutor} instance to run.
     * @return A {@code Future} representing pending completion of the job.
     * @throws RejectedExecutionException
     *             If the job cannot be scheduled for execution.
     */
    public <R> Future<R> submitJob(IPythonExecutor<P, R> executor) {
        // submit job
        Callable<R> job = new PythonScriptPseudoCallable<>(executor, null);
        return execService.submit(job);
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are
     * executed, but no new tasks will be accepted. Invocation has no additional
     * effect if already shut down.
     * 
     * <p>
     * All threads in the pool will dispose its {@code PythonInterpreter}
     * instance.
     * 
     * <p>
     * This method does not block waiting for previously submitted tasks to
     * complete execution. Use awaitTermination to do that.
     */
    public void shutdown() {
        execService.shutdown();
    }

    /**
     * Blocks until all tasks have completed execution after a shutdown request,
     * or the timeout occurs, or the current thread is interrupted, whichever
     * happens first.
     * 
     * @param timeout
     *            the maximum time to wait
     * @param unit
     *            unit the time unit of the timeout argument
     * @return {@code true} if this executor terminated and {@code false} if the
     *         timeout elapsed before termination
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        return execService.awaitTermination(timeout, unit);
    }
}
