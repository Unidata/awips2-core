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
package com.raytheon.uf.edex.database.health;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.edex.core.EDEXUtil;

/**
 * Monitor for checking bloat in database tables and indexes.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 10, 2016 4630       rjpeter     Initial creation
 * Sep 08, 2017 DR 20135   D. Friedman Add system property to enable index rebuilding
 * Aug 21, 2018 DR 20505   tjensen     Exclude temp indexes from reindexing
 * Jul 25, 2019 7840       mroos       Update to allow configurable bloat threshold options
 *
 * </pre>
 *
 * @author rjpeter
 */
public class DatabaseBloatMonitor implements DatabaseMonitor {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final BloatDao dao;

    protected static final String REINDEXING_ENABLED_PROPERTY = "database.health.reindex.enable";

    protected static final String CONFIG = "bloatMonitor/thresholds/";

    protected List<BloatThresholds> allThresholds;

    private static JAXBManager jaxb;

    public DatabaseBloatMonitor(BloatDao dao) {
        this.dao = dao;
    }

    @Override
    public String getDatabase() {
        return dao.getDatabase();
    }

    @Override
    public void runMonitor() {
        logger.info("Running Database Bloat Monitor: ");
        this.allThresholds = loadThresholds();
        checkTables();
        checkIndexes();
        logger.info("Finished running Database Bloat Monitor.");
    }

    protected void checkTables() {
        logger.info("Running database bloat monitor on [" + getDatabase()
                + "] tables");
        List<TableBloat> tableBloatData = dao.getTableBloatData();
        String database = dao.getDatabase();
        List<TableBloat> criticalLevel = filter(tableBloatData, allThresholds,
                "crit", database);
        List<TableBloat> warningLevel = filter(tableBloatData, allThresholds,
                "warn", database);

        // print warning level messages
        for (TableBloat info : warningLevel) {
            logger.warn(String.format(
                    "Database [%s] Table [%s] has reached bloat WARNING threshold.  Table Size [%s], Bloat amount [%s], Bloat Percentage [%.2f]",
                    getDatabase(), info.getTableName(),
                    SizeUtil.prettyByteSize(info.getRealSizeBytes()),
                    SizeUtil.prettyByteSize(info.getBloatBytes()),
                    info.getBloatPercent()));
        }

        for (TableBloat info : criticalLevel) {
            if (EDEXUtil.isRunning()) {
                logger.warn(String.format(
                        "Database [%s] Table [%s] has reached bloat CRITICAL threshold.  Table Size [%s], Bloat amount [%s], Bloat Percentage [%.2f].  Full vacuum recommended!",
                        getDatabase(), info.getTableName(),
                        SizeUtil.prettyByteSize(info.getRealSizeBytes()),
                        SizeUtil.prettyByteSize(info.getBloatBytes()),
                        info.getBloatPercent()));
                /*
                 * TODO: If it goes too far, should this be done automatically?
                 */
                // vacuumTable(info);
            }
        }
    }

    protected void checkIndexes() {
        logger.info("Running database bloat monitor on [" + getDatabase()
                + "] indexes");
        List<IndexBloat> indexBloatData = dao.getIndexBloatData();
        String database = dao.getDatabase();

        List<IndexBloat> criticalLevel = filter(indexBloatData, allThresholds,
                "crit", database);
        List<IndexBloat> warningLevel = filter(indexBloatData, allThresholds,
                "warn", database);
        boolean reindex = Boolean.parseBoolean(
                System.getProperty(REINDEXING_ENABLED_PROPERTY, "true"));

        // print warning level messages
        for (IndexBloat info : warningLevel) {
            logger.warn(String.format(
                    "Database [%s] Index [%s] on Table [%s] has reached bloat WARNING threshold.  Index Size [%s], Bloat amount [%s], Bloat Percentage [%.2f]",
                    getDatabase(), info.getIndexName(), info.getTableName(),
                    SizeUtil.prettyByteSize(info.getRealSizeBytes()),
                    SizeUtil.prettyByteSize(info.getBloatBytes()),
                    info.getBloatPercent()));
        }

        for (IndexBloat info : criticalLevel) {
            if (EDEXUtil.isRunning()) {
                if (!info.getIndexName()
                        .startsWith(BloatDao.TMP_INDEX_PREFIX)) {
                    String action = reindex ? "Reindexing..."
                            : "Reindexing is disabled.  Manual reindex recommended.";
                    logger.warn(String.format(
                            "Database [%s] Index [%s] on Table [%s] has reached bloat CRITICAL threshold.  Index Size [%s], Bloat amount [%s], Bloat Percentage [%.2f].  %s",
                            getDatabase(), info.getIndexName(),
                            info.getTableName(),
                            SizeUtil.prettyByteSize(info.getRealSizeBytes()),
                            SizeUtil.prettyByteSize(info.getBloatBytes()),
                            info.getBloatPercent(), action));
                    if (reindex) {
                        try {
                            dao.reindex(info);
                            logger.info("REINDEX of index '"
                                    + info.getIndexName() + "' queued.");
                        } catch (Exception e) {
                            logger.error("Error occurred reindexing "
                                    + info.getIndexName(), e);
                        }
                    }
                } else {
                    logger.warn(
                            "Temporary indexes do not need reindexed: Skipping reindex of"
                                    + info.getIndexName());

                }
            }
        }
    }

    /**
     * Scan bloatList for items that meet or exceed the thresholds. The items
     * are removed from bloatList.
     *
     * @param bloatList
     *            list of bloatList items to loop through (check all
     *            tables/indices)
     * @param bloatThresh
     *            list of all BloatThresholds items to find the correct
     *            thresholds from
     * @param bloatType
     *            whether the percentage to check is critical or warning (only
     *            values allowed are "warn" and "crit")
     * @param database
     *            the database the table/index to scan is inside of
     * @return object containing the table/index information as well as what
     *         bloat threshold it hit, if any
     */
    protected <T extends TableBloat> List<T> filter(List<T> bloatList,
            List<BloatThresholds> bloatThresh, String bloatType,
            String database) {
        List<T> rval = new LinkedList<>();

        Iterator<T> iter = bloatList.iterator();
        while (iter.hasNext()) {
            T info = iter.next();
            long sizeBytes = info.getRealSizeBytes();
            double percent = info.getBloatPercent();
            // Get the correct threshold list to loop over table/index
            List<Threshold> thresholds = getThresholds(info, database,
                    bloatThresh);
            // If no thresholds, no need for warnings
            if (thresholds == null) {
                return rval;
            }
            // Check table/index against the thresholds within the list
            for (Threshold threshold : thresholds) {
                /*
                 * Determine which percent value to grab from the threshold
                 * list; default of 100% is a kind of exception handling in the
                 * case that neither kind is specified - this will cause it to
                 * only delete empty tables. You should not use the default for
                 * 100% bloat - use configuration files instead.
                 */
                double percentCheck = 100;
                if (bloatType == "warn") {
                    percentCheck = threshold.getWarningPercent();
                } else if (bloatType == "crit") {
                    percentCheck = threshold.getCriticalPercent();
                }

                /*
                 * Check if the size of the table/index is larger than the size
                 * of the threshold, and if its bloat percentage is above the
                 * given warning percentage
                 */
                if ((sizeBytes >= (threshold.getSizeInBytes()))
                        && (percent >= percentCheck)) {
                    logger.info(bloatType + "threshold reached; size: "
                            + threshold.getSize() + "percent: " + percentCheck);
                    rval.add(info);
                    iter.remove();
                    break;
                }
            }
        }

        return rval;
    }

    /**
     * Get all the localization files for this catalog.
     * 
     * @return the localization files for the procedures.
     */
    protected LocalizationFile[] getLocalizationFiles() {
        LocalizationFile[] procFiles = PathManagerFactory.getPathManager()
                .listFiles(
                        PathManagerFactory.getPathManager()
                                .getLocalSearchHierarchy(
                                        LocalizationType.COMMON_STATIC),
                        CONFIG, new String[] { ".xml" }, false, true);
        return procFiles;
    }

    /**
     * Load the proper threshold xml file for the BloatMonitor and populate the
     * values into the allThresholds list.
     * 
     * @return the set of all BloatThreshold objects (all threshold lists to use
     *         for checking table bloat)
     */
    protected List<BloatThresholds> loadThresholds() {
        // Get the list of all xml configuration files for thresholds
        LocalizationFile[] fullList = getLocalizationFiles();
        // Create Set to hold the unmarshalled BloatThreshold classes from the
        // xml
        List<BloatThresholds> finalList = new ArrayList<>();
        // Loop over the files and unmarshal each one into a BloatThresholds
        // object
        for (LocalizationFile loc : fullList) {
            try (InputStream is = loc.openInputStream()) {
                BloatThresholds th = (BloatThresholds) getJaxbManager()
                        .unmarshalFromInputStream(is);
                // Add to the list of BloatThresholds
                finalList.add(th);
                // Error handling
            } catch (IOException e) {
                logger.error("IOException while processing file " + loc, e);
            } catch (LocalizationException e) {
                logger.error(
                        "LocalizationException while processing file " + loc,
                        e);
            } catch (SerializationException e) {
                logger.error("SerializationException in file " + loc, e);
            } catch (JAXBException e) {
                logger.error("JAXBException in file " + loc, e);
            }
        }
        return finalList;
    }

    /**
     * Returns the JAXBManager used for unmarshalling the thresholds. Sets the
     * JAXBManager if null.
     * 
     * @return The JAXBManager
     * @throws JAXBException
     */
    private static synchronized JAXBManager getJaxbManager()
            throws JAXBException {
        if (jaxb == null) {
            jaxb = new JAXBManager(BloatThresholds.class, Threshold.class);
        }
        return jaxb;
    }

    /**
     * For a given table, this pulls the lowest-level Threshold list to apply to
     * that table.
     * 
     * @param info
     *            the TableBloat object for a specific table to correspond to a
     *            threshold list
     * @param database
     *            the name of the database the table resides in
     * @param bloatThresh
     *            the list of all BloatThreshold items to search through for
     *            applying to the requisite table
     * @return the single list of thresholds to apply to the given table
     */
    protected <T extends TableBloat> List<Threshold> getThresholds(T info,
            String database, List<BloatThresholds> bloatThresh) {
        // Create a set to hold the BloatThresholds with matching schema, table
        // name, and database
        List<BloatThresholds> allMatches = new ArrayList<>();
        // Loop through the set of all BloatThresholds to find the matches
        for (BloatThresholds bt : bloatThresh) {
            if (bt.matches(database, info.getSchema(), info.getTableName())) {
                allMatches.add(bt);
            }
        }
        BloatThresholds match = null;
        // Grab the Threshold list with the highest priority
        for (BloatThresholds prio : allMatches) {
            if (match == null) {
                match = prio;
            } else {
                if (prio.getPriority() > match.getPriority()) {
                    match = prio;
                }
            }
        }
        logger.info("Threshold ID " + match.getId() + " applied to table "
                + info.getTableName());
        // Return the list to start looping
        return match.getThresholdList();
    }
}
