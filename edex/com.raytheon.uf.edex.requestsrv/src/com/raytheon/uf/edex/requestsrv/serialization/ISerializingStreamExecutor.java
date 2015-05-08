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
package com.raytheon.uf.edex.requestsrv.serialization;

import java.io.InputStream;
import java.io.OutputStream;

import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * Interface for a stream-based {@link IServerRequest} executor that
 * deserializes from an {@link InputStream} into an {@link IServerRequest}
 * object and serializes the response of the execution to an
 * {@link OutputStream}
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 21, 2014 3541       mschenke    Initial creation
 * Jan 06, 2015 3789       bclement    added getContentType(), execute throws UnsupportedFormatException
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public interface ISerializingStreamExecutor {

    /**
     * @param format
     * @return
     * @throws UnsupportedFormatException
     */
    public String getContentType(String format)
            throws UnsupportedFormatException;

    /**
     * Deserializes the {@link IServerRequest} from the {@link InputStream}
     * passed in, executes the request and serializes the response out to the
     * {@link OutputStream} passed in
     * 
     * @param inputFormat
     * @param in
     * @param outputFormat
     * @param out
     * @throws UnsupportedFormatException
     *             most errors are returned to the output stream in the
     *             requested output format, however an exception is thrown if
     *             requested output format is unsupported
     */
    public void execute(String inputFormat, InputStream in,
            String outputFormat, OutputStream out)
            throws UnsupportedFormatException;

}
