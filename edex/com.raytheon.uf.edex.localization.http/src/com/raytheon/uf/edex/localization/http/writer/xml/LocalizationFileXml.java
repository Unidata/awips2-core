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

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.edex.localization.http.writer.IDirectoryListingWriter;

/**
 * JAXB representation of localization file metadata that will be displayed in a
 * json directory listing.
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
public class LocalizationFileXml extends LocalizationFilesXml {

    @XmlAttribute
    private String name;

    @XmlAttribute
    private String checksum;

    @XmlAttribute
    private Date timestamp;

    public LocalizationFileXml() {

    }

    public LocalizationFileXml(ILocalizationFile file) {
        this.name = IDirectoryListingWriter.getBaseName(file);
        this.checksum = file.getCheckSum();
        this.timestamp = file.getTimeStamp();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}
