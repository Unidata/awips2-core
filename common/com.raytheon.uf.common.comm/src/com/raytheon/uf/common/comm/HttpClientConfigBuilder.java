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
 * Builder pattern implementation for creating immutable HTTP configuration
 * objects
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 03, 2014  3570      bclement     Initial creation
 * Nov 15, 2014  3757      dhladky      General HTTPS handler
 * Jan 26, 2015  3952      njensen      gzip handled by default
 * Jun 30, 2015            mjames@ucar  maxConnections increased from 10 to 1000
 * Jul 06, 2015  4614      njensen      Add gzipEnabled
 * Dec 07, 2015  4834      njensen      Changed for rename of IHttpsHandler to HttpAuthHandler
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class HttpClientConfigBuilder {

    private int socketTimeout = 330000;

    private int connectionTimeout = 10000;

    private int maxConnections = 1000;

    private HttpAuthHandler httpAuthHandler;

    private boolean tcpNoDelay = true;

    private boolean expectContinueEnabled = true;

    private boolean gzipEnabled = false;

    /**
     * 
     */
    public HttpClientConfigBuilder() {
    }

    /**
     * Creates a builder using settings in an existing config
     * 
     * @param config
     */
    public HttpClientConfigBuilder(HttpClientConfig config) {
        this.setConnectionTimeout(config.getConnectionTimeout());
        this.setHttpAuthHandler(config.getHttpAuthHandler());
        this.setMaxConnections(config.getMaxConnections());
        this.setSocketTimeout(config.getSocketTimeout());
        this.setTcpNoDelay(config.isTcpNoDelay());
        this.setExpectContinueEnabled(config.isExpectContinueEnabled());
    }

    public static HttpClientConfig defaultConfig() {
        return new HttpClientConfigBuilder().build();
    }

    /**
     * @return immutable configuration object
     */
    public HttpClientConfig build() {
        return new HttpClientConfig(socketTimeout, connectionTimeout,
                maxConnections, httpAuthHandler, tcpNoDelay,
                expectContinueEnabled, gzipEnabled);
    }

    /**
     * @param socketTimeout
     *            the socketTimeout to set
     */
    public HttpClientConfigBuilder withSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    /**
     * @param connectionTimeout
     *            the connectionTimeout to set
     */
    public HttpClientConfigBuilder withConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    /**
     * @param maxConnections
     *            the maxConnections to set
     */
    public HttpClientConfigBuilder withMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    /**
     * @param handler
     *            the handler to set
     */
    public HttpClientConfigBuilder withHttpsHandler(HttpAuthHandler handler) {
        this.httpAuthHandler = handler;
        return this;
    }

    /**
     * @param tcpNoDelay
     *            the tcpNoDelay to set
     */
    public HttpClientConfigBuilder withTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
        return this;
    }

    /**
     * @param expectContinueEnabled
     *            the expectContinueEnabled to set
     */
    public HttpClientConfigBuilder withExpectContinueEnabled(
            boolean expectContinueEnabled) {
        this.expectContinueEnabled = expectContinueEnabled;
        return this;
    }

    /**
     * @return the socketTimeout
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * @param socketTimeout
     *            the socketTimeout to set
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * @return the connectionTimeout
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @param connectionTimeout
     *            the connectionTimeout to set
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * @return the maxConnections
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * @param maxConnections
     *            the maxConnections to set
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * @return the httpsHandler
     */
    public HttpAuthHandler getHttpAuthHandler() {
        return httpAuthHandler;
    }

    /**
     * @param httpAuthHandler
     *            the httpAuthHandler to set
     */
    public void setHttpAuthHandler(HttpAuthHandler httpAuthHandler) {
        this.httpAuthHandler = httpAuthHandler;
    }

    /**
     * @return the tcpNoDelay
     */
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * @param tcpNoDelay
     *            the tcpNoDelay to set
     */
    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    /**
     * @return the expectContinueEnabled
     */
    public boolean isExpectContinueEnabled() {
        return expectContinueEnabled;
    }

    /**
     * @param expectContinueEnabled
     *            the expectContinueEnabled to set
     */
    public void setExpectContinueEnabled(boolean expectContinueEnabled) {
        this.expectContinueEnabled = expectContinueEnabled;
    }

    public boolean isGzipEnabled() {
        return gzipEnabled;
    }

    public void setGzipEnabled(boolean gzipEnabled) {
        this.gzipEnabled = gzipEnabled;
    }

}
