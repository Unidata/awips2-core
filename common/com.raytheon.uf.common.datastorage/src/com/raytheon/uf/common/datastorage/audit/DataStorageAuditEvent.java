/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastorage.audit;

import java.util.Arrays;
import java.util.Map;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Contains info about a data storage operation for sending to the auditer.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2021 8608       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
@DynamicSerialize
public class DataStorageAuditEvent {

    @DynamicSerializeElement
    private MetadataAndDataId[] dataIds;

    @DynamicSerializeElement
    private Map<String, MetadataStatus> metadataStatuses;

    @DynamicSerializeElement
    private Map<String, DataStatus> dataStatuses;

    public DataStorageAuditEvent() {
    }

    public MetadataAndDataId[] getDataIds() {
        return dataIds;
    }

    public void setDataIds(MetadataAndDataId[] dataIds) {
        this.dataIds = dataIds;
    }

    public Map<String, MetadataStatus> getMetadataStatuses() {
        return metadataStatuses;
    }

    public void setMetadataStatuses(
            Map<String, MetadataStatus> metadataStatuses) {
        this.metadataStatuses = metadataStatuses;
    }

    public Map<String, DataStatus> getDataStatuses() {
        return dataStatuses;
    }

    public void setDataStatuses(Map<String, DataStatus> dataStatuses) {
        this.dataStatuses = dataStatuses;
    }

    @Override
    public String toString() {
        return "DataStorageAuditEvent [dataIds=" + Arrays.toString(dataIds)
                + ", metadataStatuses=" + metadataStatuses + ", dataStatuses="
                + dataStatuses + "]";
    }

}
