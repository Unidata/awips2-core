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

package com.raytheon.uf.common.message.response;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Abstract base for messages returned from EDEX service endpoints
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ???                     chammack     Initial creation
 * Sep 12, 2014 3583       bclement     removed ISerializableObject
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public abstract class AbstractResponseMessage {

    @XmlElement
    @DynamicSerializeElement
    protected String fileType;

    @XmlElement
    @DynamicSerializeElement
    protected String dataURI;

    @XmlElement
    @DynamicSerializeElement
    protected Date validTime;

    /**
     * @return the fileType
     */
    public String getFileType() {
        return fileType;
    }

    /**
     * @param fileType
     *            the fileType to set
     */
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    /**
     * @return the dataURI
     */
    public String getDataURI() {
        return dataURI;
    }

    /**
     * @param dataURI
     *            the dataURI to set
     */
    public void setDataURI(String dataURI) {
        this.dataURI = dataURI;
    }

    /**
     * @return the validTime
     */
    public Date getValidTime() {
        return validTime;
    }

    /**
     * @param validTime
     *            the validTime to set
     */
    public void setValidTime(Date validTime) {
        this.validTime = validTime;
    }

}
