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
package com.raytheon.uf.edex.distribution;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.ArrayUtils;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Container for the various Distribution patterns used by plugins.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 6, 2013  2327      rjpeter     Initial creation
 * May 09, 2014 3151      bclement    added checkForPluginsMissingPatterns()
 * Jul 21, 2014 3373      bclement    uses its own jaxb manager during refresh
 * Apr 14, 2016 5565      skorolev    extended getDistributionFiles() to common_static
 * 
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */
public class DistributionPatterns {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DistributionPatterns.class);

    private static final DistributionPatterns instance = new DistributionPatterns();

    /**
     * Used to track file modified time to determine if a pattern set needs to
     * be reloaded.
     */
    private final ConcurrentMap<String, Long> modifiedTimes = new ConcurrentHashMap<>();

    /**
     * Patterns for the various plugins.
     */
    private final ConcurrentMap<String, RequestPatterns> patterns = new ConcurrentHashMap<>();

    private final Set<String> pluginsMissingPatterns = Collections
            .newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Returns the singleton instance.
     * 
     * @return
     */
    public static DistributionPatterns getInstance() {
        return instance;
    }

    private DistributionPatterns() {
        refresh();
    }

    /**
     * Loads patterns from a distribution file for the specified plugin.
     * 
     * @param jaxb
     *            jaxb manager for request patterns
     * @param file
     *            The file containing the ingest patterns
     * @throws DistributionException
     *             If the modelFile cannot be deserialized
     */
    private RequestPatterns loadPatterns(
            SingleTypeJAXBManager<RequestPatterns> jaxb, File file)
            throws DistributionException {
        RequestPatterns patternSet = null;
        try {
            patternSet = jaxb.unmarshalFromXmlFile(file);
        } catch (Exception e) {
            throw new DistributionException("File " + file.getAbsolutePath()
                    + " could not be unmarshalled.", e);
        }
        patternSet.compilePatterns();
        return patternSet;
    }

    /**
     * Lists the files in the distribution directory
     * 
     * @return An array of the files in the distribution directory
     */
    private Collection<File> getDistributionFiles() {
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        // files from edex_static
        LocalizationFile[] edex_files = pathMgr.listFiles(
                pathMgr.getLocalSearchHierarchy(LocalizationType.EDEX_STATIC),
                "distribution", new String[] { ".xml" }, true, false);
        // files from common_static
        LocalizationFile[] common_files = pathMgr
                .listFiles(
                        pathMgr.getLocalSearchHierarchy(LocalizationType.COMMON_STATIC),
                        "distribution", new String[] { ".xml" }, true, false);
        // join both arrays of files
        LocalizationFile[] files = (LocalizationFile[]) ArrayUtils.addAll(
                common_files, edex_files);

        Map<String, File> distFiles = new HashMap<>();
        for (LocalizationFile file : files) {
            if (!distFiles.containsKey(file.getPath())) {
                distFiles.put(file.getPath(), file.getFile());
            }
        }

        return distFiles.values();
    }

    /**
     * Refreshes the distribution patterns if a plugin's distribution pattern
     * file has been modified. This method is executed via a quartz timer
     */
    public void refresh() {
        SingleTypeJAXBManager<RequestPatterns> jaxb = null;

        for (File file : getDistributionFiles()) {
            String fileName = file.getName();
            Long modTime = modifiedTimes.get(fileName);
            if ((modTime == null)
                    || (modTime.longValue() != file.lastModified())) {
                // getDistributionFiles only returns files ending in .xml
                int index = fileName.lastIndexOf(".");
                String plugin = null;
                if (index > 0) {
                    plugin = fileName.substring(0, index);
                } else {
                    plugin = fileName;
                }

                try {
                    if (patterns.containsKey(plugin)) {
                        statusHandler
                                .info("Change to distribution file detected. "
                                        + fileName
                                        + " has been modified.  Reloading distribution patterns");
                    }
                    if (jaxb == null) {
                        jaxb = createJaxbManager();
                    }
                    patterns.put(plugin, loadPatterns(jaxb, file));
                    modifiedTimes.put(fileName, file.lastModified());
                } catch (DistributionException e) {
                    statusHandler.error(
                            "Error reloading distribution patterns from file: "
                                    + fileName, e);
                }
            }
        }
        checkForPluginsMissingPatterns();
    }

    /**
     * @return new jaxb manager for unmarshalling RequestPatterns
     * @throws DistributionException
     */
    private SingleTypeJAXBManager<RequestPatterns> createJaxbManager()
            throws DistributionException {
        try {
            return new SingleTypeJAXBManager<>(true, RequestPatterns.class);
        } catch (JAXBException e) {
            throw new DistributionException(
                    "Unable to refresh distribution patterns, "
                            + "cannot create JAXB manager", e);
        }
    }

    /**
     * Returns a list of plugins that are interested in the given header.
     * 
     * @param header
     * @return
     */
    public List<String> getMatchingPlugins(String header) {
        List<String> plugins = new LinkedList<>();

        for (Map.Entry<String, RequestPatterns> entry : patterns.entrySet()) {
            if (entry.getValue().isDesiredHeader(header)) {
                plugins.add(entry.getKey());
            }
        }

        return plugins;
    }

    /**
     * Returns a list of plugins that are interested in the given header.
     * 
     * @param header
     * @param pluginsToCheck
     * @return
     */
    public List<String> getMatchingPlugins(String header,
            Collection<String> pluginsToCheck) {
        List<String> plugins = new LinkedList<>();

        for (String plugin : pluginsToCheck) {
            RequestPatterns pattern = patterns.get(plugin);
            if (pattern == null || pattern.noPossibleMatch()) {
                pluginsMissingPatterns.add(plugin);
            } else if (pattern.isDesiredHeader(header)) {
                plugins.add(plugin);
            }
        }

        return plugins;
    }

    /**
     * check if there have been requests for distribution patterns for plugins
     * that don't have valid patterns. Logs an error message if any are found.
     */
    public void checkForPluginsMissingPatterns() {
        for (String plugin : pluginsMissingPatterns) {
            String msg = "No valid distribution patterns for " + plugin;
            statusHandler.error(msg);
        }
        pluginsMissingPatterns.clear();
    }

    /**
     * Returns true if there are patterns registered for the given plugin, false
     * otherwise.
     * 
     * @param pluginName
     * @return
     */
    public boolean hasPatternsForPlugin(String pluginName) {
        RequestPatterns rp = patterns.get(pluginName);
        return rp != null && !rp.noPossibleMatch();
    }
}
