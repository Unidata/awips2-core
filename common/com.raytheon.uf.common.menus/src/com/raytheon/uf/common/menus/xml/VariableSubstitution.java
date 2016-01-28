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
package com.raytheon.uf.common.menus.xml;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.raytheon.uf.common.util.VariableSubstitutor;

/**
 * 
 * Describes a set of variable substitutions
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ---------------------------
 * Mar 12, 2009  2214     chammack  Initial creation
 * Jun 09, 2014  3266     njensen   Remove ISerializableObject
 * Jan 28, 2016  5294     bsteffen  add substituteAndCombine
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
public class VariableSubstitution {

    @XmlAttribute(required = true)
    public String key;

    @XmlAttribute(required = true)
    public String value;

    public VariableSubstitution() {

    }

    public VariableSubstitution(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static VariableSubstitution[] combine(VariableSubstitution[] grp1,
            VariableSubstitution[] grp2) {
        if (grp1 == null)
            return grp2;
        if (grp2 == null)
            return grp1;

        VariableSubstitution[] aggArr = new VariableSubstitution[grp1.length
                + grp2.length];
        System.arraycopy(grp1, 0, aggArr, 0, grp1.length);
        System.arraycopy(grp2, 0, aggArr, grp1.length, grp2.length);

        return aggArr;
    }

    /**
     * Use the variable substitutions in the first group to replace any
     * variables that are in values in the second group, then return the
     * combined set of substitutions. This supports scenarios a variable
     * substitution in the second group may contain a value that is a variable
     * from the first group.
     */
    public static VariableSubstitution[] substituteAndCombine(
            VariableSubstitution[] grp1, VariableSubstitution[] grp2)
            throws ParseException {
        if (grp1 == null) {
            return grp2;
        }
        if (grp2 == null) {
            return grp1;
        }
        Map<String, String> map = toMap(grp1);
        VariableSubstitution[] newGroup2 = new VariableSubstitution[grp2.length];
        for (int i = 0; i < grp2.length; i += 1) {
            String oldValue = grp2[i].value;
            String newValue = VariableSubstitutor.processVariables(oldValue,
                    map);
            if (oldValue.equals(newValue)) {
                newGroup2[i] = grp2[i];
            } else {
                newGroup2[i] = new VariableSubstitution(grp2[i].key, newValue);
            }
        }
        grp2 = newGroup2;
        return combine(grp1, grp2);
    }

    public static HashMap<String, String> toMap(VariableSubstitution[] arr) {
        HashMap<String, String> map = new HashMap<String, String>();
        for (VariableSubstitution vs : arr) {
            map.put(vs.key, vs.value);
        }

        return map;
    }

    @Override
    public String toString() {
        return "VariableSubstitution [key=" + key + ", value=" + value + "]";
    }

}