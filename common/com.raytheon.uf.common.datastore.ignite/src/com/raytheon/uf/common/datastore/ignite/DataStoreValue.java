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
package com.raytheon.uf.common.datastore.ignite;

import java.util.Arrays;
import java.util.Collection;

import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.RecordAndMetadata;

/**
 * A simple object for holding an array of {@link IDataRecord}s.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 20, 2019  7628     bsteffen  Initial creation
 * Sep 23, 2021  8608     mapeters  Add metadata id handling
 * Jan 25, 2022  8608     mapeters  Add lastAppendRecordsAndMetadata
 *
 * </pre>
 *
 * @author bsteffen
 */
public class DataStoreValue {

    /**
     * All the records and metadata for this data store value.
     */
    private RecordAndMetadata[] recordsAndMetadata;

    /**
     * The records and metadata that were added to recordsAndMetadata by just
     * the last append operation. This is only used to help performance by
     * sending the last append operation through to pypies as an append
     * operation, instead of a replace operation
     */
    private RecordAndMetadata[] lastAppendRecordsAndMetadata;

    public DataStoreValue() {
    }

    public DataStoreValue(RecordAndMetadata[] records) {
        this.recordsAndMetadata = records;
    }

    public DataStoreValue(Collection<RecordAndMetadata> values) {
        this(values.toArray(new RecordAndMetadata[0]));
    }

    public RecordAndMetadata[] getRecordsAndMetadata() {
        return recordsAndMetadata;
    }

    public void setRecordsAndMetadata(RecordAndMetadata[] records) {
        this.recordsAndMetadata = records;
    }

    public RecordAndMetadata[] getLastAppendRecordsAndMetadata() {
        return lastAppendRecordsAndMetadata;
    }

    public void setLastAppendRecordsAndMetadata(
            RecordAndMetadata[] lastAppendRecordAndMetadata) {
        this.lastAppendRecordsAndMetadata = lastAppendRecordAndMetadata;
    }

    public static DataStoreValue createWithoutMetadata(
            Collection<IDataRecord> records) {
        return createWithoutMetadata(records.toArray(new IDataRecord[0]));
    }

    public static DataStoreValue createWithoutMetadata(IDataRecord[] records) {
        return new DataStoreValue(
                Arrays.stream(records).map(r -> new RecordAndMetadata(r, null))
                        .toArray(RecordAndMetadata[]::new));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(lastAppendRecordsAndMetadata);
        result = prime * result + Arrays.hashCode(recordsAndMetadata);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DataStoreValue other = (DataStoreValue) obj;
        return Arrays.equals(lastAppendRecordsAndMetadata,
                other.lastAppendRecordsAndMetadata)
                && Arrays.equals(recordsAndMetadata, other.recordsAndMetadata);
    }

    @Override
    public String toString() {
        return "DataStoreValue [recordsAndMetadata="
                + Arrays.toString(recordsAndMetadata)
                + ", lastAppendRecordsAndMetadata="
                + Arrays.toString(lastAppendRecordsAndMetadata) + "]";
    }
}
