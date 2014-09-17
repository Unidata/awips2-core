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
package com.raytheon.uf.common.dataplugin.level.request;

import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataplugin.level.LevelContainer;
import com.raytheon.uf.common.dataplugin.level.MasterLevel;
import com.raytheon.uf.common.dataplugin.level.MasterLevelContainer;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.serialization.comm.RequestRouter;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Retrieves levels known to the system
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * --/--/----              ???         Initial creation.
 * Sep 09, 2014 3356       njensen     Moved to common and updated accordingly
 * 
 * </pre>
 * 
 * @version 1.0
 */
public class LevelRetrievalAdapter implements ILevelRetrievalAdapter {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(LevelRetrievalAdapter.class);

    @Override
    public Level getLevel(GetLevelRequest request) {
        Level rval = null;

        try {
            rval = (Level) RequestRouter.route(request);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error occurred retrieving level information from server.",
                    e);
        }

        return rval;
    }

    @Override
    public Level getLevel(GetLevelByIdRequest request) {
        Level rval = null;

        try {
            rval = (Level) RequestRouter.route(request);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error occurred retrieving level information from server.",
                    e);
        }

        return rval;
    }

    @Override
    public MasterLevel getMasterLevel(GetMasterLevelRequest request) {
        MasterLevel rval = null;
        try {
            rval = (MasterLevel) RequestRouter.route(request);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error occurred retrieving level information from server.",
                    e);
        }
        return rval;
    }

    @Override
    public LevelContainer getAllLevelsForMasterLevel(
            GetAllLevelsForMasterLevelRequest request) {
        LevelContainer rval = null;
        try {
            rval = (LevelContainer) RequestRouter.route(request);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error occurred retrieving level information from server.",
                    e);
        }
        return rval;
    }

    @Override
    public LevelContainer getAllLevels() {
        LevelContainer rval = null;
        DbQueryRequest query = new DbQueryRequest();
        query.setConstraints(new HashMap<String, RequestConstraint>());
        query.setEntityClass(Level.class.getName());
        try {
            DbQueryResponse resp = (DbQueryResponse) RequestRouter.route(query);
            rval = new LevelContainer(resp.getResults().size());
            for (Map<String, Object> result : resp.getResults()) {
                rval.add((Level) result.get(null));
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error occurred retrieving level information from server.",
                    e);
        }
        return rval;
    }

    @Override
    public MasterLevelContainer getAllMasterLevels() {
        MasterLevelContainer rval = null;
        DbQueryRequest query = new DbQueryRequest();
        query.setConstraints(new HashMap<String, RequestConstraint>());
        query.setEntityClass(MasterLevel.class.getName());
        try {
            DbQueryResponse resp = (DbQueryResponse) RequestRouter.route(query);
            rval = new MasterLevelContainer(resp.getResults().size());
            for (Map<String, Object> result : resp.getResults()) {
                rval.add((MasterLevel) result.get(null));
            }
        } catch (Exception e) {
            statusHandler
                    .handle(Priority.PROBLEM,
                            "Error occurred retrieving master level information from server.",
                            e);
        }
        return rval;
    }
}
