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
package com.raytheon.uf.common.dataplugin.level.mapping;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXB;

import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataplugin.level.MasterLevel;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.ILocalizationPathObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Factory for getting level mappings
 *
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 11/16/2009    #3120     rjpeter     Initial version
 * 11/21/2009    #3576     rjpeter     Added group capability
 * 04/17/2013    #1913     randerso    Moved to common
 * 05/16/2013    #2010     randerso    Added read/write locking to mutable maps
 * Sep 09, 2014   3356     njensen     Remove CommunicationException
 * May 18, 2015   4412     bsteffen    Add getAllLevelMappingsForLevel
 * Dec 06, 2017   6355     nabowle     Move level mapping files into a directory.
 *                                     Allow loading of all in a directory. Observe
 *                                     localization changes to an instance's path
 *                                     and reload. Make state maps volatile and replace
 *                                     with updated maps as needed instead of locking.
 *
 * </pre>
 *
 * @author rjpeter
 */
public class LevelMappingFactory implements ILocalizationPathObserver {

    public static final String VOLUMEBROWSER_LEVEL_MAPPING_FILE = "level"
            + IPathManager.SEPARATOR + "mappings";

    public static final String DEFAULT_VB_LEVEL_MAPPING_FILE = VOLUMEBROWSER_LEVEL_MAPPING_FILE
            + IPathManager.SEPARATOR + "LevelMappingFile.xml";

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(LevelMappingFactory.class);

    private static final Map<String, LevelMappingFactory> instanceMap = new HashMap<>();

    private volatile MappingContainer mappingContainer = new MappingContainer();

    private final String filePath;

    public static synchronized LevelMappingFactory getInstance(
            String filePath) {
        LevelMappingFactory instance = instanceMap.get(filePath);
        if (instance == null) {
            instance = new LevelMappingFactory(filePath);
            instanceMap.put(filePath, instance);
        }
        return instance;
    }

    private LevelMappingFactory(String filePath) {
        loadMappings(filePath);
        this.filePath = filePath;
        PathManagerFactory.getPathManager()
                .addLocalizationPathObserver(filePath, this);
    }

    private synchronized void loadMappings(String filePath) {
        List<ILocalizationFile> paths = new ArrayList<>();
        ILocalizationFile path = PathManagerFactory.getPathManager()
                .getStaticLocalizationFile(filePath);
        if (path.isDirectory()) {
            ILocalizationFile[] lfiles = PathManagerFactory.getPathManager()
                    .listStaticFiles(filePath, new String[] { ".xml" }, true,
                            true);
            for (ILocalizationFile lfile : lfiles) {
                paths.add(lfile);
            }
        } else {
            paths.add(path);
        }

        LevelMappingFile levelMapFile = null;
        long start = System.currentTimeMillis();
        Map<String, LevelMapping> newKeyToLevelMappings = new HashMap<>();
        for (ILocalizationFile lf : paths) {
            try (InputStream is = lf.openInputStream()) {
                levelMapFile = JAXB.unmarshal(is, LevelMappingFile.class);
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "An error was encountered while creating the LevelNameMappingFile from "
                                + path.toString(),
                        e);
            }

            List<LevelMapping> levelMappings = levelMapFile
                    .getLevelMappingFile();

            if (levelMappings != null) {
                for (LevelMapping mapping : levelMappings) {
                    if (newKeyToLevelMappings.containsKey(mapping.getKey())) {
                        // handle multiple entries to same key by appending
                        // levels
                        LevelMapping priorEntry = newKeyToLevelMappings
                                .get(mapping.getKey());
                        priorEntry.getDatabaseLevels()
                                .addAll(mapping.getDatabaseLevels());
                    } else {
                        newKeyToLevelMappings.put(mapping.getKey(), mapping);
                    }
                }
            }
        }
        MappingContainer newMappings = new MappingContainer();
        newMappings.setKeyToLevelMappings(newKeyToLevelMappings);

        initializeLevelToLevelMappings(newMappings);
        initializeGroupToMasterLevels(newMappings);

        mappingContainer = newMappings;

        long finish = System.currentTimeMillis();
        statusHandler.info("LevelMappingFactory initialization took ["
                + (finish - start) + "] ms");
    }

    public LevelMapping getLevelMappingForKey(String key) {
        return mappingContainer.getKeyToLevelMappings().get(key);
    }

    /**
     * Return all {@link LevelMapping}s which contain a specified {@link Level}.
     * For most Levels this will return a collection with a single element but
     * for rare cases this will return multiple levels. If the level is not in
     * any mappings, this method will return null.
     *
     * @see #getLevelMappingForLevel(Level)
     */
    public Collection<LevelMapping> getAllLevelMappingsForLevel(Level level) {
        Collection<LevelMapping> mappings = mappingContainer
                .getLevelToLevelMappings().get(level);
        if (mappings == null) {
            return null;
        } else {
            return Collections.unmodifiableCollection(mappings);
        }
    }

    /**
     * Convenience method which can return a single {@link LevelMapping} which
     * contains a {@link Level}. It is generally discouraged to have a level in
     * multiple mappings so for most use cases this method can be used. In rare
     * cases where the level is contained in multiple mappings this will return
     * the mapping with the least levels(the most specific mapping). If the
     * level is not in any mappings, this method will return null.
     *
     * @see #getAllLevelMappingsForLevel(Level)
     */
    public LevelMapping getLevelMappingForLevel(Level level) {
        Collection<LevelMapping> mappings = mappingContainer
                .getLevelToLevelMappings().get(level);
        if (mappings == null || mappings.isEmpty()) {
            return null;
        } else {
            Iterator<LevelMapping> it = mappings.iterator();
            LevelMapping retVal = it.next();
            while (it.hasNext()) {
                LevelMapping test = it.next();
                if (test.getLevels().size() < retVal.getLevels().size()) {
                    /*
                     * Only replace the old level mapping if we have less levels
                     * than the old mapping This should cause the most specific
                     * mapping to be used
                     */
                    retVal = test;
                }
            }
            return retVal;
        }
    }

    public Collection<LevelMapping> getAllLevelMappings() {
        return mappingContainer.getKeyToLevelMappings().values();
    }

    public Set<Level> getAllLevels() {
        Set<Level> retVal = mappingContainer.getLevelToLevelMappings().keySet();
        return retVal;
    }

    public Map<MasterLevel, Set<Level>> getLevelMapForGroup(String group) {
        Map<MasterLevel, Set<Level>> retVal = mappingContainer
                .getGroupToMasterLevels().get(group);
        return retVal;
    }

    private void initializeLevelToLevelMappings(MappingContainer newMappings) {
        Map<Level, Collection<LevelMapping>> newLevelToLevelMappings = new HashMap<>();
        Collection<LevelMapping> levelMappings = newMappings
                .getKeyToLevelMappings().values();
        for (LevelMapping mapping : levelMappings) {
            for (Level l : mapping.getLevels()) {
                Collection<LevelMapping> mappings = newLevelToLevelMappings
                        .get(l);
                if (mappings == null) {
                    mappings = new ArrayList<>(1);
                    newLevelToLevelMappings.put(l, mappings);
                }
                mappings.add(mapping);
            }
        }
        newMappings.setLevelToLevelMappings(newLevelToLevelMappings);
    }

    private void initializeGroupToMasterLevels(MappingContainer newMappings) {
        Map<String, Map<MasterLevel, Set<Level>>> newGroupToMasterLevels = new HashMap<>();
        Collection<LevelMapping> levelMappings = newMappings
                .getKeyToLevelMappings().values();
        for (LevelMapping mapping : levelMappings) {
            String group = mapping.getGroup();
            Map<MasterLevel, Set<Level>> masterLevels = null;

            if (group != null) {
                masterLevels = newGroupToMasterLevels.get(mapping.getGroup());
                if (masterLevels == null) {
                    masterLevels = new HashMap<>();
                    newGroupToMasterLevels.put(group, masterLevels);
                }
            }

            for (Level l : mapping.getLevels()) {
                // populate grouping map
                if (masterLevels != null) {
                    MasterLevel ml = l.getMasterLevel();
                    Set<Level> levels = masterLevels.get(ml);

                    if (levels == null) {
                        levels = new HashSet<>();
                        masterLevels.put(ml, levels);
                    }

                    levels.add(l);
                }
            }
        }
        newMappings.setGroupToMasterLevels(newGroupToMasterLevels);
    }

    @Override
    public void fileChanged(ILocalizationFile file) {
        loadMappings(this.filePath);
    }

    private static class MappingContainer {
        private Map<String, LevelMapping> keyToLevelMappings = new HashMap<>();

        private Map<Level, Collection<LevelMapping>> levelToLevelMappings = new HashMap<>();

        private Map<String, Map<MasterLevel, Set<Level>>> groupToMasterLevels = new HashMap<>();

        public MappingContainer() {
            super();
        }

        public Map<String, LevelMapping> getKeyToLevelMappings() {
            return keyToLevelMappings;
        }

        public void setKeyToLevelMappings(
                Map<String, LevelMapping> keyToLevelMappings) {
            this.keyToLevelMappings = keyToLevelMappings;
        }

        public Map<Level, Collection<LevelMapping>> getLevelToLevelMappings() {
            return levelToLevelMappings;
        }

        public void setLevelToLevelMappings(
                Map<Level, Collection<LevelMapping>> levelToLevelMappings) {
            this.levelToLevelMappings = levelToLevelMappings;
        }

        public Map<String, Map<MasterLevel, Set<Level>>> getGroupToMasterLevels() {
            return groupToMasterLevels;
        }

        public void setGroupToMasterLevels(
                Map<String, Map<MasterLevel, Set<Level>>> groupToMasterLevels) {
            this.groupToMasterLevels = groupToMasterLevels;
        }

    }
}
