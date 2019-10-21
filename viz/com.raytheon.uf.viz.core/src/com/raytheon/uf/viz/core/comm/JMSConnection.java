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

package com.raytheon.uf.viz.core.comm;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import com.raytheon.uf.common.jms.JMSConnectionInfo;
import com.raytheon.uf.common.jms.qpid.QpidUFConnectionFactory;
import com.raytheon.uf.viz.core.VizApp;

/**
 *
 * Common JMS connection code
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 24, 2008            chammack    Moved to uf.viz.core
 * Nov 2, 2009  #3067      chammack    Send all jms connections through failover:// to properly reconnect
 * Nov 2, 2011  #7391      bkowal      Ensure that the generated WsId is properly formatted to be
 *                                     included in a url.
 * May 09, 2013 1814       rjpeter     Updated prefetch to 10.
 * Aug 16, 2013 2169       bkowal      CAVE will now synchronously acknowledge messages.
 * Aug 27, 2013 2295       bkowal      The entire connection string is now provided by EDEX; so, it
 *                                     no longer needs to be constructed. Replaced stacktrace
 *                                     printing with UFStatus.
 * Feb 02, 2017 6085       bsteffen    Enable ssl in the JMS connection.
 * Sep 23, 2019 7724       mrichardson Upgrade Qpid to Qpid Proton.
 * Oct 16, 2019 7724       tgurney     Replace connection string with a
 *                                     {@link JMSConnectionInfo} object
 *
 * </pre>
 *
 * @author chammack
 */
public class JMSConnection {
    private static JMSConnection instance;

    private ConnectionFactory factory;

    public static synchronized JMSConnection getInstance() throws JMSException {
        if (instance == null) {
            instance = new JMSConnection();
        }

        return instance;
    }

    public JMSConnection() throws JMSException {
        this(VizApp.getJmsConnectionInfo());
    }

    public JMSConnection(JMSConnectionInfo connInfo) throws JMSException {
        try {
            this.factory = new QpidUFConnectionFactory(connInfo);
        } catch (Exception e) {
            JMSException wrapper = new JMSException(
                    "Failed to connect to the JMS Server!");
            wrapper.initCause(e);
            throw wrapper;
        }
    }

    /**
     * @return the factory
     */
    public ConnectionFactory getFactory() {
        return factory;
    }
}
