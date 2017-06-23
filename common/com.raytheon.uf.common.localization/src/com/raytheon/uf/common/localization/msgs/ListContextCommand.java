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
 * Command to list the available localization context names for the specified
 * level
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 02, 2010            mpduff      Initial creation
 * Oct 01, 2013  2361      njensen     Removed XML annotations
 * Jun 22, 2017  6339      njensen     Overrode toString()
 * 
 * </pre>
 * 
 * @author mpduff
 */

@DynamicSerialize
public class ListContextCommand extends AbstractUtilityCommand {

    @DynamicSerializeElement
    private LocalizationLevel requestLevel;

    /**
     * Constructor
     */
    public ListContextCommand() {
    }

    public LocalizationLevel getRequestLevel() {
        return requestLevel;
    }

    public void setRequestLevel(LocalizationLevel requestLevel) {
        this.requestLevel = requestLevel;
    }

    @Override
    public String toString() {
        return "ListContextCommand [requestLevel=" + requestLevel + ", context="
                + context + "]";
    }

}
