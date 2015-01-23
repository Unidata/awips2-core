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
package com.raytheon.uf.common.comm;

/**
 * Configuration holder used to construct HTTP client instances. Uses builder
 * pattern with {@link HttpClientConfigBuilder}
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 3, 2014  3570       bclement     Initial creation
 * Nov 15, 2014 3757       dhladky      General HTTPS handler
 * Jan 26, 2015 3952       njensen      gzip handled by default
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class HttpClientConfig {

    private final int socketTimeout;

    private final int connectionTimeout;

    private final int maxConnections;

    private final IHttpsHandler httpsHandler;

    private final boolean tcpNoDelay;

    private final boolean expectContinueEnabled;

    /**
     * Protected constructor used by builder.
     * 
     * @param socketTimeout
     * @param connectionTimeout
     * @param maxConnections
     * @param handler
     * @param httpsConfiguration
     */
    protected HttpClientConfig(int socketTimeout, int connectionTimeout,
            int maxConnections, IHttpsHandler handler, boolean tcpNoDelay,
            boolean expectContinueEnabled) {
        /*
         * This is protected to limit required changes if the arguments change
         * in the future. Callers should use the builder to construct configs.
         */
        this.socketTimeout = socketTimeout;
        this.connectionTimeout = connectionTimeout;
        this.maxConnections = maxConnections;
        this.httpsHandler = handler;
        this.tcpNoDelay = tcpNoDelay;
        this.expectContinueEnabled = expectContinueEnabled;
    }

    /**
     * @return the socketTimeout
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * @return the connectionTimeout
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @return the maxConnections
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * @return the httpsHandler
     */
    public IHttpsHandler getHttpsHandler() {
        return httpsHandler;
    }

    /**
     * @return the tcpNoDelay
     */
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * @return the expectContinueEnabled
     */
    public boolean isExpectContinueEnabled() {
        return expectContinueEnabled;
    }

}
