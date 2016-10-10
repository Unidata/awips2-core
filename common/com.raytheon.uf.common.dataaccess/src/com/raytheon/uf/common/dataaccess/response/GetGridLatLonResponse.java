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
package com.raytheon.uf.common.dataaccess.response;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * 
 * Returns lat and lon values for a specific grid.
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
public class GetGridLatLonResponse {

    @DynamicSerializeElement
    private int nx;

    @DynamicSerializeElement
    private int ny;

    @DynamicSerializeElement
    private float[] lats;

    @DynamicSerializeElement
    private float[] lons;

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

    public float[] getLats() {
        return lats;
    }

    public void setLats(float[] lats) {
        this.lats = lats;
    }

    public float[] getLons() {
        return lons;
    }

    public void setLons(float[] lons) {
        this.lons = lons;
    }

}
