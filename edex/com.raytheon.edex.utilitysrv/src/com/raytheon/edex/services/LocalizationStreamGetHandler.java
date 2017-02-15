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
package com.raytheon.edex.services;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.localization.stream.LocalizationStreamGetRequest;
import com.raytheon.uf.common.localization.stream.LocalizationStreamPutRequest;

/**
 * A request handler for LocalizationStreamGetRequests. Supports sending chunks
 * of files.
 * 
 * @deprecated Continues to exist to support older clients. Newer clients should
 *             use the Localization REST service.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 14, 2017 6111       njensen     Extracted from LocalizationStreamHandler
 *
 * </pre>
 *
 * @author njensen
 */

@Deprecated
public class LocalizationStreamGetHandler
        extends LocalizationStreamHandler<LocalizationStreamGetRequest> {

    @Override
    public Object handleRequest(LocalizationStreamGetRequest request)
            throws Exception {
        File file = super.validate(request);

        return handleStreamingGet(request, file);
    }

    private Object handleStreamingGet(LocalizationStreamGetRequest request,
            File file) throws Exception {
        // TODO: Copy file to tmp location named from request unique id and
        // stream that file for the request to avoid put/delete/read issues

        try (FileInputStream inputStream = new FileInputStream(file)) {
            int bytesSkipped = 0;
            int toSkip = request.getOffset();

            // we are done if we the toSkip is 0, or toSkip increased in size
            while ((toSkip != 0) && (toSkip <= request.getOffset())) {
                bytesSkipped += inputStream.skip(toSkip);
                toSkip = request.getOffset() - bytesSkipped;
            }

            if (toSkip != 0) {
                throw new LocalizationException("Error skipping through file");
            }

            LocalizationStreamPutRequest response = new LocalizationStreamPutRequest();
            byte[] bytes = new byte[request.getNumBytes()];
            int bytesRead = inputStream.read(bytes, 0, request.getNumBytes());

            if (bytesRead == -1) {
                response.setBytes(new byte[0]);
                response.setEnd(true);
            } else {
                if (bytesRead != bytes.length) {
                    bytes = Arrays.copyOf(bytes, bytesRead);
                }

                response.setBytes(bytes);
                response.setEnd(
                        request.getOffset() + bytesRead == file.length());
            }
            return response;
        }
    }
    
    @Override
    public Class<?> getRequestType() {
        return LocalizationStreamGetRequest.class;
    }

}
