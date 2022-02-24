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

import java.util.Objects;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Identifies an entry in the metadata database by its data URI.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2021 8608       mapeters    Initial creation
 * Feb 17, 2022 8608       mapeters    Remove isMetadataUsed() and isWriteBehindSupported(),
 *                                     update constructors, add hashCodeIgnoreTraceId()
 *
 * </pre>
 *
 * @author mapeters
 */
@DynamicSerialize
public class DataUriMetadataIdentifier implements IMetadataIdentifier {

    private static final long serialVersionUID = 1L;

    @DynamicSerializeElement
    private String traceId;

    @DynamicSerializeElement
    private String dataUri;

    @DynamicSerializeElement
    private MetadataSpecificity specificity;

    /**
     * Default constructor for serialization.
     */
    public DataUriMetadataIdentifier() {
    }

    public DataUriMetadataIdentifier(String dataUri, String traceId) {
        this(dataUri, traceId, MetadataSpecificity.GROUP);
    }

    public DataUriMetadataIdentifier(String dataUri, String traceId,
            MetadataSpecificity specificity) {
        this.dataUri = dataUri;
        this.traceId = traceId;
        this.specificity = specificity;
    }

    public DataUriMetadataIdentifier(PluginDataObject pdo) {
        this(pdo.getDataURI(), pdo.getTraceId());
    }

    public DataUriMetadataIdentifier(PluginDataObject pdo,
            MetadataSpecificity specificity) {
        this(pdo.getDataURI(), pdo.getTraceId(), specificity);
    }

    public String getDataUri() {
        return dataUri;
    }

    public void setDataUri(String dataUri) {
        this.dataUri = dataUri;
    }

    @Override
    public MetadataSpecificity getSpecificity() {
        return specificity;
    }

    public void setSpecificity(MetadataSpecificity specificity) {
        this.specificity = specificity;
    }

    @Override
    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataUri, specificity, traceId);
    }

    @Override
    public int hashCodeIgnoreTraceId() {
        return Objects.hash(dataUri, specificity);
    }

    @Override
    public boolean equals(Object obj) {
        if (!equalsIgnoreTraceId(obj)) {
            return false;
        }
        DataUriMetadataIdentifier other = (DataUriMetadataIdentifier) obj;
        return Objects.equals(traceId, other.traceId);
    }

    @Override
    public boolean equalsIgnoreTraceId(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DataUriMetadataIdentifier other = (DataUriMetadataIdentifier) obj;
        return Objects.equals(dataUri, other.dataUri)
                && specificity == other.specificity;
    }

    @Override
    public String toString() {
        return "DataUriMetadataIdentifier [traceId=" + traceId + ", dataUri="
                + dataUri + ", specificity=" + specificity + "]";
    }
}
