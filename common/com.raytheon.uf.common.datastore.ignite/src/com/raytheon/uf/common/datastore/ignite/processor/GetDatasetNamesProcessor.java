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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import com.raytheon.uf.common.datastorage.records.RecordAndMetadata;
import com.raytheon.uf.common.datastore.ignite.DataStoreKey;
import com.raytheon.uf.common.datastore.ignite.DataStoreValue;

/**
 *
 * Get the names of the datasets in a group.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jun 03, 2019  7628     bsteffen  Initial creation
 * Sep 23, 2021  8608     mapeters  Add metadata id handling
 *
 * </pre>
 *
 * @author bsteffen
 */
public class GetDatasetNamesProcessor
        implements EntryProcessor<DataStoreKey, DataStoreValue, Set<String>> {

    @Override
    public Set<String> process(MutableEntry<DataStoreKey, DataStoreValue> entry,
            Object... args) throws EntryProcessorException {
        if (!entry.exists()) {
            return Collections.emptySet();
        }
        RecordAndMetadata[] rms = entry.getValue().getRecordsAndMetadata();
        Set<String> names = new HashSet<>();
        for (RecordAndMetadata rm : rms) {
            names.add(rm.getRecord().getName());
        }
        return names;
    }

}
