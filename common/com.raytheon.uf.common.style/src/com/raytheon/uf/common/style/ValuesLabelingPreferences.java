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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Contains the style preferences related to labeling
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------
 * May 07, 2019  64008    ksunil  Initial Creation.
 *
 * </pre>
 *
 * @author ksunil
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ValuesLabelingPreferences
        extends BaseLabelingPreferences {

    public ValuesLabelingPreferences() {
        super();
    }

    public ValuesLabelingPreferences(
            ValuesLabelingPreferences prefs) {
        if (prefs.values != null) {
            this.values = new float[prefs.values.length];
            System.arraycopy(prefs.values, 0, this.values, 0,
                    this.values.length);
        }
        this.thickness = prefs.thickness;
        this.lineStyle = prefs.lineStyle;
        this.color = prefs.color;
    }

    @Override
    public ValuesLabelingPreferences clone() {
        return new ValuesLabelingPreferences(this);
    }

}
