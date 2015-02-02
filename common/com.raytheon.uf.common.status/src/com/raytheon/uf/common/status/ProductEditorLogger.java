package com.raytheon.uf.common.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log4j Performance status handler.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 28, 2015   4018     randerso    Initial creation
 * 
 * </pre>
 * 
 * @author randerso
 * @version 1.0
 */

public class ProductEditorLogger {
    /** Logger */
    private final Logger productEditorLog = LoggerFactory
            .getLogger("ProductEditorLogger");

    /** Prefix to append to all log messages */
    private final String prefix;

    /**
     * Constructor.
     * 
     * @param prefix
     *            Message prefix
     */
    public ProductEditorLogger(String prefix) {
        this.prefix = prefix;
    }

    public void log(String message) {
        productEditorLog.info(prefix + " " + message);
    }

    public void logEdit(int offset, String oldText, String newText) {
        productEditorLog.info(prefix + " offset:" + offset + " oldText:'"
                + oldText + "' newText:'" + newText + "'");
    }
}
