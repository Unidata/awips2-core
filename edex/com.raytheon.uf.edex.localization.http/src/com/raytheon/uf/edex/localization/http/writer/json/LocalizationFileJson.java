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
package com.raytheon.uf.edex.localization.http.writer.json;

import java.util.Date;
import java.util.Map;

import com.raytheon.uf.common.localization.ILocalizationFile;

/**
 * JSON representation of localization file metadata that will be displayed in a
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
public class LocalizationFileJson {

    private String checksum;

    private Date timestamp;

    private Map<String, LocalizationFileJson> children;

    public LocalizationFileJson() {

    }

    public LocalizationFileJson(ILocalizationFile file) {
        this.checksum = file.getCheckSum();
        this.timestamp = file.getTimeStamp();
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

    public Map<String, LocalizationFileJson> getChildren() {
        return children;
    }

    public void setChildren(Map<String, LocalizationFileJson> children) {
        this.children = children;
    }

}
