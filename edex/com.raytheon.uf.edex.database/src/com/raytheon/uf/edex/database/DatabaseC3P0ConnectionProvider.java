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

package com.raytheon.uf.edex.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Override certain C3P0ConnectionProvider functions to better manage
 * communication between edex services and postgres.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * Jan 16, 2020  8006     mrichardson  Initial creation
 *
 * </pre>
 *
 * @author mrichardson
 */

public class DatabaseC3P0ConnectionProvider extends C3P0ConnectionProvider {

    private static final Logger logger = LoggerFactory
            .getLogger(DatabaseC3P0ConnectionProvider.class);
    
    protected static final String CONNECTION_RETRY_LENGTH = "database.connection.retry.ms";

    private String url = "";

    @Override
    public Connection getConnection() {
        Connection c = null;
        boolean retry = true;
        int connectionRetryLength = Integer.parseInt(
                System.getProperty(CONNECTION_RETRY_LENGTH, "10000"));
        int sleepSeconds = connectionRetryLength / 1000;
        while (retry) {
            try {
                c = super.getConnection();
                retry = false;
            } catch (SQLException e) {
                logger.error("A connection could not be made to " + url + "; waiting " + sleepSeconds + " seconds to try again...");
                try {
                    Thread.sleep(connectionRetryLength);
                } catch (InterruptedException ie) {
                    // ignore...
                }
            }
        }
        return c;
    }

    @Override
    public void configure(Map props) {
        this.url = (String) props.get(Environment.URL);
        super.configure(props);
    }

}
