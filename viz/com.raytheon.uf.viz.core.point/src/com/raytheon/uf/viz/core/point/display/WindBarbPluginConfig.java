package com.raytheon.uf.viz.core.point.display;

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

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * POJO container of WindBarbPluginConfig configuration.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 13, 2018 #57905      E. Debebe   Initial creation
 *
 * </pre>
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class WindBarbPluginConfig {

    // XmLElementWrapper generates a wrapper element around XML representation
    @XmlElementWrapper(name = "windBarbPluginList")
    // XmlElement sets the name of the entities
    @XmlElement(name = "windBarbPlugin")
    private List<WindBarbPlugin> windBarbPluginList;

    /**
     * @return the windBarbPluginList
     */
    public List<WindBarbPlugin> getWindBarbPluginList() {
        return windBarbPluginList;
    }

    /**
     * @param windBarbPluginList
     *            the windBarbPluginList to set
     */
    public void setWindBarbPluginList(List<WindBarbPlugin> windBarbPluginList) {
        this.windBarbPluginList = windBarbPluginList;
    }

}
