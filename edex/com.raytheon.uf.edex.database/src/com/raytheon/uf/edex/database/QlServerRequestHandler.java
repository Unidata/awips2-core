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
package com.raytheon.uf.edex.database;

import com.raytheon.uf.common.dataquery.requests.QlServerRequest;
import com.raytheon.uf.common.dataquery.requests.QlServerRequest.QueryLanguage;
import com.raytheon.uf.common.dataquery.requests.QlServerRequest.QueryType;
import com.raytheon.uf.common.message.response.ResponseMessageGeneric;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;

/**
 * Handler for QlServerRequest objects
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 16, 2011 #8070      ekladstrup  Initial creation
 * Nov 08, 2013  2361      njensen     Removed saveOrUpdate mode
 * Jul 13, 2015 4500       rjpeter     Fix SQL Injection concerns.
 * </pre>
 * 
 * @author ekladstrup
 * @version 1.0
 */
public class QlServerRequestHandler implements IRequestHandler<QlServerRequest> {
    /**
     * 
     */
    @Override
    public Object handleRequest(QlServerRequest request) throws Exception {

        // get database name
        String dbName = request.getDatabase();
        if (dbName == null) {
            dbName = DaoConfig.DEFAULT_DB_NAME;
        }

        Object result = null;
        CoreDao dao = new CoreDao(DaoConfig.forDatabase(dbName));
        if (QueryLanguage.HQL.equals(request.getLang())) {
            if (QueryType.STATEMENT.equals(request.getType())) {
                result = dao.executeHQLStatement(request.getQuery(),
                        request.getParamMap());
            } else {
                result = dao.executeHQLQuery(request.getQuery(),
                        request.getParamMap());
            }
        } else {
            if (QueryType.STATEMENT.equals(request.getType())) {
                result = dao.executeSQLUpdate(request.getQuery(),
                        request.getParamMap());
            } else {
                result = dao.executeMappedSQLQuery(request.getQuery(),
                        request.getParamMap());
            }
        }

        // instead of placing a single value in an arraylist, just return the
        // single item
        ResponseMessageGeneric rval = new ResponseMessageGeneric(result);

        return rval;
    }

}
