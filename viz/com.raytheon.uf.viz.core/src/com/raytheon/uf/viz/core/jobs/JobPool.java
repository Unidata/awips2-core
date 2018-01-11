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
package com.raytheon.uf.viz.core.jobs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.raytheon.uf.common.util.Pair;

/**
 *
 * Create a specific number of Eclipse Jobs to handle tasks. This can be useful
 * if you have dozens or hundreds of tasks that each take a short time. Creating
 * a job for each task can result in more threads than is useful. If you instead
 * use a JobPool it reduces the number of threads by limiting the number of
 * eclipse jobs that are created. For many tasks a JobPool may perform faster
 * than using eclipse Jobs directly because thread creation and context
 * switching are reduced.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 28, 2011            bsteffen    Initial creation
 * Jan 12, 2018 7189       rjpeter     Refactored to use Phaser for join.
 * </pre>
 *
 * @author bsteffen
 */
public class JobPool {

    protected final LinkedBlockingQueue<Pair<Runnable, Phaser>> workQueue = new LinkedBlockingQueue<>();

    protected final LinkedBlockingQueue<Job> jobQueue = new LinkedBlockingQueue<>();

    protected final List<Job> jobList;

    protected Phaser joinPhaser = new Phaser(1);

    protected boolean cancel = false;

    protected Object lock = new Object();

    public JobPool(String name, int size) {
        this(name, size, null, null);
    }

    public JobPool(String name, int size, Boolean system) {
        this(name, size, system, null);
    }

    public JobPool(String name, int size, Boolean system, Integer priority) {
        jobList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            PooledJob job = new PooledJob(name);
            if (system != null) {
                job.setSystem(system);
            }
            if (priority != null) {
                job.setPriority(priority);
            }
            jobList.add(job);
            jobQueue.add(job);
        }
    }

    public void schedule(Runnable runnable) {
        // do not schedule while canceling(cancel should be fast).
        synchronized (lock) {
            if (cancel) {
                return;
            }

            joinPhaser.register();
            if (!workQueue.offer(new Pair<>(runnable, joinPhaser))) {
                joinPhaser.arriveAndDeregister();
            } else {
                Job job = jobQueue.poll();
                if (job != null) {
                    job.schedule();
                }
            }
        }
    }

    /**
     * Join on all previously submitted Runnables.
     */
    public void join() {
        Phaser priorPhaser;
        /*
         * need to use newPhaser reference so that we don't arriveAndAwait
         * inside sync block. joinPhaser reference could change while in
         * arriveAndAwait
         */
        Phaser newPhaser;
        synchronized (lock) {
            priorPhaser = joinPhaser;
            /*
             * 2 waiters on phaser, 1 for the next join, and one for
             * deregistration of this phaser to ensure join's occur in order
             */
            newPhaser = new Phaser(2);
            joinPhaser = newPhaser;
        }

        priorPhaser.arriveAndAwaitAdvance();
        newPhaser.arriveAndDeregister();
    }

    /**
     * Cancel the job pool, will clear out the workQueue then join on all jobs
     * running. Once canceled all future calls to schedule will be ignored.
     */
    public void cancel() {
        cancel(true);
    }

    /**
     * Cancel the job pool, will clear out the workQueue and optionally join
     * runnning jobs. Once canceled all future calls to schedule will be
     * ignored.
     *
     * @param join
     *            true if you want to join before returning.
     */
    public void cancel(boolean join) {
        List<Pair<Runnable, Phaser>> runnablesToDecrement = new ArrayList<>(
                workQueue.size());
        synchronized (lock) {
            cancel = true;
            workQueue.drainTo(runnablesToDecrement);
        }

        for (Pair<Runnable, Phaser> pair : runnablesToDecrement) {
            pair.getSecond().arriveAndDeregister();
        }

        if (join) {
            join();
        }
    }

    /**
     * Cancels the specified runnable. Returns true if the provided runnable was
     * waiting to be run but now is not. Returns false if the provided runnable
     * is already running or if it was not enqueued to begin with.
     *
     * @param runnable
     * @return
     */
    public boolean cancel(Runnable runnable) {
        Iterator<Pair<Runnable, Phaser>> iter = workQueue.iterator();

        while (iter.hasNext()) {
            Pair<Runnable, Phaser> pair = iter.next();
            if (pair.getFirst().equals(runnable)) {
                /*
                 * specifically do not call remove on iterator as it doesn't
                 * return if it was successfully removed or had been removed
                 * while equals is checked. Safety is required to avoid a double
                 * deregister which would break the join
                 */
                if (workQueue.remove(pair)) {
                    pair.getSecond().arriveAndDeregister();
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * A JobPool is considered active if any of the jobs it contains are running
     * or waiting to be run. When all scheduled work is run
     *
     * @return
     */
    public boolean isActive() {
        if (!workQueue.isEmpty()) {
            return true;
        }

        for (Job job : jobList) {
            int state = job.getState();
            if (state == Job.RUNNING || state == Job.WAITING) {
                return true;
            }
        }

        return false;
    }

    /**
     * get the number of tasks(Runnables) that are waiting to be run. This does
     * not include tasks that are currently running so even if there are no
     * waiting tasks the pool may still be active.
     *
     * @return
     */
    public int getWorkRemaining() {
        return workQueue.size();
    }

    protected class PooledJob extends Job {

        public PooledJob(String name) {
            super(name);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            Pair<Runnable, Phaser> pair = null;
            try {
                /*
                 * immediately offer myself up as available. This may result in
                 * double scheduling, but we never miss any work.
                 */
                jobQueue.offer(this);

                while ((pair = workQueue.poll()) != null) {
                    pair.getFirst().run();
                    pair.getSecond().arriveAndDeregister();
                    pair = null;
                }
            } finally {
                /*
                 * if this didn't finish then an error occurred, let the error
                 * go, but schedule this job again to finish its work.
                 */
                if (pair != null) {
                    /*
                     * if there was an exception during running the Job make
                     * sure and decrement the phaser
                     */
                    pair.getSecond().arriveAndDeregister();
                    this.schedule();
                }
            }
            return Status.OK_STATUS;
        }

    }

}
