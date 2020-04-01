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
package com.raytheon.uf.common.datastore.ignite.plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * JAXB representation og the {@link CachePluginRegistry} state.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Apr 01, 2020  8072     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "pluginRegistry")
public class PluginRegistryConfig {

    @XmlElement(name = "entry")
    private List<ConfigEntry> entries;

    public PluginRegistryConfig() {

    }

    public PluginRegistryConfig(List<ConfigEntry> entries) {
        this.entries = entries;
    }

    public List<ConfigEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<ConfigEntry> entries) {
        this.entries = entries;
    }

    public void addEntry(ConfigEntry entry) {
        if (this.entries == null) {
            this.entries = new ArrayList<>();
        }
        this.entries.add(entry);
    }

    public void addEntry(String plugin, String cache) {
        addEntry(new ConfigEntry(plugin, cache));
    }

    public boolean isEmpty() {
        return this.entries == null ? true : this.entries.isEmpty();
    }

    public void sortByPlugin() {
        if (this.entries != null) {
            this.entries.sort(Comparator.comparing(ConfigEntry::getPlugin));
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entries == null) ? 0 : entries.hashCode());
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
        PluginRegistryConfig other = (PluginRegistryConfig) obj;
        if (entries == null) {
            if (other.entries != null) {
                return false;
            }
        } else if (!entries.equals(other.entries)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "PluginRegistryConfig [",
                "]");
        if (this.entries != null) {
            for (ConfigEntry entry : this.entries) {
                joiner.add(entry.getPlugin() + "=" + entry.getCache());
            }
        }
        return joiner.toString();
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class ConfigEntry {

        @XmlAttribute
        private String plugin;

        @XmlElement
        private String cache;

        public ConfigEntry() {

        }

        public ConfigEntry(String plugin, String cache) {
            this.plugin = plugin;
            this.cache = cache;
        }

        public String getPlugin() {
            return plugin;
        }

        public void setPlugin(String plugin) {
            this.plugin = plugin;
        }

        public String getCache() {
            return cache;
        }

        public void setCache(String cache) {
            this.cache = cache;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((cache == null) ? 0 : cache.hashCode());
            result = prime * result
                    + ((plugin == null) ? 0 : plugin.hashCode());
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
            ConfigEntry other = (ConfigEntry) obj;
            if (cache == null) {
                if (other.cache != null) {
                    return false;
                }
            } else if (!cache.equals(other.cache)) {
                return false;
            }
            if (plugin == null) {
                if (other.plugin != null) {
                    return false;
                }
            } else if (!plugin.equals(other.plugin)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "ConfigEntry [plugin=" + plugin + ", cache=" + cache + "]";
        }

    }

}
