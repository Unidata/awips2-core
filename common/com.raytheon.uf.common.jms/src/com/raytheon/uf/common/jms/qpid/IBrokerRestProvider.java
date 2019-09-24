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
 *
 * </pre>
 *
 * @author randerso
 */
public interface IBrokerRestProvider {

    /**
     * Create the specified queue.
     *
     * @param queue
     *            the name of the queue to be created
     * @return true if successfully created
     */
    public boolean createQueue(String queue) throws JMSConfigurationException,
            CommunicationException;

    /**
     * Create the specified queue.
     *
     * @param hostname
     *            the hostname where the broker is located
     * @param vhost
     *            the name of the virtual host
     * @return true if successfully created
     */
    public boolean createQueue(String hostname, String queue, String vhost)
            throws JMSConfigurationException, CommunicationException;

    /**
     * Create a binding for the specified queue.
     *
     * @param name
     *            the name of the binding
     * @param type
     *            the exchange type for the binding
     * @return true if successfully created
     */
    public boolean createBinding(String name, String type) throws JMSConfigurationException,
            CommunicationException;

    /**
     * Create a binding for the specified queue.
     *
     * @param hostname
     *            the hostname where the broker is located
     * @param queue
     *            the name of the queue to create a binding on
     * @param type
     *            the exchange type for the binding
     * @param vhost
     *            the name of the virtual host
     * @return true if successfully created
     */
    public boolean createBinding(String hostname, String queue, String type, String vhost)
            throws JMSConfigurationException, CommunicationException;

    /**
     * Determine if the specified queue exists
     *
     * @param url
     *            url that specifies which queue to check for
     * @return true if queue exists
     */
    public boolean queueExists(String url) throws CommunicationException,
            JMSConfigurationException;

    /**
     * Determine if the specified exchange exists
     *
     * @param url
     *            url used to get the list of exchanges
     * @param name
     *            name of the exchange to check exists
     * @return true if exchange exists
     */
    public boolean exchangeExists(String url, String name) throws CommunicationException,
            JMSConfigurationException;
    
    /**
     * @return list of Client IDs for active broker connections
     *
     * @throws CommunicationException
     * @throws JMSConfigurationException
     */
    public List<String> getConnections() throws CommunicationException,
            JMSConfigurationException, HttpServerException;

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
    public void deleteQueue(String queue) throws CommunicationException,
            JMSConfigurationException, HttpServerException;
}