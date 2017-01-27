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
package com.raytheon.uf.edex.esb.camel.jms;

import java.util.List;

import com.raytheon.uf.common.comm.CommunicationException;

/**
 * Provides an interface the JMS broker's REST API
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -------------------------------------
 * Apr 04, 2014           randerso  Initial creation
 * Jan 25, 2017  6092     randerso  Renamed and added queueReady method
 *
 * </pre>
 *
 * @author randerso
 */
public interface IBrokerRestProvider {

    /**
     * @return list of Client IDs for active broker connections
     *
     * @throws CommunicationException
     * @throws JMSConfigurationException
     */
    public List<String> getConnections()
            throws CommunicationException, JMSConfigurationException;

    /**
     * Determine if the specified queue exists and is ready to receive messages
     *
     * @param queue
     *            the name of the queue to be queried
     * @return true if queue exists
     *
     * @throws CommunicationException
     * @throws JMSConfigurationException
     */
    public boolean queueReady(String queue)
            throws CommunicationException, JMSConfigurationException;

    /**
     * Delete the specified JMS queue
     *
     * @param queue
     *            the name of the queue to be deleted
     *
     * @throws CommunicationException
     * @throws JMSConfigurationException
     */
    public void deleteQueue(String queue)
            throws CommunicationException, JMSConfigurationException;
}