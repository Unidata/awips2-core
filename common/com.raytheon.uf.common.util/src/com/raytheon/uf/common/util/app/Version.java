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

import java.text.ParseException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Class for storing and comparison of the AWIPS base software version
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 09, 2017  6130     tjensen   Initial creation
 * Aug 03, 2017  6352     tgurney   Moved from Data Delivery, added fromString
 *
 * </pre>
 *
 * @author tjensen
 */
public class Version implements Comparable<Version> {

    private static final Pattern versionPattern = Pattern
            .compile("^(\\d+)(\\.(\\d+))?(\\.(\\d+))?");

    private int[] versionInfo = new int[3];

    public int[] getVersionInfo() {
        return versionInfo;
    }

    /**
     * Set the version info. Argument array must contain exactly 3 integer
     * values.
     *
     * @param versionInfo
     */
    public void setVersionInfo(int[] versionInfo) {
        if (versionInfo.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid version info length received. Expected 3 ints, received "
                            + versionInfo.length);
        }
        this.versionInfo = versionInfo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(versionInfo);
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
        Version other = (Version) obj;
        if (!Arrays.equals(versionInfo, other.versionInfo)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Version o) {
        int retval = Integer.compare(this.versionInfo[0],
                o.getVersionInfo()[0]);
        if (retval == 0) {
            retval = Integer.compare(this.versionInfo[1],
                    o.getVersionInfo()[1]);
            if (retval == 0) {
                retval = Integer.compare(this.versionInfo[2],
                        o.getVersionInfo()[2]);
            }
        }

        return retval;
    }

    /**
     *
     * @param version
     *            A string in the form n.n.n where n is an integer. Second and
     *            third component are optional
     * @return the Version
     * @throws ParseException
     *             If the string is not in the form n.n.n
     */
    public static Version fromString(String version) throws ParseException {
        Version newVer = null;
        Matcher vm = versionPattern.matcher(version);

        if (vm.find()) {
            newVer = new Version();
            int[] ver = new int[3];

            ver[0] = Integer.parseInt(vm.group(1));
            if (vm.group(3) != null) {
                ver[1] = Integer.parseInt(vm.group(3));
            }
            if (vm.group(5) != null) {
                ver[2] = Integer.parseInt(vm.group(5));
            }
            newVer.setVersionInfo(ver);
        } else {
            throw new ParseException("Unable to parse version from string '"
                    + version + "'. Expected pattern = '"
                    + versionPattern.pattern() + "'", 0);
        }
        return newVer;
    }

    @Override
    public String toString() {
        return versionInfo[0] + "." + versionInfo[1] + "." + versionInfo[2];
    }
}
