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
package com.raytheon.uf.edex.stats;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import com.raytheon.uf.common.dataquery.db.QueryParam.QueryOperand;
import com.raytheon.uf.common.dataquery.db.QueryResult;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.stats.AggregateRecord;
import com.raytheon.uf.common.stats.StatsRecord;
import com.raytheon.uf.common.stats.xml.StatisticsEventConfig;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;
import com.raytheon.uf.edex.database.purge.PurgeRule;
import com.raytheon.uf.edex.database.purge.PurgeRuleSet;
import com.raytheon.uf.edex.database.query.DatabaseQuery;
import com.raytheon.uf.edex.stats.util.ConfigLoader;

/**
 * Purges the stats table of expired/unused stat records.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 21, 2012            jsanchez    Initial creation.
 * May 22, 2013 1917       rjpeter     Added purging off offline statistics.
 * Sep 04, 2014 3582       mapeters    Replaced SerializationUtil usage with SingleTypeJAXBManager.
 * May 11, 2018 19178      ryu         Purging of stats table.
 * Sep 11, 2019 7931       tgurney     Add a column alias for min date query
 * </pre>
 *
 * @author jsanchez
 *
 */
public class StatsPurge {

    /** Property key defined in stats.properties. */
    private static final String STATIC_RETENTION_HOURS = "stats.retentionHours";

    /** Default value for retention hours when property is not available */
    private static final int STATIC_RETENTION_DEFAULT = 1;

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(StatsPurge.class);

    private final CoreDao aggregateRecordDao = new CoreDao(
            DaoConfig.forClass("metadata", AggregateRecord.class));

    private final CoreDao statsRecordDao = new CoreDao(
            DaoConfig.forClass("metadata", StatsRecord.class));

    private final PurgeRuleSet aggregatePurgeRules;

    private final int statsRetentionHours;

    public StatsPurge() {
        aggregatePurgeRules = readPurgeRules("aggregatePurgeRules.xml");

        String retention = System.getProperty(STATIC_RETENTION_HOURS);
        if (retention != null) {
            statsRetentionHours = Integer.parseInt(retention);
        } else {
            statsRetentionHours = STATIC_RETENTION_DEFAULT;
        }
    }

    public void purge() {
        purgeAggregates();
        purgeStats();

        // purge offline stats
        OfflineStatsManager offlineStats = new OfflineStatsManager();
        ConfigLoader loader = ConfigLoader.getInstance();
        for (StatisticsEventConfig conf : loader.getTypeView().values()) {
            offlineStats.purgeOffline(conf);
        }
    }

    /**
     * Purges records from the aggregate table and writes them to disk.
     */
    public void purgeAggregates() {
        if (aggregatePurgeRules != null) {
            try {
                Calendar expiration = Calendar
                        .getInstance(TimeZone.getTimeZone("GMT"));
                DatabaseQuery deleteStmt = new DatabaseQuery(
                        AggregateRecord.class);
                List<PurgeRule> allRules = new ArrayList<>();

                // check for specific rules, if none, apply defaults
                if (!aggregatePurgeRules.getRules().isEmpty()) {
                    allRules.addAll(aggregatePurgeRules.getRules());
                } else if (!aggregatePurgeRules.getDefaultRules().isEmpty()) {
                    allRules.addAll(aggregatePurgeRules.getDefaultRules());
                }

                for (PurgeRule rule : allRules) {
                    if (rule.isPeriodSpecified()) {
                        long ms = rule.getPeriodInMillis();
                        int minutes = (int) (ms / TimeUtil.MILLIS_PER_MINUTE);
                        expiration.add(Calendar.MINUTE, -minutes);

                        deleteStmt.addQueryParam("endDate", expiration,
                                QueryOperand.LESSTHAN);

                        aggregateRecordDao.deleteByCriteria(deleteStmt);
                    }
                }
            } catch (DataAccessLayerException e) {
                statusHandler.error("Error purging stats aggregates", e);
            }
        }
    }

    /**
     * Purges records from the stats table if they are older than the expiration
     * time.
     */
    private void purgeStats() {
        try {
            Calendar expiration = Calendar
                    .getInstance(TimeZone.getTimeZone("GMT"));
            expiration.add(Calendar.HOUR, -statsRetentionHours);

            Calendar minTime = retrieveMinStatsTime();
            if (minTime == null) {
                return;
            }

            if (expiration.after(minTime)) {
                statusHandler.warn("Stats table records between date values of "
                        + minTime.toString() + " and " + expiration.toString()
                        + " will be purged.");
            }

            Calendar threshold = (Calendar) minTime.clone();
            while (threshold.before(expiration)) {
                threshold.add(Calendar.MINUTE, 30);
                if (threshold.after(expiration)) {
                    threshold = expiration;
                }

                DatabaseQuery deleteStmt = new DatabaseQuery(StatsRecord.class);
                deleteStmt.addQueryParam("date", threshold,
                        QueryOperand.LESSTHAN);
                statsRecordDao.deleteByCriteria(deleteStmt);
            }

        } catch (DataAccessLayerException e) {
            statusHandler.error("Error purging stats table", e);
        }
    }

    /**
     * Reads the purge files.
     */
    private PurgeRuleSet readPurgeRules(String xml) {
        PurgeRuleSet purgeRules = null;
        try {
            File file = PathManagerFactory.getPathManager()
                    .getStaticFile("purge/" + xml);
            if (file != null) {
                try {
                    SingleTypeJAXBManager<PurgeRuleSet> jaxb = new SingleTypeJAXBManager<>(
                            PurgeRuleSet.class);
                    purgeRules = jaxb.unmarshalFromXmlFile(file);

                } catch (SerializationException e) {
                    statusHandler.error(
                            "Error deserializing purge rule " + xml + "!", e);
                }

            } else {
                statusHandler.error(
                        xml + " rule not defined!!  Data will not be purged.");
            }
        } catch (Exception e) {
            statusHandler.error("Error reading purge file " + xml, e);
        }
        return purgeRules;
    }

    /**
     * Retrieves the earliest time in the stats table.
     *
     * @param
     * @return
     * @throws DataAccessLayerException
     */
    public Calendar retrieveMinStatsTime() throws DataAccessLayerException {
        String hql = "select min(rec.date) as minDate from StatsRecord rec";
        QueryResult result = statsRecordDao.executeHQLQuery(hql);
        if (result != null && result.getResultCount() > 0) {
            Object time = result.getRowColumnValue(0, "minDate");
            if (time != null) {
                return (Calendar) time;
            }
        }

        return null;
    }
}
