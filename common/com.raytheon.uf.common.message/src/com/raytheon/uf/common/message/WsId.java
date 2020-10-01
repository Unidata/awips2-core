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
package com.raytheon.uf.common.message;

import java.io.Serializable;

import com.raytheon.uf.common.message.adapter.WsIdAdapter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeTypeAdapter;
import com.raytheon.uf.common.util.SystemUtil;

/**
 * The WsId contains the work station identification for the user. *
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 10, 2009           randerso  Initial creation
 * Apr 25, 2012  545      randerso  Repurposed the lockKey field as threadId
 * Sep 19, 2012  1190     dgilling  Cache host names so toPrettyString() doesn't
 *                                  get delayed behind DNS requests.
 * Sep 20, 2012  1190     dgilling  Create method getHostName().
 * Mar 20, 2014  2726     rjpeter   Moved hostNameCache to SystemUtil.
 * Sep 12, 2014  3583     bclement  removed ISerializableObject
 * Jun 24, 2020  8187     randerso  Changed to use hostName instead of integer
 *                                  network address. Changed pid to long.
 * Oct 01, 2020  8239     randerso  Added getClientId() to match the JMS
 *                                  client ID string.
 *
 * </pre>
 *
 * @author randerso
 */

@DynamicSerialize
@DynamicSerializeTypeAdapter(factory = WsIdAdapter.class)
public class WsId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String hostName;

    private final String userName;

    private final String progName;

    private long pid;

    private final long threadId;

    /**
     * Constructs a default WsId. Only for use by serialization
     */
    public WsId() {
        this(null, null, null);
    }

    /**
     * Constructs a WsId from a string
     */
    public WsId(String s) {
        String[] token = s.split(":");

        if (token.length != 5) {
            throw new IllegalArgumentException(
                    "argument not of proper format for WsId");
        }

        this.hostName = token[0];
        this.userName = token[1];
        this.progName = token[2];
        try {
            this.pid = Long.parseLong(token[3]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "pid argument not of proper format for WsId");
        }
        this.threadId = Long.parseLong(token[4]);

    }

    /**
     * Constructor for WsId taking the networkId, user name, progName.
     *
     * @param hostName
     *            If null local host name will be used if available, otherwise
     *            will use localhost
     * @param userName
     *            if null current login name will be used
     *
     * @param progName
     *            if null "unknown" will be used
     *
     */
    public WsId(String hostName, final String userName, final String progName) {

        if (userName != null) {
            this.userName = userName;
        } else {
            this.userName = System.getProperty("user.name");
        }

        if (progName != null) {
            this.progName = progName;
        } else {
            this.progName = "unknown";
        }

        this.pid = SystemUtil.getPid();

        this.threadId = Thread.currentThread().getId();

        if (hostName != null) {
            this.hostName = hostName;
        } else {
            this.hostName = SystemUtil.getHostName();
        }
    }

    @Override
    public String toString() {

        String s = String.join(":", hostName, userName, progName,
                Long.toString(pid), Long.toString(threadId));
        return s;
    }

    /**
     * Returns WsId as a pretty text string. A pretty text string has the
     * network address in dotted decimal form or domain name.
     *
     * @return WsId as pretty string
     */
    public String toPrettyString() {
        StringBuilder o = new StringBuilder();
        o.append(userName).append('@').append(hostName).append(':')
                .append(progName).append(':').append("pid-").append(pid)
                .append(':').append("thread-").append(threadId);

        return o.toString();
    }

    public String getClientId() {
        String s = String.join(":", hostName, userName, progName,
                Long.toString(pid));

        return s;
    }

    public String getHostName() {
        return hostName;
    }

    /**
     * @return the _userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return the _progName
     */
    public String getProgName() {
        return progName;
    }

    /**
     * @return the _pid
     */
    public long getPid() {
        return pid;
    }

    /**
     * @return the threadId
     */
    public long getThreadId() {
        return threadId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + (int) (pid ^ (pid >>> 32));
        result = prime * result
                + ((progName == null) ? 0 : progName.hashCode());
        result = prime * result + (int) (threadId ^ (threadId >>> 32));
        result = prime * result
                + ((userName == null) ? 0 : userName.hashCode());
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
        WsId other = (WsId) obj;
        if (hostName == null) {
            if (other.hostName != null) {
                return false;
            }
        } else if (!hostName.equals(other.hostName)) {
            return false;
        }
        if (pid != other.pid) {
            return false;
        }
        if (progName == null) {
            if (other.progName != null) {
                return false;
            }
        } else if (!progName.equals(other.progName)) {
            return false;
        }
        if (threadId != other.threadId) {
            return false;
        }
        if (userName == null) {
            if (other.userName != null) {
                return false;
            }
        } else if (!userName.equals(other.userName)) {
            return false;
        }
        return true;
    }

}
