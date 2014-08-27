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

import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.common.util.stream.LimitingInputStream;
import com.raytheon.uf.common.util.stream.LimitingOutputStream;

/**
 * Serializing stream executor that limits the amount of data that can be read
 * in from the request {@link InputStream} as well as written out to the
 * response {@link OutputStream}
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 21, 2014 3541       mschenke    Initial creation
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class ByteLimitingStreamExecutor implements ISerializingStreamExecutor {

    private final int byteLimitInMB;

    private final ISerializingStreamExecutor executor;

    private boolean limitInput = true;

    private boolean limitOutput = true;

    public ByteLimitingStreamExecutor(ISerializingStreamExecutor executor,
            int byteLimitInMB) {
        this.executor = executor;
        this.byteLimitInMB = byteLimitInMB;
    }

    @Override
    public void execute(String inputFormat, InputStream in,
            String outputFormat, OutputStream out) {
        // wrap and pass on
        if (isLimitInput()) {
            in = new LimitingInputStream(in, getByteLimit());
        }
        if (isLimitOutput()) {
            out = new LimitingOutputStream(out, getByteLimit());
        }
        executor.execute(inputFormat, in, outputFormat, out);
    }

    /**
     * @return the service object size limit in bytes
     */
    public long getByteLimit() {
        return byteLimitInMB * SizeUtil.BYTES_PER_MB;
    }

    /**
     * @return the service object size limit in megabytes
     */
    public int getByteLimitInMB() {
        return byteLimitInMB;
    }

    public boolean isLimitInput() {
        return limitInput;
    }

    public void setLimitInput(boolean limitInput) {
        this.limitInput = limitInput;
    }

    public boolean isLimitOutput() {
        return limitOutput;
    }

    public void setLimitOutput(boolean limitOutput) {
        this.limitOutput = limitOutput;
    }

}
