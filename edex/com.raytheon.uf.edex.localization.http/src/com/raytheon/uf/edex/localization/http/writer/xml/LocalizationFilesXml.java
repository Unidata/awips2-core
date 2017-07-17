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
package com.raytheon.uf.edex.localization.http.writer.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.localization.ILocalizationFile;

/**
 * JAXB representation of list of localization files.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Aug 07, 2017  5731     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "files")
public class LocalizationFilesXml {

    @XmlElement(name = "file")
    private List<LocalizationFileXml> files;

    public List<LocalizationFileXml> getFiles() {
        return files;
    }

    public void setFiles(List<LocalizationFileXml> files) {
        this.files = files;
    }

    public void add(ILocalizationFile file) {
        add(new LocalizationFileXml(file));
    }

    public void add(LocalizationFileXml xml) {
        if (files == null) {
            files = new ArrayList<>();
        }
        files.add(xml);
    }

    public void addAll(Collection<ILocalizationFile> files) {
        files.stream().forEach(this::add);
    }

}
