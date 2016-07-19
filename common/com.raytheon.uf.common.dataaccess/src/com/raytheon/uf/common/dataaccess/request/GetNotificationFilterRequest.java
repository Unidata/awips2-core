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

import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

/**
 * Request to get a filter to listen to updates as data comes in.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 03, 2016 2416       rjpeter     Initial creation
 * 
 * </pre>
 * 
 * @author rjpeter
 */

@DynamicSerialize
public class GetNotificationFilterRequest extends AbstractDataAccessRequest {

    public GetNotificationFilterRequest() {
        super();
    }

    public GetNotificationFilterRequest(final IDataRequest request) {
        super(request);
    }

}
