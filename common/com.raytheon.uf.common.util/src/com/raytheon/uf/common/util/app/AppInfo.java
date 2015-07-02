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
package com.raytheon.uf.common.util.app;

/**
 * 
 * Information about the currently running application.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Jul 01, 2015  4021     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class AppInfo {

    private static AppInfo instance;

    private final String name;

    private final String version;

    private final String mode;

    public AppInfo(String name, String version, String mode) {
        super();
        this.name = name;
        this.version = version;
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getMode() {
        return mode;
    }

    public static synchronized AppInfo getInstance() {
        return instance;
    }

    public static synchronized AppInfo initialize(String name, String version,
            String mode) throws IllegalStateException {
        if (instance != null) {
            throw new IllegalStateException(
                    "Application Version cannot be set more than once.");
        }
        return instance = new AppInfo(name, version, mode);
    }
}
