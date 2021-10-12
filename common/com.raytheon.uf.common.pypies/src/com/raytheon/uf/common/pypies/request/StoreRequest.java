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
package com.raytheon.uf.common.pypies.request;

import java.util.List;

import com.raytheon.uf.common.datastorage.IDataStore.StoreOp;
import com.raytheon.uf.common.datastorage.records.RecordAndMetadata;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * TODO Add Description
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 27, 2010            njensen     Initial creation
 * Sep 23, 2021 8608       mapeters    Add {@link #getType()}, handle metadata ids
 *
 * </pre>
 *
 * @author njensen
 */
@DynamicSerialize
public class StoreRequest extends AbstractRequest {

    @DynamicSerializeElement
    private StoreOp op;

    @DynamicSerializeElement
    private List<RecordAndMetadata> recordsAndMetadata;

    public StoreOp getOp() {
        return op;
    }

    public void setOp(StoreOp op) {
        this.op = op;
    }

    public List<RecordAndMetadata> getRecordsAndMetadata() {
        return recordsAndMetadata;
    }

    public void setRecordsAndMetadata(
            List<RecordAndMetadata> recordsAndMetadata) {
        this.recordsAndMetadata = recordsAndMetadata;
    }

    @Override
    public RequestType getType() {
        return RequestType.STORE;
    }
}
