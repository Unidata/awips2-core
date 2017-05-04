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

package com.raytheon.uf.common.style.graph;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.style.AbstractStylePreferences;
import com.raytheon.uf.common.style.LabelingPreferences;

/**
 * Preferences describing how data should be displayed on an x/y graph.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Oct 03, 2007           njensen   Initial creation
 * Apr 26, 2017  6247     bsteffen  Implement clone
 * 
 * </pre>
 * 
 * @author njensen
 */
@XmlRootElement(name = "graphStyle")
@XmlAccessorType(XmlAccessType.NONE)
public class GraphPreferences extends AbstractStylePreferences {

    @XmlElement(name = "range")
    private AxisScale axisScale;

    /**
     * Dotted lines are drawn at these values if they're within the range on the
     * axis
     */
    @XmlElement
    private LabelingPreferences dottedLines;

    public GraphPreferences() {

    }

    public GraphPreferences(GraphPreferences prefs) {
        super(prefs);
        this.axisScale = prefs.getAxisScale();
        this.dottedLines = prefs.getDottedLines().clone();
    }

    public AxisScale getAxisScale() {
        return axisScale;
    }

    public void setAxisScale(AxisScale axisScale) {
        this.axisScale = axisScale;
    }

    public LabelingPreferences getDottedLines() {
        return dottedLines;
    }

    public void setDottedLines(LabelingPreferences dottedLines) {
        this.dottedLines = dottedLines;
    }

    @Override
    public GraphPreferences clone() {
        return new GraphPreferences(this);
    }

}
