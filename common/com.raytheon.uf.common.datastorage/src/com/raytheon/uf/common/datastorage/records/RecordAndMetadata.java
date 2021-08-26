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
package com.raytheon.uf.common.datastorage.records;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Container class for a data record and its metadata identifier.
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
public class RecordAndMetadata {

    @DynamicSerializeElement
    private IDataRecord record;

    /**
     * Identifies the metadata entry that references the data record. Note that
     * generally there is only one metadata identifier per record, but an
     * example of where there may be multiple is with partial data. Each partial
     * data piece has its own metadata identifier that all match except for
     * trace ID, and once the data pieces all get merged into one record, you
     * have multiple metadata identifiers for that one record.
     */
    @DynamicSerializeElement
    private Set<IMetadataIdentifier> metadata = new HashSet<>();

    /**
     * Default constructor for serialization.
     */
    public RecordAndMetadata() {
    }

    public RecordAndMetadata(IDataRecord record,
            Collection<IMetadataIdentifier> metadata) {
        this.record = record;
        if (metadata != null) {
            this.metadata.addAll(metadata);
        }
    }

    public IDataRecord getRecord() {
        return record;
    }

    public void setRecord(IDataRecord record) {
        this.record = record;
    }

    public Set<IMetadataIdentifier> getMetadata() {
        return Collections.unmodifiableSet(metadata);
    }

    public void setMetadata(Set<IMetadataIdentifier> metadata) {
        if (this.metadata == null) {
            this.metadata = new HashSet<>();
        }
        if (metadata != null) {
            this.metadata.addAll(metadata);
        }
    }

    public void addMetadata(IMetadataIdentifier metaId) {
        if (this.metadata == null) {
            this.metadata = new HashSet<>();
        }
        this.metadata.add(metaId);
    }

    @Override
    public String toString() {
        return "RecordAndMetadata [record=" + record + ", metadata=" + metadata
                + "]";
    }
}