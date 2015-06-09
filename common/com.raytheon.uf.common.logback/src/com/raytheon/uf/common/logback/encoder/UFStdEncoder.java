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
package com.raytheon.uf.common.logback.encoder;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

import com.raytheon.uf.common.logback.LogbackUtil;

/**
 * This class uses a standard pattern when an instance pattern is not defined.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 16, 2015 4015       rferrel     Initial creation
 * Jun 09, 2015 4473       njensen     Moved from status to logback plugin
 * 
 * </pre>
 * 
 * @author rferrel
 * @version 1.0
 */

public class UFStdEncoder extends PatternLayoutEncoder {
    /**
     * When false %nopex is added to the standard pattern.
     */
    private boolean trace = true;

    @Override
    public String getPattern() {
        String pattern = super.getPattern();
        if ((pattern == null) || pattern.trim().isEmpty()) {
            pattern = LogbackUtil.getUFMessagePattern(context);
            if (!trace) {
                pattern += " %nopex";
            }
        }
        return pattern;
    }

    public boolean isTrace() {
        return trace;
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }
}
