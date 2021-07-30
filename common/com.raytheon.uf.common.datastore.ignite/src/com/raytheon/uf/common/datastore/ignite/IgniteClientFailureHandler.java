/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastore.ignite;

import java.util.ArrayList;
import java.util.List;

import org.apache.ignite.Ignite;
import org.apache.ignite.failure.AbstractFailureHandler;
import org.apache.ignite.failure.FailureContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for when an ignite client node fails. Delegates to registered
 * listeners for actually handling the failure.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 25, 2021 8450       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class IgniteClientFailureHandler extends AbstractFailureHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<IgniteClientFailureListener> listeners = new ArrayList<>();

    @Override
    protected boolean handle(Ignite ignite, FailureContext failureCtx) {
        String threadName = "awips-ignite-client-failure-handler-"
                + ignite.configuration().getIgniteInstanceName();
        new Thread(new Runnable() {
            @Override
            public void run() {
                logger.error("Ignite client node failed: [failureCtx="
                        + failureCtx + ']');

                synchronized (listeners) {
                    for (IgniteClientFailureListener listener : listeners) {
                        listener.handle(ignite, failureCtx);
                    }
                }
            }
        }, threadName).start();

        return true;
    }

    /**
     * Add listener for handling ignite failure.
     *
     * @param listener
     *            the listener to add
     */
    public void addListener(IgniteClientFailureListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public interface IgniteClientFailureListener {

        void handle(Ignite ignite, FailureContext failureCtx);
    }
}
