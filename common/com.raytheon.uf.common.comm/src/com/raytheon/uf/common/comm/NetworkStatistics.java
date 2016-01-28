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
package com.raytheon.uf.common.comm;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class for logging network sent/received amounts for various types
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 1, 2011             mschenke    Initial creation
 * Jan 27, 2016 5170       tjensen     Improve network statistic to track messages,
 *                                      byte tracking only performed when configured
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class NetworkStatistics {

    public static class NetworkTraffic {

        private String identifier;

        private long bytesSent;

        private long bytesReceived;

        private long requestCount;

        /**
         * Stores the value of the System Property used to configure if
         * statistics for the number of bytes sent and received should be
         * tracked.
         * 
         * Note: Byte sizes are unavailable for requests/responses that are
         * compressed due to 'content-length' not being available, which may
         * adversely effect the tracking of these stats.
         */
        private static final boolean doByteStats = Boolean
                .getBoolean("logging.byteStats");

        private NetworkTraffic(String identifier) {
            this.identifier = identifier;
        }

        private void addBytesSent(long sent) {
            bytesSent += sent;
        }

        private void addBytesReceived(long received) {
            bytesReceived += received;
        }

        private void incrementRequestCount() {
            requestCount += 1;
        }

        public boolean isDoByteStats() {
            return doByteStats;
        }

        public long getBytesSent() {
            return bytesSent;
        }

        public long getBytesReceived() {
            return bytesReceived;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public String getIdentifier() {
            return identifier;
        }

        @Override
        public NetworkTraffic clone() {
            NetworkTraffic newTraffic = new NetworkTraffic(identifier);
            newTraffic.identifier = identifier;
            newTraffic.bytesReceived = bytesReceived;
            newTraffic.bytesSent = bytesSent;
            newTraffic.requestCount = requestCount;

            return newTraffic;
        }

        @Override
        public String toString() {
            String sentString = NetworkStatistics.toString(bytesSent), receivedString = NetworkStatistics
                    .toString(bytesReceived), bytesStatsMsg = "";
            if (doByteStats) {
                bytesStatsMsg = ", sent " + sentString + ", received "
                        + receivedString;
            }

            return "Network Traffic Stats for '" + identifier + "' : "
                    + requestCount + " messages" + bytesStatsMsg;
        }
    }

    private NetworkTraffic totalTraffic = new NetworkTraffic(null);

    private Map<String, NetworkTraffic> mappedTraffic = new LinkedHashMap<String, NetworkTraffic>();

    public NetworkStatistics() {

    }

    /**
     * Add to the log of bytes sent/received for total network traffic. Since
     * *all* network communication *should* go through HttpClient, this method
     * has package level visibility. Other methods should log using an
     * identifier string
     * 
     * @param bytesSent
     * @param bytesRecieved
     */
    void log(long bytesSent, long bytesReceived) {
        synchronized (totalTraffic) {
            /**
             * Only log bytes if byte stats are enabled and if the number
             * sent/received is greater than 1. Byte counts for compressed
             * messages or messages of unknown length may return -1. Requests
             * known to be compressed may also pass a bytes size of '1' to
             * trigger the incrementing of the request count. Any sizes greater
             * than 1 can be assumed to be 'real' sizes.
             */
            if (NetworkTraffic.doByteStats) {
                if (bytesSent > 1) {
                    totalTraffic.addBytesSent(bytesSent);
                }
                if (bytesReceived > 1) {
                    totalTraffic.addBytesReceived(bytesReceived);
                }
            }
            if (bytesSent > 0) {
                totalTraffic.incrementRequestCount();
            }
        }
    }

    /**
     * Add to the log of bytes sent/received for the traffic tracked by the type
     * identifier passed in
     * 
     * @param typeIdentifier
     * @param bytesSent
     * @param bytesRecieved
     */
    public synchronized void log(String typeIdentifier, long bytesSent,
            long bytesReceived) {
        NetworkTraffic traffic = mappedTraffic.get(typeIdentifier);
        if (traffic == null) {
            traffic = new NetworkTraffic(typeIdentifier);
            mappedTraffic.put(typeIdentifier, traffic);
        }

        /**
         * Only log bytes if byte stats are enabled and if the number
         * sent/received is greater than 1. Byte counts for compressed messages
         * or messages of unknown length may return -1. Requests known to be
         * compressed may also pass a bytes size of '1' to trigger the
         * incrementing of the request count. Any sizes greater than 1 can be
         * assumed to be 'real' sizes.
         */
        if (NetworkTraffic.doByteStats) {
            if (bytesSent > 1) {
                traffic.addBytesSent(bytesSent);
            }
            if (bytesReceived > 1) {
                traffic.addBytesReceived(bytesReceived);
            }
        }
        if (bytesSent > 0) {
            traffic.incrementRequestCount();
        }
        this.log(bytesSent, bytesReceived);
    }

    /**
     * Get a copy of the total traffic stats at point of calling
     * 
     * @return
     */
    public NetworkTraffic getTotalTrafficStats() {
        synchronized (totalTraffic) {
            return totalTraffic.clone();
        }
    }

    /**
     * Get the NetworkTraffic objects for mapped entries. Theoretically total
     * from mapped should add up to values from getMappedTrafficStats()
     * 
     * @return copy of network traffic stats
     */
    public NetworkTraffic[] getMappedTrafficStats() {
        Collection<NetworkTraffic> trafficStats = mappedTraffic.values();
        NetworkTraffic[] traffic = trafficStats.toArray(
                new NetworkTraffic[trafficStats.size()]).clone();
        for (int i = 0; i < traffic.length; ++i) {
            traffic[i] = traffic[i].clone();
        }
        return traffic;
    }

    private static final long[] divisions = new long[] { 1, 1024, 1024 * 1024,
            1024 * 1024 * 1024 };

    private static final String[] units = new String[] { "B", "kB", "MB", "GB" };

    public static String toString(long amount) {
        String unit = units[units.length - 1];
        long divideBy = divisions[divisions.length - 1];
        for (int i = 0; i < divisions.length - 1; ++i) {
            if (amount < divisions[i + 1]) {
                divideBy = divisions[i];
                unit = units[i];
                break;
            }
        }

        return ((amount / divideBy) + unit);
    }
}
