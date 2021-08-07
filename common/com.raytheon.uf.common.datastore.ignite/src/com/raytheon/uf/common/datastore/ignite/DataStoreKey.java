/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastore.ignite;

import java.util.Comparator;

import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

/**
 * Unique key for a file path and group name.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------
 * May 20, 2019  7628     bsteffen  Initial creation
 * Jun 10, 2021  8450     mapeters  Implement {@link Comparable}
 *
 * </pre>
 *
 * @author bsteffen
 */
public class DataStoreKey implements Comparable<DataStoreKey> {

    private static final Comparator<String> stringComparator = Comparator
            .nullsFirst(String::compareTo);

    private static final Comparator<DataStoreKey> comparator = Comparator
            .comparing(DataStoreKey::getPath, stringComparator)
            .thenComparing(DataStoreKey::getGroup, stringComparator);

    /*
     * In ignite 2.7 inlineSize doesn't seem to take effect unless you also set
     * sqlIndexMaxInlineSize in the CacheConfiguration
     */
    @AffinityKeyMapped
    @QuerySqlField(index = true, inlineSize = 350)
    private String path;

    @QuerySqlField(name = "recgroup")
    private String group;

    public DataStoreKey() {

    }

    public DataStoreKey(String path, String group) {
        this.path = path;
        this.group = group;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public int compareTo(DataStoreKey that) {
        return comparator.compare(this, that);
    }

    @Override
    public String toString() {
        return "DataStoreKey [path=" + path + ", group=" + group + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
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
        DataStoreKey other = (DataStoreKey) obj;
        if (group == null) {
            if (other.group != null) {
                return false;
            }
        } else if (!group.equals(other.group)) {
            return false;
        }
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        return true;
    }
}
