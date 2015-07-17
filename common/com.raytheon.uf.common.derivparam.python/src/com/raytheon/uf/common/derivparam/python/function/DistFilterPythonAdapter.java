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
package com.raytheon.uf.common.derivparam.python.function;

import jep.NDArray;

/**
 * Calls {@link com.raytheon.uf.common.wxmath.DistFilter} and transforms the
 * output into an NDArray.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 13, 2013            njensen     Initial creation
 * Apr 22, 2015  4259      njensen     Updated for new JEP API
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class DistFilterPythonAdapter {

    public static NDArray<float[]> filter(float[] input, float npts, int nx,
            int ny, int times) {
        float[] result = com.raytheon.uf.common.wxmath.DistFilter.filter(input,
                npts, nx, ny, times);
        // FIXME we shouldn't have to reverse nx and ny!
        return new NDArray<float[]>(result, ny, nx);
    }

}
