/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.serialization.thrift.exception;

import org.apache.thrift.protocol.TMap;

/**
 * An exception indicating that a map could not be deserialized from the {code
 * SelfDescribingBinaryProtocol}.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 16, 2015  4561      njensen     Initial creation
 *
 * </pre>
 *
 * @author njensen
 * @version 1.0	
 */

public class MapDeserializationException extends
        CollectionDeserializationException {

    private static final long serialVersionUID = 1L;

    protected final TMap tmap;

    public MapDeserializationException(TMap tmap, int index) {
        this(tmap, index, null, null);
    }

    public MapDeserializationException(TMap tmap, int index, String msg) {
        this(tmap, index, msg, null);
    }

    public MapDeserializationException(TMap tmap, int index, Throwable t) {
        this(tmap, index, null, t);
    }

    public MapDeserializationException(TMap tmap, int index, String msg,
            Throwable t) {
        super(index, msg, t);
        this.tmap = tmap;
    }

    public TMap getTmap() {
        return tmap;
    }

}
