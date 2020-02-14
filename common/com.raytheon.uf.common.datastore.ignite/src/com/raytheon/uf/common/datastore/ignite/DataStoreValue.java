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
 *
 * </pre>
 *
 * @author bsteffen
 */
public class DataStoreValue {

    private IDataRecord[] records;

    public DataStoreValue() {

    }

    public DataStoreValue(IDataRecord[] records) {
        this.records = records;
    }

    public DataStoreValue(Collection<IDataRecord> values) {
        this(values.toArray(new IDataRecord[0]));
    }

    public IDataRecord[] getRecords() {
        return records;
    }

    public void setRecords(IDataRecord[] records) {
        this.records = records;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(records);
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
        if (!Arrays.equals(records, other.records)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(getClass().getSimpleName());
        s.append(" [records={");
        boolean first = true;
        for (IDataRecord record : records) {
            if (first) {
                first = false;
            } else {
                s.append(", ");
            }
            s.append(record.getClass().getSimpleName());
            s.append(" [group=" + record.getGroup());
            s.append(" , name=" + record.getName());
            s.append(" , sizes=" + Arrays.toString(record.getSizes()));
            s.append("}");
        }
        s.append("}]");
        return s.toString();
    }

}
