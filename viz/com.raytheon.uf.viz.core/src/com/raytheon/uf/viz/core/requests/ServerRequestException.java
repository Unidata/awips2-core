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
package com.raytheon.uf.viz.core.requests;

import com.raytheon.uf.viz.core.exception.VizException;

/**
 * Exception type for when exceptions occur during a thrift request
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 26, 2010            mschenke     Initial creation
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class ServerRequestException extends VizException {

    private static final long serialVersionUID = -5122006766636378142L;

    /**
     * 
     */
    public ServerRequestException() {
        super();
    }

    /**
     * @param message
     * @param cause
     */
    public ServerRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public ServerRequestException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public ServerRequestException(Throwable cause) {
        super(cause);
    }

}
