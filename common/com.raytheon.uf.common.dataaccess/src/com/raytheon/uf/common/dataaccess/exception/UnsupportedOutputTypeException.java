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
package com.raytheon.uf.common.dataaccess.exception;

/**
 * An exception indicating that the datatype factory does not support the output
 * type, i.e. IGridData or IGeometryData.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 15, 2013 1614       bsteffen    Initial creation
 * Jul 14, 2014 3184       njensen     Added javadoc
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */

public class UnsupportedOutputTypeException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    private final String dataType;

    private final String outputType;

    public UnsupportedOutputTypeException(String dataType, String outputType) {
        super(dataType + " does not support " + outputType + " data");
        this.dataType = dataType;
        this.outputType = outputType;
    }

    /**
     * @return the dataType
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * @return the outputType
     */
    public String getOutputType() {
        return outputType;
    }
    
}
