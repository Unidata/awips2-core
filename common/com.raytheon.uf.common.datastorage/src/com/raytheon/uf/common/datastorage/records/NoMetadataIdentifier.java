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
 * metadata. Used to indicate that the data being stored is not referenced by
 * any metadata.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2021 8608       mapeters    Initial creation
 * Feb 17, 2022 8608       mapeters    Updates to fix data storage audit errors
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
            Thread.currentThread().getName(), getClass().getSimpleName(),
            UUID.randomUUID().toString());

    public NoMetadataIdentifier() {
    }

    @Override
    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public MetadataSpecificity getSpecificity() {
        return MetadataSpecificity.NO_METADATA;
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId);
    }

    @Override
    public int hashCodeIgnoreTraceId() {
        return NoMetadataIdentifier.class.hashCode();
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
        return true;
    }

    @Override
    public String toString() {
        return "NoMetadataIdentifier [traceId=" + traceId + "]";
    }
}
