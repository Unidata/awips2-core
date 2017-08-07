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
package com.raytheon.uf.common.localization.msgs;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * A response for protected files.
 * 
 * @deprecated Use ProtectedFileLookup instead.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 26, 2010            mschenke    Initial creation
 * Oct 01, 2013  2361      njensen     Removed XML annotations
 * Aug 04, 2017  6379      njensen     Deprecated
 * 
 * </pre>
 * 
 * @author mschenke
 */

@Deprecated
@DynamicSerialize
public class ProtectedFileResponse extends AbstractUtilityResponse {

    @DynamicSerializeElement
    private LocalizationLevel protectedLevel;

    /**
     * @return the protectedLevel
     */
    public LocalizationLevel getProtectedLevel() {
        return protectedLevel;
    }

    /**
     * @param protectedLevel
     *            the protectedLevel to set
     */
    public void setProtectedLevel(LocalizationLevel protectedLevel) {
        this.protectedLevel = protectedLevel;
    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.raytheon.uf.common.localization.msgs.AbstractUtilityResponse#
     * getFormattedErrorMessage()
     */
    @Override
    public String getFormattedErrorMessage() {
        return "Error";
    }

}
