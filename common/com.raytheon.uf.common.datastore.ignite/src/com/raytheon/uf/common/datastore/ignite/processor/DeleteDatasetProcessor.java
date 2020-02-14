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
package com.raytheon.uf.common.datastore.ignite.processor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastore.ignite.DataStoreKey;
import com.raytheon.uf.common.datastore.ignite.DataStoreValue;

/**
 * 
 * Delete a specific dataset from a group. The arguments to this processor
 * should be the names of the datasets to delete.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jun 03, 2019  7628     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
public class DeleteDatasetProcessor
        implements EntryProcessor<DataStoreKey, DataStoreValue, Boolean> {

    @Override
    public Boolean process(MutableEntry<DataStoreKey, DataStoreValue> entry,
            Object... args) throws EntryProcessorException {
        if (!entry.exists()) {
            return Boolean.FALSE;
        }
        Set<String> namesToDelete = new HashSet<>();
        for (Object arg : args) {
            namesToDelete.add((String) arg);
        }
        IDataRecord[] oldRecords = entry.getValue().getRecords();
        List<IDataRecord> newRecords = new ArrayList<>(
                oldRecords.length - namesToDelete.size());
        for (IDataRecord record : oldRecords) {
            if (!namesToDelete.contains(record.getName())) {
                newRecords.add(record);
            }
        }
        if (newRecords.size() == oldRecords.length) {
            return Boolean.FALSE;
        } else if (newRecords.isEmpty()) {
            entry.remove();
        } else {
            entry.setValue(new DataStoreValue(newRecords));
        }
        return Boolean.TRUE;
    }

}
