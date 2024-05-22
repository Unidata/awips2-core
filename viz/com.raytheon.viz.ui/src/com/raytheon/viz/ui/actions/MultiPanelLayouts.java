/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.viz.ui.actions;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Contains a list of available editor panel layouts.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 31, 2023 2029803    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
@XmlRootElement
@XmlAccessorType(value = XmlAccessType.NONE)
public class MultiPanelLayouts {

    @XmlElement(name = "layout")
    private List<MultiPanelLayout> layouts;

    public List<MultiPanelLayout> getLayouts() {
        return layouts;
    }

    public void setLayouts(List<MultiPanelLayout> layouts) {
        this.layouts = layouts;
    }
}
