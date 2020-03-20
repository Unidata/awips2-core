package com.raytheon.uf.edex.requestsrv.logging;

import java.util.List;
import java.util.ArrayList;
import javax.xml.bind.annotation.*;

/**
 * Class used by JAXB to transform request logging configuration from XML to POJOs.
 * This class contains the top-level list of all request filters and global attributes.
 * 
 * @author Brian Rapp
 * @version 1.0
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#    Engineer    Description
 * ------------- ---------- ----------- --------------------------------------------
 * Mar 8, 2020   DCS 21885  brapp       Initial creation
 * </pre>
 */
@XmlRootElement(name="requests")
@XmlAccessorType(XmlAccessType.NONE)
public class RawRequestFilters {
	/**
	 * Default maximum length of request string attributes.  This is the default
	 * value used if a length is not specified in the config file.
	 */
    private static final int defaultMaxStringLength = 160;

    /**
     * Default maximum JSON string length.  This is the default value used if a
     * length is not specified in the config file.
     */
    private static final int defaultMaxJsonLength = 8192;

    @XmlAttribute
    private boolean loggingEnabled = true;

    @XmlAttribute
    private int maxFieldStringLength = defaultMaxStringLength;

    @XmlAttribute
    private int maxJsonStringLength = defaultMaxJsonLength;

	@XmlAttribute
    private boolean discoveryMode = false;

    @XmlElement(name="request")
    private List<RawRequestFilter> rawFilters = new ArrayList<>();

    /**
     * Getter for request logging enabled flag
     * @return boolean
     *     true if request logging is enabled; false if disabled
     */
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    /**
     * Getter for maximum length of logged request attribute strings
     * @return int
     *     maximum length of string attributes
     */
    public int getMaxFieldStringLength() {
        return maxFieldStringLength;
    }

    /**
     * Getter for maximum length of JSON log strings
     * @return int
     *     maximum configured length of JSON log strings
     */
    public int getMaxJsonStringLength() {
		return maxJsonStringLength;
	}

    /**
     * Getter for request filters
     * @return List
     *     List of {@link RawRequestFilter}s
     */
    public List<RawRequestFilter> getFilters() {
        return rawFilters;
    }

    /**
     * Getter for discovery mode
     * @return boolean
     *     if true, log all request classes; else use the config files to determine
     *  the classes to log.
     */
    public boolean isDiscoveryMode() {
        return discoveryMode;
    }
}
