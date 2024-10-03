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
package com.raytheon.uf.edex.esb.camel.cluster.cron;

/**
 * A custom Camel Configuration that has additional support for the timezone
 * parameter. timezone is mostly used in CpgRequestCamelRoutes, but the parameter is optional
 * and can be null.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date           Ticket#      Engineer        Description
 * ------------   ----------   -----------     --------------------------
 * Sep 24, 2024   2037919      lisa.singh      Initial creation
 *
 * </pre>
 */

import org.apache.camel.component.cron.api.CamelCronConfiguration;

public class TimeZonedCamelCronConfiguration extends CamelCronConfiguration {

    private String timeZone;

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

}