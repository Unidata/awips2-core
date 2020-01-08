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
package com.raytheon.uf.edex.database.handlers;

import java.util.List;

import com.raytheon.uf.common.dataquery.requests.DbCreateRequest;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;

/**
 * Handler for creating objects in the DB
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket# Engineer    Description
 * ------------ ------- ----------- --------------------------
 * Nov 04, 2019 7960    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class DbCreateHandler
        implements IRequestHandler<DbCreateRequest> {

    @Override
    public Object handleRequest(DbCreateRequest request) {
        String dbName = request.getDbName();
        if (dbName == null) {
            throw new IllegalArgumentException("Database name cannot be null");
        }

        CoreDao dao = new CoreDao(DaoConfig.forDatabase(dbName));
        List<Object> objs = request.getObjectsToCreate();

        if (objs != null) {
            dao.createAll(objs);
        }

        return null;
    }
}
