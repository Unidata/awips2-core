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
package com.raytheon.uf.common.inventory.data;

import com.raytheon.uf.common.inventory.TimeAndSpace;

/**
 * {@link AbstractRequestableData} object for a single constant {@link Double}
 * value.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jul 17, 2017  6345     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class DoubleRequestableData extends AbstractRequestableData {

    private Double value;

    public DoubleRequestableData(Double value) {
        this.value = value;
        this.dataTime = TimeAndSpace.TIME_AGNOSTIC;
        this.space = TimeAndSpace.SPACE_AGNOSTIC;
    }

    public Double getDataValue() {
        return value;
    }

    @Override
    public Double getDataValue(Object arg) {
        return getDataValue();
    }

}
