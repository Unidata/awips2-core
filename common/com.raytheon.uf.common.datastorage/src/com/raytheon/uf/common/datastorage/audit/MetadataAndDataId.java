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
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Container class for metadata and data identifiers.
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
public class MetadataAndDataId {

    @DynamicSerializeElement
    private IMetadataIdentifier metaId;

    @DynamicSerializeElement
    private DataId dataId;

    /**
     * Default constructor for serialization.
     */
    public MetadataAndDataId() {
    }

    public MetadataAndDataId(IMetadataIdentifier metaId, DataId dataId) {
        this.metaId = metaId;
        this.dataId = dataId;
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

    @Override
    public String toString() {
        return "DataIds [metaId=" + metaId + ", dataId=" + dataId + "]";
    }
}