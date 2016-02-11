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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */
public class DatabaseBloatMonitor implements DatabaseMonitor {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final BloatDao dao;

    protected final List<Threshold> warningThresholds;

    protected final List<Threshold> criticalThresholds;

    public DatabaseBloatMonitor(BloatDao dao,
            List<Threshold> warningThresholds,
            List<Threshold> criticalThresholds) {
        this.dao = dao;
        this.warningThresholds = warningThresholds;
        this.criticalThresholds = criticalThresholds;

    }

    @Override
    public String getDatabase() {
        return dao.getDatabase();
    }

    @Override
    public void runMonitor() {
        checkTables();
        checkIndexes();
    }

    protected void checkTables() {
        logger.info("Running database bloat monitor on [" + getDatabase()
                + "] tables");
        List<TableBloat> tableBloatData = dao.getTableBloatData();

        List<TableBloat> criticalLevel = filter(tableBloatData,
                criticalThresholds);
        List<TableBloat> warningLevel = filter(tableBloatData,
                warningThresholds);

        // print warning level messages
        for (TableBloat info : warningLevel) {
            logger.warn(String
                    .format("Database [%s] Table [%s] has reached bloat WARNING threshold.  Table Size [%s], Bloat amount [%s], Bloat Percentage [%.2f]",
                            getDatabase(), info.getTableName(),
                            SizeUtil.prettyByteSize(info.getRealSizeBytes()),
                            SizeUtil.prettyByteSize(info.getBloatBytes()),
                            info.getBloatPercent()));
        }

        for (TableBloat info : criticalLevel) {
            if (EDEXUtil.isRunning()) {
                logger.error(String
                        .format("Database [%s] Table [%s] has reached bloat CRITICAL threshold.  Table Size [%s], Bloat amount [%s], Bloat Percentage [%.2f].  Full vacuum recommended!",
                                getDatabase(),
                                info.getTableName(),
                                SizeUtil.prettyByteSize(info.getRealSizeBytes()),
                                SizeUtil.prettyByteSize(info.getBloatBytes()),
                                info.getBloatPercent()));
                /* TODO: If it goes too far, should this be done automatically? */
                // vacuumTable(info);
            }
        }
    }

    protected void checkIndexes() {
        logger.info("Running database bloat monitor on [" + getDatabase()
                + "] indexes");
        List<IndexBloat> indexBloatData = dao.getIndexBloatData();

        List<IndexBloat> criticalLevel = filter(indexBloatData,
                criticalThresholds);
        List<IndexBloat> warningLevel = filter(indexBloatData,
                warningThresholds);

        // print warning level messages
        for (IndexBloat info : warningLevel) {
            logger.warn(String
                    .format("Database [%s] Index [%s] on Table [%s] has reached bloat WARNING threshold.  Index Size [%s], Bloat amount [%s], Bloat Percentage [%.2f]",
                            getDatabase(), info.getIndexName(),
                            info.getTableName(),
                            SizeUtil.prettyByteSize(info.getRealSizeBytes()),
                            SizeUtil.prettyByteSize(info.getBloatBytes()),
                            info.getBloatPercent()));
        }

        for (IndexBloat info : criticalLevel) {
            if (EDEXUtil.isRunning()) {
                logger.error(String
                        .format("Database [%s] Index [%s] on Table [%s] has reached bloat CRITICAL threshold.  Index Size [%s], Bloat amount [%s], Bloat Percentage [%.2f].  Reindexing...",
                                getDatabase(),
                                info.getIndexName(),
                                info.getTableName(),
                                SizeUtil.prettyByteSize(info.getRealSizeBytes()),
                                SizeUtil.prettyByteSize(info.getBloatBytes()),
                                info.getBloatPercent()));
                try {
                    dao.reindex(info);
                } catch (Exception e) {
                    logger.error(
                            "Error occurred reindexing " + info.getIndexName(),
                            e);
                }
            }
        }
    }

    /**
     * Scan bloatList for items that meet or exceed the thresholds. The items
     * are removed from bloatList.
     * 
     * @param bloatList
     * @param thresholds
     * @return
     */
    protected <T extends TableBloat> List<T> filter(List<T> bloatList,
            List<Threshold> thresholds) {
        List<T> rval = new LinkedList<>();

        Iterator<T> iter = bloatList.iterator();
        while (iter.hasNext()) {
            T info = iter.next();
            long sizeBytes = info.getRealSizeBytes();
            double percent = info.getBloatPercent();
            for (Threshold threshold : thresholds) {
                if ((sizeBytes >= threshold.getSizeInBytes())
                        && (percent >= threshold.getPercent())) {
                    rval.add(info);
                    iter.remove();
                    break;
                }
            }
        }

        return rval;
    }
}
