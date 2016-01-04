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

import org.apache.commons.codec.binary.Base64;

/**
 * Abstract base class for http authorization schemes
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 9, 2015   4834      njensen     Initial creation, extracted from SignatureAuthScheme
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public abstract class AuthScheme {

    /**
     * Create a non-chunked (no newlines) base64 string from bytes
     * 
     * @param bytes
     * @return
     */
    public static final String base64Encode(byte[] bytes) {
        bytes = Base64.encodeBase64(bytes, false);
        return org.apache.commons.codec.binary.StringUtils.newStringUtf8(bytes);
    }

    /**
     * Decode a base64 encoded string
     * 
     * @param encString
     * @return
     */
    public static final byte[] base64Decode(String encString) {
        return Base64.decodeBase64(encString);
    }

}
