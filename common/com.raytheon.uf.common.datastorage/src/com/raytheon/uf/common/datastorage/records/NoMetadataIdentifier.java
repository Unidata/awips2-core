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
import java.util.UUID;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.util.SystemUtil;

/**
 * Metadata identifier implementation that doesn't actually identify any
 * metadata. Reasons to use this include that the data may in fact have no
 * metadata that references it, or that the metadata can not be adequately
 * identified/tracked (e.g. the indices used by point data can't be easily
 * determined where this is used).
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
public class NoMetadataIdentifier implements IMetadataIdentifier {

    private static final long serialVersionUID = 1L;

    @DynamicSerializeElement
    private String traceId = String.join(":", SystemUtil.getClientID(),
            getClass().getSimpleName(), UUID.randomUUID().toString());

    @DynamicSerializeElement
    private boolean writeBehindSupported = true;

    @DynamicSerializeElement
    private boolean metadataUsed = false;

    public NoMetadataIdentifier() {
    }

    /**
     * Constructor
     *
     * @param writeBehindSupported
     *            true if the data that this identifies supports being written
     *            behind asynchronously instead of written through synchronously
     * @param metadataUsed
     *            true if the data that this identifies is in fact referenced by
     *            metadata and we just aren't tracking it, false if the data is
     *            not referenced by metadata
     */
    public NoMetadataIdentifier(boolean writeBehindSupported,
            boolean metadataUsed) {
        this.writeBehindSupported = writeBehindSupported;
        this.metadataUsed = metadataUsed;
    }

    @Override
    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public boolean isWriteBehindSupported() {
        return writeBehindSupported;
    }

    public void setWriteBehindSupported(boolean writeBehindSupported) {
        this.writeBehindSupported = writeBehindSupported;
    }

    @Override
    public boolean isMetadataUsed() {
        return metadataUsed;
    }

    public void setMetadataUsed(boolean metadataUsed) {
        this.metadataUsed = metadataUsed;
    }

    @Override
    public MetadataSpecificity getSpecificity() {
        return MetadataSpecificity.NONE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(writeBehindSupported, traceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (!equalsIgnoreTraceId(obj)) {
            return false;
        }
        NoMetadataIdentifier other = (NoMetadataIdentifier) obj;
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
        NoMetadataIdentifier other = (NoMetadataIdentifier) obj;
        return writeBehindSupported == other.writeBehindSupported;
    }

    @Override
    public String toString() {
        return "NoMetadataIdentifier [traceId=" + traceId
                + ", writeBehindSupported=" + writeBehindSupported + "]";
    }
}
