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
package com.raytheon.uf.common.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;

import com.raytheon.uf.common.jms.qpid.IBrokerRestProvider;

/**
 * JMS connection factory with additional properties
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 11, 2019 7724       tgurney     Initial creation
 * </pre>
 *
 * @author tgurney
 */

public class WrappedJmsConnectionFactory implements ConnectionFactory {
    private final IBrokerRestProvider brokerRestProvider;

    private String provider = "QPID";

    private ConnectionFactory connFactory;

    public WrappedJmsConnectionFactory(ConnectionFactory connFactory,
            IBrokerRestProvider brokerRestProvider) {
        this.connFactory = connFactory;
        this.brokerRestProvider = brokerRestProvider;
    }

    public IBrokerRestProvider getBrokerRestProvider() {
        return brokerRestProvider;
    }

    @Override
    public Connection createConnection(String arg0, String arg1)
            throws JMSException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " "
                + "does not support username/password connections");
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public JMSContext createContext(String userName, String password) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " "
                + "does not support username/password connections");
    }

    @Override
    public JMSContext createContext(String userName, String password,
            int sessionMode) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " "
                + "does not support username/password connections");
    }

    @Override
    public Connection createConnection() throws JMSException {
        return connFactory.createConnection();
    }

    @Override
    public JMSContext createContext() {
        return connFactory.createContext();
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        return connFactory.createContext(sessionMode);
    }
}
