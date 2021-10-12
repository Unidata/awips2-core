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

import com.raytheon.uf.common.datastorage.records.IMetadataIdentifier;
import com.raytheon.uf.common.time.SimulatedTime;

/**
 * Contains info about a data storage operation.
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
public class DataStorageInfo {

    private final long startTime;

    private final String traceId;

    private IMetadataIdentifier metaId;

    private DataId dataId;

    private MetadataStatus metaStatus;

    private DataStatus dataStatus;

    /**
     * Constructor. Note that on instantiation, the current time is stored as
     * the data storage start time.
     *
     * @param traceId
     *            the trace ID that uniqely identifies this data storage route -
     *            there should be one per unique metadata identifier or unique
     *            data group (whichever is more specific) per data storage route
     */
    public DataStorageInfo(String traceId) {
        this.traceId = traceId;
        this.startTime = SimulatedTime.getSystemTime().getMillis();
    }

    /**
     * @return the start time of this storage route
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @return the trace ID uniquely identifying this data storage operation
     */
    public String getTraceId() {
        return traceId;
    }

    public IMetadataIdentifier getMetaId() {
        return metaId;
    }

    public void setMetaId(IMetadataIdentifier metaId) {
        this.metaId = metaId;
    }

    public DataId getDataId() {
        return dataId;
    }

    public void setDataId(DataId dataId) {
        this.dataId = dataId;
    }

    public MetadataStatus getMetaStatus() {
        return metaStatus;
    }

    public void setMetaStatus(MetadataStatus metaStatus) {
        this.metaStatus = metaStatus;
    }

    public DataStatus getDataStatus() {
        return dataStatus;
    }

    public void setDataStatus(DataStatus dataStatus) {
        this.dataStatus = dataStatus;
    }

    /**
     * Get whether or not all the pieces of the data storage operation have been
     * set on this info, indicating that the operation has completed. This
     * doesn't indicate whether it was successful or not.
     *
     * @return true if the data storage operation has completed, false otherwise
     */
    public boolean isComplete() {
        return metaId != null && dataId != null && metaStatus != null
                && dataStatus != null;
    }

    /**
     * @return true if only the data storage status has been reported for this
     *         operation, false otherwise
     */
    public boolean hasDataStatusOnly() {
        return metaId == null && dataId == null && metaStatus == null
                && dataStatus != null;
    }

    @Override
    public String toString() {
        return "DataStorageInfo [time=" + startTime + ", traceId=" + traceId
                + ", metaId=" + metaId + ", dataId=" + dataId + ", metaStatus="
                + metaStatus + ", dataStatus=" + dataStatus + "]";
    }
}