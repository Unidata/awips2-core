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

package com.raytheon.uf.common.dataplugin.level.mapping;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * Class defines the XML file contains the level mapping information.
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Nov 01, 2007  518      S.Manoj     Initial version
 * Nov 16, 2009  3120     rjpeter     Refactored to use factory and level mapping.
 * Apr 17, 2013  1913     randerso    Moved to common
 * Jan 23, 2014  2711     bsteffen    Remove ISerializableObject
 * 
 * @author smanoj
 * @version 1.0
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "LevelMappings")
public class LevelMappingFile {

    @XmlElements({ @XmlElement(name = "Level", type = LevelMapping.class) })
    private List<LevelMapping> levelMappingFile;

    public List<LevelMapping> getLevelMappingFile() {
        return levelMappingFile;
    }

    public void setLevelMappingFile(ArrayList<LevelMapping> levelMappingFile) {
        this.levelMappingFile = levelMappingFile;
    }
}
