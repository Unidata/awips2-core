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
package com.raytheon.uf.common.dataplugin.message;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * A message that contains a set of DataURIs that have been updated
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 2, 2008             chammack    Initial creation
 * Feb 15, 2013 1638       mschenke    Moved from com.raytheon.edex.common project
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */
@DynamicSerialize
public class PracticeDataURINotificationMessage implements ISerializableObject {

    @DynamicSerializeElement
    private String[] dataURIs;

    /**
     * @return the dataURIs
     */
    public String[] getDataURIs() {
        return dataURIs;
    }

    /**
     * @param dataURIs
     *            the dataURIs to set
     */
    public void setDataURIs(String[] dataURIs) {
        this.dataURIs = dataURIs;
    }

}
