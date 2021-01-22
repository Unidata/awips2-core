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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.MoreExecutors;
import com.raytheon.uf.common.python.PythonInterpreter;

/**
 * Specialized version of a {@link ThreadPoolExecutor} that executes python code
 * using instances of {@link PythonInterpreter}. The primary inteface to this
 * class should be through {@link PythonJobCoordinator}; however, that is not
 * required. If a {@link PythonScriptPseudoCallable} is passed to this executor,
 * it will be re-wrapped as a {@link PythonListenableFutureTask} so the proper
 * thread-specific {@link PythonInterpreter} instance is passed to it.
 * Otherwise, all submitted jobs will simply pass through and be executed as if
 * this is an unmodified {@link ThreadPoolExecutor}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 10, 2015  #4816     dgilling     Initial creation
 * 
 * </pre>
 * 
 * @author dgilling
 * @version 1.0
 */

class PythonInterpreterThreadPoolExecutor<P extends PythonInterpreter> extends
        ThreadPoolExecutor {

    /**
     * Creates a new {@code PythonInterpreterThreadPoolExecutor} with the given
     * parameters for number of threads and thread factory.
     * 
     * @param numThreads
     *            the number of threads in the pool
     * @param threadFactory
     *            the factory to use when the executor creates a new thread
     */
    PythonInterpreterThreadPoolExecutor(int numThreads,
            PythonThreadFactory<P> threadFactory) {
        super(numThreads, numThreads, 0L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), threadFactory);
    }

    /**
     * Creates a new {@code PythonInterpreterThreadPoolExecutor} with a limited
     * size work queue for tasks that have not yet been executed. If the work
     * queue exceeds its bounds, any new tasks that are submitted will throw a
     * {@link RejectedExecutionException}.
     * 
     * @param numThreads
     *            the number of threads in the pool
     * @param workQueueSize
     *            the maximum size for the work queue of tasks to be executed
     * @param threadFactory
     *            the factory to use when the executor creates a new thread
     */
    PythonInterpreterThreadPoolExecutor(int numThreads, int workQueueSize,
            PythonThreadFactory<P> threadFactory) {
        super(numThreads, numThreads, 0L, TimeUnit.SECONDS,
                ((workQueueSize >= 1) ? new LinkedBlockingQueue<Runnable>(
                        workQueueSize) : new SynchronousQueue<Runnable>()),
                threadFactory);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        if (r instanceof PythonListenableFutureTask) {
            PythonListenableFutureTask<P, ?> casted = (PythonListenableFutureTask<P, ?>) r;
            ThreadLocal<P> threadLocal = ((PythonThreadFactory) getThreadFactory())
                    .getThreadLocal();
            casted.setThreadPython(threadLocal);
        }

        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        if (r instanceof PythonListenableFutureTask) {
            PythonListenableFutureTask<P, ?> casted = (PythonListenableFutureTask<P, ?>) r;
            casted.setThreadPython(null);
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return super.newTaskFor(runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        if (callable instanceof PythonScriptPseudoCallable) {
            PythonScriptPseudoCallable<P, T> casted = (PythonScriptPseudoCallable<P, T>) callable;
            return new PythonListenableFutureTask<>(casted.getPython(),
                    casted.getListener());
        } else {
            return super.newTaskFor(callable);
        }
    }

    /**
     * A {@link RunnableFuture} implementation that asynchronously executes a
     * result-value returning job using a {@link PytonInterpreter} and
     * {@link IPythonExecutor}. If a {@link IPythonJobListener} instance is
     * provided the appropriate listener callback will be fired once this job
     * has been executed. Else, clients will have to use the {@link isDone} or
     * {@link get} methods to determine when this job has completed execution.
     * 
     * @author dgilling
     * @version 1.0
     * @param <S>
     *            The {@code PythonInterpreter} type to execute the job.
     * @param <R>
     *            The return type of the job.
     */
    private class PythonListenableFutureTask<S extends PythonInterpreter, R>
            implements RunnableFuture<R> {

        /**
         * Delegate Future object to provide all the RunnableFuture methods. We
         * use a ListenableFuture so we can have the executor fire our
         * IPythonJobListener callbacks if a listener was provided. We use a
         * delegate object because guava does not let us subclass
         * ListenableFutureTask for some reason.
         */
        private final ListenableFutureTask<R> futureDelegate;

        /*
         * When the executor executes this job, it will set this object to the
         * thread-specific instance of the PythonInterpreter.
         */
        private ThreadLocal<S> threadPython;

        /**
         * Constructs a new instance.
         * 
         * @param executor
         *            The {@code IPythonExecutor} instance to use for the job.
         * @param listener
         *            Optional {@code IPythonJobListener} instance to use to
         *            return results asynchronously.
         */
        PythonListenableFutureTask(IPythonExecutor<S, R> executor,
                IPythonJobListener<R> listener) {
            this.futureDelegate = ListenableFutureTask
                    .create(getCallable(executor));
            if (listener != null) {
                FutureCallback<R> callback = getCallback(listener);
                Futures.addCallback(this.futureDelegate, callback,
                        MoreExecutors.directExecutor());
            }
            this.threadPython = null;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return futureDelegate.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return futureDelegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return futureDelegate.isDone();
        }

        @Override
        public R get() throws InterruptedException, ExecutionException {
            return futureDelegate.get();
        }

        @Override
        public R get(long timeout, TimeUnit unit) throws InterruptedException,
                ExecutionException, TimeoutException {
            return futureDelegate.get(timeout, unit);
        }

        @Override
        public void run() {
            futureDelegate.run();
        }

        public void setThreadPython(ThreadLocal<S> threadPython) {
            this.threadPython = threadPython;
        }

        private Callable<R> getCallable(final IPythonExecutor<S, R> executor) {
            return new Callable<R>() {

                @Override
                public R call() throws Exception {
                    try {
                        S script = threadPython.get();
                        return executor.execute(script);
                    } catch (Throwable t) {
                        throw new PythonJobFailedException(t);
                    }
                }
            };
        }

        private FutureCallback<R> getCallback(
                final IPythonJobListener<R> listener) {
            return new FutureCallback<R>() {

                @Override
                public void onSuccess(R result) {
                    listener.jobFinished(result);
                }

                @Override
                public void onFailure(Throwable t) {
                    listener.jobFailed(t);
                }
            };
        }
    }
}
