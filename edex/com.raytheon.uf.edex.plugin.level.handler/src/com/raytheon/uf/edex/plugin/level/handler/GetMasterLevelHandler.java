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
package com.raytheon.uf.edex.plugin.level.handler;

import com.raytheon.uf.common.dataplugin.level.MasterLevel;
import com.raytheon.uf.common.dataplugin.level.request.GetMasterLevelRequest;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.plugin.level.dao.LevelDao;

/**
 * IRequestHandler that returns a master level
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 24, 2009 2924       rjpeter     Initial creation
 * Sep 09, 2014 3356       njensen     Reimplemented handleRequest
 * Sep 16, 2014 3356       njensen     Removed dependency on LevelFactory
 * 
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */

public class GetMasterLevelHandler implements
        IRequestHandler<GetMasterLevelRequest> {

    @Override
    public MasterLevel handleRequest(GetMasterLevelRequest request)
            throws Exception {
        LevelDao dao = new LevelDao();
        return dao.lookupMasterLevel(request.getMasterLevel(),
                request.isCreate());
    }
}
