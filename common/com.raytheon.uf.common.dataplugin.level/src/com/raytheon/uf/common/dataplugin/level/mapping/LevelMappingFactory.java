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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.JAXB;

import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataplugin.level.MasterLevel;
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
 * 
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */
public class LevelMappingFactory {
    // TODO: this should move somewhere else
    public static final String VOLUMEBROWSER_LEVEL_MAPPING_FILE = "volumebrowser/LevelMappingFile.xml";

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(LevelMappingFactory.class);

    private static Map<String, LevelMappingFactory> instanceMap = new HashMap<String, LevelMappingFactory>();

    private Map<String, LevelMapping> keyToLevelMappings = new HashMap<String, LevelMapping>();

    private volatile boolean levelToLevelMappingsInitialized = false;

    private ReentrantReadWriteLock levelToLevelLock = new ReentrantReadWriteLock();

    private Map<Level, Collection<LevelMapping>> levelToLevelMappings = new HashMap<Level, Collection<LevelMapping>>();

    private volatile boolean groupToMasterLevelsInitialized = false;

    private ReentrantReadWriteLock groupToMasterLevelsLock = new ReentrantReadWriteLock();

    private Map<String, Map<MasterLevel, Set<Level>>> groupToMasterLevels = new HashMap<String, Map<MasterLevel, Set<Level>>>();

    public synchronized static LevelMappingFactory getInstance(String filePath) {
        LevelMappingFactory instance = instanceMap.get(filePath);
        if (instance == null) {
            instance = new LevelMappingFactory(filePath);
            instanceMap.put(filePath, instance);
        }
        return instance;
    }

    private LevelMappingFactory(String filePath) {
        File path = PathManagerFactory.getPathManager().getStaticFile(filePath);
        LevelMappingFile levelMapFile = null;
        long start = System.currentTimeMillis();
        try {
            levelMapFile = JAXB.unmarshal(path, LevelMappingFile.class);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "An error was encountered while creating the LevelNameMappingFile from "
                            + path.toString(), e);
        }

        List<LevelMapping> levelMappings = levelMapFile.getLevelMappingFile();

        if (levelMappings != null && levelMappings.size() > 0) {
            for (LevelMapping mapping : levelMappings) {
                if (keyToLevelMappings.containsKey(mapping.getKey())) {
                    // handle multiple entries to same key by appending levels
                    LevelMapping priorEntry = keyToLevelMappings.get(mapping
                            .getKey());
                    priorEntry.getDatabaseLevels().addAll(
                            mapping.getDatabaseLevels());
                } else {
                    keyToLevelMappings.put(mapping.getKey(), mapping);
                }
            }

        }

        initializeLevelToLevelMappings();
        initializeGroupToMasterLevels();

        long finish = System.currentTimeMillis();
        System.out.println("LevelMappingFactory initialization took ["
                + (finish - start) + "] ms");
    }

    public LevelMapping getLevelMappingForKey(String key) {
        return keyToLevelMappings.get(key);
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
        if (!levelToLevelMappingsInitialized) {
            initializeLevelToLevelMappings();
        }

        levelToLevelLock.readLock().lock();
        try {
            Collection<LevelMapping> mappings = levelToLevelMappings.get(level);
            if (mappings == null) {
                return null;
            } else {
                return Collections.unmodifiableCollection(mappings);
            }
        } finally {
            levelToLevelLock.readLock().unlock();
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
        if (!levelToLevelMappingsInitialized) {
            initializeLevelToLevelMappings();
        }

        levelToLevelLock.readLock().lock();
        try {
            Collection<LevelMapping> mappings = levelToLevelMappings.get(level);
            if (mappings == null || mappings.isEmpty()) {
                return null;
            } else {
                Iterator<LevelMapping> it = mappings.iterator();
                LevelMapping retVal = it.next();
                while (it.hasNext()) {
                    LevelMapping test = it.next();
                    if (test.getLevels().size() < retVal.getLevels().size()) {
                        /*
                         * Only replace the old level mapping if we have less
                         * levels than the old mapping This should cause the
                         * most specific mapping to be used
                         */
                        retVal = test;
                    }
                }
                return retVal;
            }
        } finally {
            levelToLevelLock.readLock().unlock();
        }
    }

    public Collection<LevelMapping> getAllLevelMappings() {
        return keyToLevelMappings.values();
    }

    public Set<Level> getAllLevels() {
        if (!levelToLevelMappingsInitialized) {
            initializeLevelToLevelMappings();
        }

        levelToLevelLock.readLock().lock();
        try {
            Set<Level> retVal = levelToLevelMappings.keySet();
            return retVal;
        } finally {
            levelToLevelLock.readLock().unlock();
        }
    }

    public Map<MasterLevel, Set<Level>> getLevelMapForGroup(String group) {
        if (!groupToMasterLevelsInitialized) {
            initializeGroupToMasterLevels();
        }

        groupToMasterLevelsLock.readLock().lock();
        try {
            Map<MasterLevel, Set<Level>> retVal = groupToMasterLevels
                    .get(group);
            return retVal;
        } finally {
            groupToMasterLevelsLock.readLock().unlock();
        }
    }

    private void initializeLevelToLevelMappings() {
        // acquire the write lock
        levelToLevelLock.writeLock().lock();
        try {
            // verify some other thread hasn't already initialized
            if (!levelToLevelMappingsInitialized) {
                for (LevelMapping mapping : keyToLevelMappings.values()) {
                    for (Level l : mapping.getLevels()) {
                        Collection<LevelMapping> mappings = levelToLevelMappings
                                .get(l);
                        if (mappings == null) {
                            mappings = new ArrayList<>(1);
                            levelToLevelMappings.put(l, mappings);
                        }
                        mappings.add(mapping);
                    }
                }
                levelToLevelMappingsInitialized = true;
            }
        } finally {
            // release the write lock
            levelToLevelLock.writeLock().unlock();
        }
    }

    private void initializeGroupToMasterLevels() {
        // acquire the write lock
        groupToMasterLevelsLock.writeLock().lock();
        try {
            // verify some other thread hasn't already initialized
            if (!groupToMasterLevelsInitialized) {
                for (LevelMapping mapping : keyToLevelMappings.values()) {
                    String group = mapping.getGroup();
                    Map<MasterLevel, Set<Level>> masterLevels = null;

                    if (group != null) {
                        masterLevels = groupToMasterLevels.get(mapping
                                .getGroup());
                        if (masterLevels == null) {
                            masterLevels = new HashMap<MasterLevel, Set<Level>>();
                            groupToMasterLevels.put(group, masterLevels);
                        }
                    }

                    for (Level l : mapping.getLevels()) {

                        // populate grouping map
                        if (masterLevels != null) {
                            MasterLevel ml = l.getMasterLevel();
                            Set<Level> levels = masterLevels.get(ml);

                            if (levels == null) {
                                levels = new HashSet<Level>();
                                masterLevels.put(ml, levels);
                            }

                            levels.add(l);
                        }
                    }
                }
                groupToMasterLevelsInitialized = true;
            }
        } finally {
            // release the write lock
            groupToMasterLevelsLock.writeLock().unlock();
        }
    }
}
