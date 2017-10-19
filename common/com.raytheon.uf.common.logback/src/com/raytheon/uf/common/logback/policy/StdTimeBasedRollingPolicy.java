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
package com.raytheon.uf.common.logback.policy;

import com.raytheon.uf.common.logback.LogbackUtil;

import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

/**
 * Class uses a standard filename pattern when one is not defined. It also sets
 * default value for history.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 17, 2015 4015       rferrel     Initial creation
 * Jun 09, 2015 4473       njensen     Moved from status to logback plugin
 * Oct 19, 2017 6367       tgurney     Allow null name
 *
 * </pre>
 *
 * @author rferrel
 */

public class StdTimeBasedRollingPolicy<E> extends TimeBasedRollingPolicy<E> {
    private String name;

    public StdTimeBasedRollingPolicy() {
        setMaxHistory(LogbackUtil.STD_HISTORY);
    }

    @Override
    public void start() {
        String fileNamePattern = getFileNamePattern();
        if ((fileNamePattern == null) || fileNamePattern.trim().isEmpty()) {
            fileNamePattern = LogbackUtil.determineUFFilenamePattern(context,
                    name);
            setFileNamePattern(fileNamePattern);
        }
        super.start();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
