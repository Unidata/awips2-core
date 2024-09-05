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
package com.raytheon.uf.common.dataplugin.notify;

import java.util.HashMap;

import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestableMetadataMarshaller;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Configuration object for plugin notification. An empty or null metadataMap
 * implies it should receive all data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Nov 19, 2013  2170     rjpeter   Initial creation
 * Jun 28, 2016  5679     rjpeter   Moved PluginNotifierConfig to common.
 * Sep  5, 2024  2037700  tgurney   Remove vm and direct-vm components (Camel 4)
 *
 * </pre>
 *
 * @author rjpeter
 */
@XmlAccessorType(XmlAccessType.NONE)
public class PluginNotifierConfig {

    public static enum EndpointType {
        /*
         * VM and DIRECTVM are kept only for backwards compatibility and are
         * treated the same as SEDA and DIRECT
         */
        QUEUE, TOPIC, SEDA, DIRECT, VM, DIRECTVM
    }

    public static enum NotifyFormat {
        DATAURI, PDO
    }

    @XmlElement
    protected String endpointName;

    @XmlElement
    protected EndpointType endpointType;

    @XmlElement(required = true)
    protected NotifyFormat format;

    /**
     * Time to live for JMS type endpoints in milliseconds.
     */
    @XmlElement
    protected int timeToLive = -1;

    /**
     * If the JMS message is being sent to a durable endpoint.
     */
    @XmlElement
    protected boolean durable;

    /**
     * the metadata criteria to retrieve the resource
     */
    @XmlElement
    @XmlJavaTypeAdapter(value = RequestableMetadataMarshaller.class)
    protected HashMap<String, RequestConstraint>[] metadataMap;

    protected transient String endpointUri;

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public EndpointType getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(EndpointType endpointType) {
        this.endpointType = endpointType;
    }

    public NotifyFormat getFormat() {
        return format;
    }

    public void setFormat(NotifyFormat format) {
        this.format = format;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(int timeToLive) {
        this.timeToLive = timeToLive;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public HashMap<String, RequestConstraint>[] getMetadataMap() {
        return metadataMap;
    }

    public void setMetadataMap(
            HashMap<String, RequestConstraint>[] metadataMap) {
        this.metadataMap = metadataMap;
    }

    /**
     * Generates and returns the endpoint uri for this configuration.
     *
     * @return
     */
    public String getEndpointUri() {
        if (endpointUri == null) {
            StringBuilder builder = new StringBuilder(64);
            switch (endpointType) {
            case DIRECT:
            case DIRECTVM:
                builder.append("direct:");
                break;
            case SEDA:
            case VM:
                builder.append("seda:");
                break;
            case QUEUE:
            case TOPIC:
                if (durable) {
                    builder.append("jms-durable:");
                } else {
                    builder.append("jms-generic:");
                }

                builder.append(endpointType.name().toLowerCase()).append(':');
                break;
            }

            builder.append(endpointName);

            // for jms endpoints add time to live field
            if (timeToLive > 0 && (EndpointType.QUEUE.equals(endpointType)
                    || EndpointType.TOPIC.equals(endpointType))) {
                // append time to live in milliseconds
                builder.append("?timeToLive=").append(timeToLive);
            }

            endpointUri = builder.toString();
        }

        return endpointUri;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (endpointName == null ? 0 : endpointName.hashCode());
        result = prime * result
                + (endpointType == null ? 0 : endpointType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PluginNotifierConfig other = (PluginNotifierConfig) obj;
        if (endpointName == null) {
            if (other.endpointName != null) {
                return false;
            }
        } else if (!endpointName.equals(other.endpointName)) {
            return false;
        }
        if (endpointType != other.endpointType) {
            return false;
        }
        return true;
    }

}
