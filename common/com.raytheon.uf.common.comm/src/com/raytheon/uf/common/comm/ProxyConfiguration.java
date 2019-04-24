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

import com.raytheon.uf.common.util.StringUtil;

/**
 * Structure to hold proxy settings
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 3, 2013    1786     mpduff      Initial creation
 * 8/28/2013    1538       bphillip    Added nonProxyHosts field
 * 6/18/2014    3255       bphillip    Added complete set of Java Proxy properties
 * 6/18/2014    1712        bphillip    Updated Proxy configuration
 * Feb 25, 2019 6140       tgurney     Remove SOCKS config (Postgis JDBC fix)
 *
 * </pre>
 *
 * @author mpduff
 */

public class ProxyConfiguration {

    /** HTTP proxy host environment variable name */
    private static final String HTTP_PROXY_HOST = "http.proxyHost";

    /** HTTP proxy port environment variable name */
    private static final String HTTP_PROXY_PORT = "http.proxyPort";

    /** HTTP hosts that bypass the proxy */
    private static final String HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";

    /** HTTPS proxy host environment variable name */
    private static final String HTTPS_PROXY_HOST = "https.proxyHost";

    /** HTTPS proxy port environment variable name */
    private static final String HTTPS_PROXY_PORT = "https.proxyPort";

    /** FTP proxy host environment variable name */
    private static final String FTP_PROXY_HOST = "ftp.proxyHost";

    /** FTP proxy port environment variable name */
    private static final String FTP_PROXY_PORT = "ftp.proxyPort";

    /** FTP hosts that bypass the proxy */
    private static final String FTP_NON_PROXY_HOSTS = "ftp.nonProxyHosts";

    /** Default HTTP (and FTP) proxy port */
    private static final String HTTP_PROXY_DEFAULT_PORT = "80";

    /** Default HTTPS proxy port */
    private static final String HTTPS_PROXY_DEFAULT_PORT = "443";

    /** Flag set if http proxy information is defined */
    public static final boolean HTTP_PROXY_DEFINED;

    /** Flag set if https proxy information is defined */
    public static final boolean HTTPS_PROXY_DEFINED;

    /** Flag set if ftp proxy information is defined; */
    public static final boolean FTP_PROXY_DEFINED;

    static {
        HTTP_PROXY_DEFINED = !StringUtil.isEmptyString(getHttpProxyHost())
                && !StringUtil.isEmptyString(getHttpProxyPortString());
        HTTPS_PROXY_DEFINED = !StringUtil.isEmptyString(getHttpsProxyHost())
                && !StringUtil.isEmptyString(getHttpsProxyPortString());
        FTP_PROXY_DEFINED = !StringUtil.isEmptyString(getFtpProxyHost())
                && !StringUtil.isEmptyString(getFtpProxyPortString());
    }

    /**
     * Initializes the proxy settings from the System properties
     */
    public ProxyConfiguration() {

    }

    /**
     * @return the httpProxyHost
     */
    public static String getHttpProxyHost() {
        return System.getProperty(HTTP_PROXY_HOST);
    }

    /**
     * @return the httpProxyPort
     */
    public static int getHttpProxyPort() {
        return Integer.parseInt(getHttpProxyPortString());
    }

    /**
     * @return the httpProxyPort as a string
     */
    public static String getHttpProxyPortString() {
        return System.getProperty(HTTP_PROXY_PORT, HTTP_PROXY_DEFAULT_PORT);
    }

    /**
     * @return the httpNonProxyHosts
     */
    public static String getHttpNonProxyHosts() {
        return System.getProperty(HTTP_NON_PROXY_HOSTS);
    }

    /**
     * @return the httpsProxyHost
     */
    public static String getHttpsProxyHost() {
        return System.getProperty(HTTPS_PROXY_HOST);
    }

    /**
     * @return the httpsProxyPort
     */
    public static int getHttpsProxyPort() {
        return Integer.parseInt(getHttpsProxyPortString());
    }

    /**
     * @return the httpsProxyPort as a string
     */
    public static String getHttpsProxyPortString() {
        return System.getProperty(HTTPS_PROXY_PORT, HTTPS_PROXY_DEFAULT_PORT);
    }

    /**
     * @return the httpsNonProxyHosts
     */
    public static String getHttpsNonProxyHosts() {
        return getHttpNonProxyHosts();
    }

    /**
     * @return the ftpProxyHost
     */
    public static String getFtpProxyHost() {
        return System.getProperty(FTP_PROXY_HOST);
    }

    /**
     * @return the ftpProxyPort
     */
    public static int getFtpProxyPort() {
        return Integer.parseInt(getFtpProxyPortString());
    }

    /**
     * @return the ftpProxyPort as a String
     */
    public static String getFtpProxyPortString() {
        // FTP default port is also 80
        return System.getProperty(FTP_PROXY_PORT, HTTP_PROXY_DEFAULT_PORT);
    }

    /**
     * @return the ftpNonProxyHosts
     */
    public static String getFtpNonProxyHosts() {
        return System.getProperty(FTP_NON_PROXY_HOSTS);
    }
}
