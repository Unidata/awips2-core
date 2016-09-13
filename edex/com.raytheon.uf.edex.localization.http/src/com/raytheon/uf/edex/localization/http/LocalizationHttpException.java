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
package com.raytheon.uf.edex.localization.http;

import java.util.Map;

/**
 * Exception encapsulating an HTTP error. Used only to send error messages to
 * client.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 15, 2015 3978       bclement     Initial creation
 * Dec 04, 2015 4834       njensen      Added headers map
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class LocalizationHttpException extends Exception {

    private static final long serialVersionUID = 6071078985378997494L;

    private final int errorCode;

    /** additional http headers to include with a response */
    private Map<String, String> headers;

    /**
     * @param message
     */
    public LocalizationHttpException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LocalizationHttpException(int errorCode, String message,
            Map<String, String> additionalHeaders) {
        this(errorCode, message);
        this.headers = additionalHeaders;
    }

    /**
     * @return the errorCode
     */
    public int getErrorCode() {
        return errorCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

}
