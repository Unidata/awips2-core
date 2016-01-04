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
package com.raytheon.uf.common.http.auth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic authorization scheme
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 2, 2015   4834      njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class BasicScheme extends AuthScheme {

    /**
     * matches 'Basic [credentials]'
     */
    private static final Pattern AUTH_HEADER_PATTERN = Pattern
            .compile("^Basic\\s+(\\S.+)$");

    /**
     * matches '[username]:[password]'
     */
    private static final Pattern AUTH_PARAMETER_PATTERN = Pattern
            .compile("^(\\S+?):(.+)$");

    /**
     * Parse an Authorization header in the form
     * 
     * <pre>
     * 'Basic [encoded]'
     * </pre>
     * 
     * @param authHeader
     * @return
     */
    public static BasicCredential parseAuthHeader(String authHeader) {
        Matcher m = AUTH_HEADER_PATTERN.matcher(authHeader.trim());
        if (!m.matches()) {
            return null;
        }

        String encoded = m.group(1).trim();
        String decoded = new String(base64Decode(encoded));
        m = AUTH_PARAMETER_PATTERN.matcher(decoded);
        if (!m.matches()) {
            return null;
        }

        String username = m.group(1);
        String password = m.group(2);
        return new BasicCredential(username, password);
    }

}
