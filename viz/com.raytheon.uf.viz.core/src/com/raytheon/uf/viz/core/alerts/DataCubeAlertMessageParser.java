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
package com.raytheon.uf.viz.core.alerts;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractRequestableResourceData;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged.ChangeType;
import com.raytheon.uf.viz.core.rsc.IUpdateHandlingResourceData;

/**
 *
 * A class for parsing alerts which retrieves the data using the data cube,
 * which makes it work well for anything which may have derived parameters
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 16, 2009            bsteffen     Initial creation
 * Sep 02, 2021 8651       njensen      Added IUpdateHandlingResourceData condition
 *
 * </pre>
 *
 * @author bsteffen
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
public class DataCubeAlertMessageParser extends AbstractAlertMessageParser {

    public DataCubeAlertMessageParser() {

    }

    @Override
    public Object parseAlertMessage(AlertMessage message,
            AbstractRequestableResourceData reqResourceData)
            throws VizException {
        if (reqResourceData instanceof IUpdateHandlingResourceData) {
            ((IUpdateHandlingResourceData) reqResourceData)
                    .handleUpdate(message);
        } else {
            reqResourceData.fireChangeListeners(ChangeType.DATA_REMOVE,
                    message.decodedAlert.get("dataTime"));
        }
        return null;
    }

}
