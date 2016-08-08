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
package com.raytheon.uf.common.dataaccess;

import com.raytheon.uf.common.dataaccess.exception.TimeAgnosticDataException;

/**
 * The Data Notification Layer is the published API for getting data through the
 * Data Access Framework as it comes. Currently, this is only supported for
 * Python clients. All methods may potentially throw
 * UnsupportedOperationException or IllegalArgumentException dependent on how
 * much support has been provided per datatype.
 * 
 * The implementation of this class is a retrieval of the corresponding factory
 * and then delegating the processing to that factory.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 3, 2016  2416      rjpeter     Initial creation
 * 
 * </pre>
 * 
 * @author rjpeter
 */

public class DataNotificationLayer {

    private DataNotificationLayer() {
        // static interface only
    }

    /**
     * Gets the notification filter to be applied to a set of data URIs
     * 
     * @param request
     *            the request to find available times for
     * @return the available times that match the request
     * @throws TimeAgnosticDataException
     */
    public static INotificationFilter getNotificationFilter(IDataRequest request) {
        IDataFactory factory = DataAccessLayer.getFactory(request);
        return factory.getNotificationFilter(request);
    }

}
