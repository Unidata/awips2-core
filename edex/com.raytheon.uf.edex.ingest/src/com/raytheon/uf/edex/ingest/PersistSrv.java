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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.datastorage.DuplicateRecordStorageException;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageStatus;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.time.util.ITimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.database.plugin.PluginDao;
import com.raytheon.uf.edex.database.plugin.PluginFactory;

/**
 * Performs persistence services to non-database stores
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 31, 2008            chammack    Initial creation
 * Feb 06, 2009 1990       bphillip    Refactored to use plugin specific daos
 * Nov 02, 2012 1302       djohnson    Remove unused method, fix formatting.
 * Mar 19, 2013 1785       bgonzale    Added performance status to persist.
 * Dec 17, 2015 5166       kbisanz     Update logging to use SLF4J
 * Apr 25, 2016 5604       rjpeter     Added dupElim checking by dataURI.
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */
public class PersistSrv {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final PersistSrv instance = new PersistSrv();

    public static PersistSrv getInstance() {
        return instance;
    }

    private PersistSrv() {
    }

    public PluginDataObject[] persist(PluginDataObject[] pdos) {
        if ((pdos == null) || (pdos.length == 0)) {
            return new PluginDataObject[0];
        }

        EDEXUtil.checkPersistenceTimes(pdos);

        pdos = dupElim(pdos);
        Set<PluginDataObject> pdoSet = new HashSet<>(pdos.length, 1);

        try {
            String pluginName = pdos[0].getPluginName();
            PluginDao dao = PluginFactory.getInstance()
                    .getPluginDao(pluginName);
            ITimer timer = TimeUtil.getTimer();
            timer.start();
            StorageStatus ss = dao.persistToHDF5(pdos);
            timer.stop();
            StorageException[] se = ss.getExceptions();
            pdoSet.addAll(Arrays.asList(pdos));
            if (se != null) {
                Map<PluginDataObject, StorageException> pdosThatFailed = new HashMap<>(
                        se.length, 1);
                for (StorageException s : se) {
                    IDataRecord rec = s.getRecord();

                    if (rec != null) {
                        // If we have correlation info and it's a pdo, use that
                        // for the error message...
                        Object corrObj = rec.getCorrelationObject();
                        if ((corrObj != null)
                                && (corrObj instanceof PluginDataObject)) {
                            pdosThatFailed.put((PluginDataObject) corrObj, s);
                        } else {
                            // otherwise, do the best we can with the group
                            // information
                            logger.error("Persisting record " + rec.getGroup()
                                    + "/" + rec.getName() + " failed.", s);
                        }
                    } else {
                        // All we know is something bad happened.
                        logger.error("Persistence error occurred: ", s);
                    }
                }

                // Produce error messages for each pdo that failed
                int errCnt = 0;
                boolean suppressed = false;
                for (Map.Entry<PluginDataObject, StorageException> e : pdosThatFailed
                        .entrySet()) {
                    PluginDataObject failedPdo = e.getKey();

                    if (errCnt > 50) {
                        logger.warn("More than 50 errors occurred in this batch.  The remaining errors will be suppressed.");
                        suppressed = true;
                        continue;
                    }

                    if (!suppressed) {
                        if (e.getValue() instanceof DuplicateRecordStorageException) {
                            logger.warn("Duplicate record encountered (duplicate skipped): "
                                    + failedPdo.getDataURI());

                        } else {
                            logger.error("Error persisting record " + failedPdo
                                    + " to database: ", e.getValue());
                        }
                    }

                    /*
                     * Remove from pdoSet so the pdo is not propagated to the
                     * next service
                     */
                    pdoSet.remove(failedPdo);
                    errCnt++;
                }
            }
        } catch (Throwable e1) {
            logger.error(
                    "Critical persistence error occurred.  Individual records that failed will be logged separately.",
                    e1);
            for (PluginDataObject p : pdos) {
                logger.error("Record "
                        + p
                        + " failed persistence due to critical error logged above.");
            }
        }

        return pdoSet.toArray(new PluginDataObject[pdoSet.size()]);
    }

    /**
     * Checks pdos for any duplicates based on dataURI.
     * 
     * @param pdos
     * @return
     */
    protected PluginDataObject[] dupElim(PluginDataObject[] pdos) {
        Set<String> dataUris = new HashSet<>(pdos.length, 1);

        /*
         * have to maintaint a separate list due to overwrite on the same
         * dataURI with two different objects
         */
        List<PluginDataObject> pdosToStore = new ArrayList<>(pdos.length);

        boolean pdosRemoved = false;

        /*
         * dup elim same dataURI within pdos that do not have overwrite set, for
         * partial writes to a large dataset overwrite must be set to true
         */
        for (PluginDataObject pdo : pdos) {
            String dataUri = pdo.getDataURI();

            if (dataUris.contains(dataUri) && !pdo.isOverwriteAllowed()) {
                pdosRemoved = true;
                logger.warn("Duplicate record encountered in batched persist (duplicate skipped): "
                        + pdo.getDataURI());
            } else {
                dataUris.add(dataUri);
                pdosToStore.add(pdo);
            }
        }

        if (pdosRemoved) {
            pdos = pdosToStore
                    .toArray(new PluginDataObject[pdosToStore.size()]);
        }

        return pdos;
    }
}
