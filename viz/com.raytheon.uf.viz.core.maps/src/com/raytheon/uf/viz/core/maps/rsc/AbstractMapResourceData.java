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
package com.raytheon.uf.viz.core.maps.rsc;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.raytheon.uf.viz.core.rsc.AbstractResourceData;

/**
 * Resource data for a map
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 30, 2018 6562       tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractMapResourceData extends AbstractResourceData {

    @XmlElement
    protected String mapName = null;

    @XmlElementWrapper(name = "unloadList")
    @XmlElement(name = "unload")
    private List<String> unloadList = new ArrayList<>();

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    /** @return list of maps to unload when this map is loaded. */
    public List<String> getUnloadList() {
        return unloadList;
    }

    /**
     * @param unloadList
     *            list of maps to unload when this map is loaded.
     */
    public void setUnloadList(List<String> unloadList) {
        this.unloadList = unloadList;
    }

}
