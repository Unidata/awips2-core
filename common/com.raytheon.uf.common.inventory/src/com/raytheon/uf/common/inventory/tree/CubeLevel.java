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
package com.raytheon.uf.common.inventory.tree;

/**
 * A pair set of items for a cube. Always represented as a parameter and a
 * pressure.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------------
 * Apr 08, 2010  4473     rjpeter   Initial creation
 * Apr 29, 2016  5439     bsteffen  Implement hashCode and equals
 * 
 * </pre>
 * 
 * @author rjpeter
 */
public class CubeLevel<K, V> {
    private K pressure;

    private V param;

    public CubeLevel() {
    }

    public CubeLevel(K pressure, V param) {
        this.param = param;
        this.pressure = pressure;
    }

    public K getPressure() {
        return pressure;
    }

    public void setPressure(K pressure) {
        this.pressure = pressure;
    }

    public V getParam() {
        return param;
    }

    public void setParam(V param) {
        this.param = param;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((param == null) ? 0 : param.hashCode());
        result = prime * result
                + ((pressure == null) ? 0 : pressure.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CubeLevel other = (CubeLevel) obj;
        if (param == null) {
            if (other.param != null) {
                return false;
            }
        } else if (!param.equals(other.param)) {
            return false;
        }
        if (pressure == null) {
            if (other.pressure != null) {
                return false;
            }
        } else if (!pressure.equals(other.pressure)) {
            return false;
        }
        return true;
    }

}
