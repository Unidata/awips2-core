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
package com.raytheon.uf.common.dataaccess.request;

import com.raytheon.uf.common.dataaccess.response.GetGridDataResponse;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;
import org.locationtech.jts.geom.Envelope;

/**
 * 
 * Get lat lon values for a specific grid. The values used in the request can be
 * easily extracted from a {@link GetGridDataResponse}.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Oct 18, 2016  5916     bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
@DynamicSerialize
public class GetGridLatLonRequest implements IServerRequest {

    @DynamicSerializeElement
    private int nx;

    @DynamicSerializeElement
    private int ny;

    @DynamicSerializeElement
    private Envelope envelope;

    @DynamicSerializeElement
    private String crsWkt;

    public int getNx() {
        return nx;
    }

    public void setNx(int nx) {
        this.nx = nx;
    }

    public int getNy() {
        return ny;
    }

    public void setNy(int ny) {
        this.ny = ny;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }

    public String getCrsWkt() {
        return crsWkt;
    }

    public void setCrsWkt(String crsWkt) {
        this.crsWkt = crsWkt;
    }

}
