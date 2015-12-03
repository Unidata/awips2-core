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
package com.raytheon.uf.edex.localization.http.scheme;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.raytheon.uf.edex.localization.http.LocalizationHttpException;

/**
 * Implementation of Basic authorization scheme.
 * 
 * FIXME: At present ignores the password/key and only uses the username.
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

public class BasicScheme {

    public static String authenticate(String encoded)
            throws LocalizationHttpException {
        String decoded = new String(Base64.decodeBase64(encoded));
        int index = decoded.indexOf(":");
        if (index < 1) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Bad authorization header");
        }
        String username = decoded.substring(0, index);

        // TODO actually authenticate someday
        String password = decoded.substring(index + 1);

        return username;
    }

}
