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
package com.raytheon.uf.common.status;

import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Status handler for status messages. Outputs via the UFStatus configured
 * factory.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 15, 2011            randerso    Initial creation
 * Oct 22, 2013 2303       bgonzale    Merged VizStatusHandler and SysErrStatusHandler.
 * Jun 14, 2017 6316       njensen     Removed inherited interface default methods
 * 
 * </pre>
 * 
 * @author randerso
 */

public class StatusHandler implements IUFStatusHandler {

    private final String pluginId;

    private final String category;

    private String source;

    public StatusHandler(String pluginId, String category, String source) {
        this.pluginId = pluginId;
        this.category = category;
        this.source = source;
    }

    @Override
    public boolean isPriorityEnabled(Priority p) {
        return true;
    }

    @Override
    public void handle(UFStatus status) {
        handle(status.getPriority(), status.getMessage(),
                status.getException());
    }

    @Override
    public void handle(UFStatus status, String category) {
        handle(status.getPriority(), category, status.getMessage(),
                status.getException());
    }

    @Override
    public void handle(Priority priority, String message) {
        handle(priority, message, (Throwable) null);
    }

    @Override
    public void handle(Priority priority, String category, String message) {
        handle(priority, category, message, (Throwable) null);
    }

    @Override
    public void handle(Priority priority, String message, Throwable throwable) {
        handle(priority, category, message, throwable);
    }

    @Override
    public void handle(Priority priority, String category, String message,
            Throwable throwable) {
        UFStatus.log(priority, this, message, throwable);
    }

    /**
     * @return the pluginId
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Set the source
     */
    public void setSource(String source) {
        this.source = source;
    }

}
