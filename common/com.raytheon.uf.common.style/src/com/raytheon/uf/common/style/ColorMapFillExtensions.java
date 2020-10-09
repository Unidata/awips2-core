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

package com.raytheon.uf.common.style;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Contains the style preferences related to labeling
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------
 * Jul 29, 2019  65809    ksunil     initial creation
 * </pre>
 *
 * @author ksunil
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ColorMapFillExtensions {

    @XmlElement(name = "fill")
    private List<FillLabelingPreferences> fill = new ArrayList<>();

    @XmlAttribute
    private String name;

    public ColorMapFillExtensions() {
        super();
    }

    public ColorMapFillExtensions(ColorMapFillExtensions prefs) {

        this.name = prefs.name;
        Iterator<FillLabelingPreferences> iterator = prefs.getFill().iterator();
        while (iterator.hasNext()) {
            this.fill.add((FillLabelingPreferences) iterator.next().clone());
        }

    }

    public List<FillLabelingPreferences> getFill() {
        return fill;
    }

    public void setFill(List<FillLabelingPreferences> fill) {
        this.fill = fill;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ColorMapFillExtensions clone() {
        return new ColorMapFillExtensions(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fill == null) ? 0 : fill.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ColorMapFillExtensions other = (ColorMapFillExtensions) obj;
        if (fill == null) {
            if (other.fill != null)
                return false;
        } else if (!fill.equals(other.fill))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ColorMapExtensions [fill=" + fill + ", name=" + name + "]";
    }

}
