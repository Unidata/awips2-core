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
package com.raytheon.uf.viz.core.rsc.groups;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.viz.core.rsc.AbstractRequestableResourceData;

/**
 * @deprecated do not extend this class, only exists for XML compatibility.
 * 
 *             <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -------------------------------
 * Mar 02, 2010  2496     mnash     Initial creation
 * Oct 23, 2013  2491     bsteffen  Remove ISerializableObject
 * Sep 26, 2014  3669     bsteffen  Deprecate
 * Sep 12, 2016  3241     bsteffen  Move to uf.viz.core.rsc plugin
 * 
 *             </pre>
 * 
 * @author mnash
 */
@Deprecated
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractSpatialEnabler {

    public void enable(PluginDataObject d, AbstractRequestableResourceData arrd) {
        /* Nothing calls this method */
    }
}
