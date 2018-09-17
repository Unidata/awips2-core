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
package com.raytheon.uf.edex.database.jdbc;

import java.io.IOException;
import java.util.Map;

import org.geotools.data.postgis.PostgisNGDataStoreFactory;

/**
 * Data store factory for SSL PostGIS database connections.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 17, 2018   6979     mduff       Extracted from DbShapeSource for reuse.
 *
 * </pre>
 *
 * @author mpduff
 */

public class SSLEnabledPostgisNGDataStoreFactory
        extends PostgisNGDataStoreFactory {

    /** parameter for database type */
    public static final Param SSL_MODE = new Param("sslmode", String.class,
            "ssl mode", true, "verify-ca");

    public static final Param SSL_CERT = new Param("sslcert", String.class,
            "path to local ssl certificate", true, "filename.crt");

    public static final Param SSL_KEY = new Param("sslkey", String.class,
            "path to local ssl key", true, "filename.pk8");

    public static final Param SSL_ROOTCERT = new Param("sslrootcert",
            String.class, "path to ssl root certificate", true, "root.crt");

    @Override
    protected String getJDBCUrl(Map params) throws IOException {
        StringBuilder sb = new StringBuilder(super.getJDBCUrl(params));
        Param[] sslParams = { SSL_MODE, SSL_CERT, SSL_KEY, SSL_ROOTCERT };
        boolean first = sb.indexOf("?") < 0;
        for (Param param : sslParams) {
            String value = (String) param.lookUp(params);
            sb.append(first ? '?' : '&').append(param.key).append('=')
                    .append(value);
            first = false;
        }
        return sb.toString();
    }

    @Override
    protected void setupParameters(Map parameters) {
        super.setupParameters(parameters);
        parameters.put(SSL_KEY.key, SSL_KEY);
        parameters.put(SSL_MODE.key, SSL_MODE);
        parameters.put(SSL_CERT.key, SSL_CERT);
        parameters.put(SSL_ROOTCERT.key, SSL_ROOTCERT);

    }
}
