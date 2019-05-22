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
package com.raytheon.uf.viz.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;

/**
 * Class that parses application arguments and provides easy access to them
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 10, 2009            mschenke    Initial creation
 * Apr 29, 2019 7715       tgurney     Add getString(key, defaultValue)
 *
 * </pre>
 *
 * @author mschenke
 */

public class ProgramArguments {

    private static ProgramArguments instance = new ProgramArguments();

    private Map<String, Object> argumentMap;

    public static ProgramArguments getInstance() {
        return instance;
    }

    private ProgramArguments() {
        argumentMap = new HashMap<>();
        parseArgs();
    }

    private void parseArgs() {
        String[] args = Platform.getApplicationArgs();
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                // we have a key
                if (args.length > i + 1 && !args[i + 1].startsWith("-")) {
                    argumentMap.put(arg, args[i + 1]);
                    ++i;
                } else {
                    argumentMap.put(arg, Boolean.valueOf(true));
                }
            }
        }
    }

    public String getString(String key, String defaultValue) {
        Object val = argumentMap.get(key);
        if (val != null) {
            return val.toString();
        }
        return defaultValue;
    }

    /**
     * @return value corresponding to the key, or null if there is no value
     */
    public String getString(String key) {
        return getString(key, null);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        Object val = argumentMap.get(key);
        if (val != null) {
            return (Boolean) val;
        }
        return defaultValue;
    }

    /**
     * @return value corresponding to the key, or false if there is no value
     */
    public Boolean getBoolean(String key) {
        return getBoolean(key, Boolean.valueOf(false));
    }

    public Double getDouble(String key, Double defaultValue) {
        String s = getString(key);
        try {
            return Double.parseDouble(s);
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    /**
     * @return Double value corresponding to the key, or null if there is no
     *         value
     */
    public Double getDouble(String key) {
        return getDouble(key, null);
    }

    public Integer getInteger(String key, Integer defaultValue) {
        String s = getString(key);
        try {
            return Integer.parseInt(s);
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    /**
     * @return Integer value corresponding to the key, or null if there is no
     *         value
     */
    public Integer getInteger(String key) {
        return getInteger(key, null);
    }

    public void addString(String key, String value) {
        argumentMap.put(key, value);
    }
}