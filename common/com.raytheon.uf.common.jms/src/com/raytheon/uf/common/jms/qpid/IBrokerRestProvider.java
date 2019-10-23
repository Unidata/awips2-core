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
package com.raytheon.uf.common.jms.qpid;

import java.util.List;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.comm.HttpServerException;

/**
 * Provides an interface the JMS broker's REST API
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- -------------------------------------
 * Apr 04, 2014           randerso    Initial creation
 * Jan 25, 2017  6092     randerso    Renamed and added queueReady method
 * Jul 17, 2019  7724     mrichardson Upgrade Qpid to Qpid Proton.
 * Oct 22, 2019  7724     tgurney     API cleanup
 * Oct 23, 2019  7724     tgurney     Deprecate {@link #deleteQueue(String)}
 *
 * </pre>
 *
 * @author randerso
 */
public interface IBrokerRestProvider {

    /**
     * Create the specified queue. Also creates a direct binding with the same
     * name as the new queue that points to the new queue. This operation is
     * idempotent
     *
     * @param queue
     *            the name of the queue to be created
     * @throws CommunicationException
     *             if communication with the server failed
     * @throws HttpServerException
     *             if the server returned an error
     */
    void createQueue(String queue)
            throws HttpServerException, CommunicationException;

    /**
     * @return list of Client IDs for active broker connections
     *
     * @throws CommunicationException
     *             if communication with the server failed
     * @throws JMSConfigurationException
     *             if the server returned an error
     */
    List<String> getConnections()
            throws HttpServerException, CommunicationException;

    /**
     * Determine if the specified queue exists and is ready to receive messages
     *
     * @param queue
     *            the name of the queue to be queried
     * @return true if queue exists
     *
     * @throws CommunicationException
     *             if communication with the server failed
     * @throws HttpServerException
     *             if the server returned an error
     */
    boolean queueReady(String queue)
            throws HttpServerException, CommunicationException;

    /**
     * Delete the specified JMS queue. This operation is idempotent
     *
     * @param queue
     *            the name of the queue to be deleted
     *
     * @throws CommunicationException
     *             if communication with the server failed
     * @throws HttpServerException
     *             if the server returned an error.
     * @deprecated Don't do this. It may cause problems with internal caching
     *             mechanisms, such that deleted queues may not be createable
     *             again via {@link #createQueue(String)}. If you find yourself
     *             needing to delete queues you should be creating those as
     *             temporary queues instead, using
     *             {@link javax.jms.Session#createTemporaryQueue()}
     */
    @Deprecated
    void deleteQueue(String queue)
            throws HttpServerException, CommunicationException;
}