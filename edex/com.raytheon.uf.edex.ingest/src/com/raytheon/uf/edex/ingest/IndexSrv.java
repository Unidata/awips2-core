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

package com.raytheon.uf.edex.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.time.util.ITimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.database.plugin.PluginDao;
import com.raytheon.uf.edex.database.plugin.PluginFactory;

/**
 * Receives events from the file endpoint.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *                         fgriffit    Initial Creation.
 * Apr 08, 2008 1039       jkorman     Added traceId for tracing data.
 * Nov 11, 2008            chammack    Refactored for Camel
 * Feb 06, 2009 1990       bphillip    Refactored to use plugin daos
 * Mar 19, 2013 1785       bgonzale    Added performance status to indexOne and index.
 * Dec 17, 2015 5166       kbisanz     Update logging to use SLF4J
 * Feb 22, 2022 8608       mapeters    Add auditMissingPiecesForDatabaseOnlyPdos()
 * </pre>
 *
 * @author Frank Griffith
 */
public class IndexSrv {

    private String sessionFactory;

    private String txFactory;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler("DataBase:");

    /** The default constructor */
    public IndexSrv() {
    }

    /** Addtional constructor */
    public IndexSrv(String dbname) {
    }

    /**
     * Index a single record
     *
     * Return null if the indexing was not successful, else return the record.
     *
     * @param record
     *            the record
     * @return the record, else null if indexing failed
     * @throws PluginException
     */
    public PluginDataObject indexOne(PluginDataObject record)
            throws PluginException {
        String pluginName = record.getPluginName();
        PluginDao dao = PluginFactory.getInstance().getPluginDao(pluginName);
        ITimer timer = TimeUtil.getTimer();
        timer.start();
        dao.persistToDatabase(record);
        timer.stop();
        perfLog.logDuration(pluginName + ": Saved a record: Time to Save",
                timer.getElapsedTime());
        if (logger.isDebugEnabled()) {
            logger.debug("Persisted: " + record + " to database");
        }
        return record;
    }

    /**
     * Index all records in an array
     *
     * Return the list of records that were successfully persisted
     *
     * @param record
     *            a record array
     * @return the list of objects that were successfully persisted
     * @throws PluginException
     */
    public PluginDataObject[] index(PluginDataObject[] record)
            throws PluginException {

        if (record == null || record.length == 0) {
            return new PluginDataObject[0];
        }

        try {
            String pluginName = record[0].getPluginName();
            PluginDao dao = PluginFactory.getInstance()
                    .getPluginDao(pluginName);
            EDEXUtil.checkPersistenceTimes(record);
            ITimer timer = TimeUtil.getTimer();
            timer.start();
            PluginDataObject[] persisted = dao.persistToDatabase(record);
            timer.stop();
            perfLog.logDuration(
                    pluginName + ": Saved " + persisted.length
                            + " record(s): Time to Save",
                    timer.getElapsedTime());
            if (logger.isDebugEnabled()) {
                for (PluginDataObject rec : record) {
                    logger.debug("Persisted: " + rec + " to database");
                }
            }

            return persisted;
        } catch (Throwable e) {
            logger.error("Error occurred during persist", e);
            return new PluginDataObject[0];
        }
    }

    /**
     * This should be called for PDOs that only store metadata to the database,
     * and have no associated data store values.
     *
     * The data store route normally audits the metadata ID, data ID, and data
     * status, so this generates and sends those pieces to the auditer.
     *
     * @param pdos
     *            the plugin data objects to audit
     * @return the given PDOs (to support calling this in a spring route)
     */
    public PluginDataObject[] auditMissingPiecesForDatabaseOnlyPdos(
            PluginDataObject[] pdos) {
        if (pdos == null || pdos.length == 0) {
            return new PluginDataObject[0];
        }

        try {
            String pluginName = pdos[0].getPluginName();
            PluginDao dao = PluginFactory.getInstance()
                    .getPluginDao(pluginName);
            dao.auditMissingPiecesForDatabaseOnlyPdos(pdos);
        } catch (Throwable e) {
            logger.error(
                    "Error occurred auditing missing pieces for database-only PDOs: "
                            + pdos,
                    e);
        }

        return pdos;
    }

    public void dispose() {
    }

    public String getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(String sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public String getTxFactory() {
        return txFactory;
    }

    public void setTxFactory(String txFactory) {
        this.txFactory = txFactory;
    }

}
