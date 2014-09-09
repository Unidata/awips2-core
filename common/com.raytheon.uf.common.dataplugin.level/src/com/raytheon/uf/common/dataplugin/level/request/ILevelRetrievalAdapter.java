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

import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataplugin.level.LevelContainer;
import com.raytheon.uf.common.dataplugin.level.MasterLevel;
import com.raytheon.uf.common.dataplugin.level.MasterLevelContainer;

/**
 * Interface to load a level from.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 03, 2009            rjpeter     Initial creation.
 * Sep 09, 2014 3356       njensen     Removed CommunicationException
 * 
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */
public interface ILevelRetrievalAdapter {

    public Level getLevel(GetLevelRequest request);

    public Level getLevel(GetLevelByIdRequest request);

    public MasterLevel getMasterLevel(GetMasterLevelRequest request);

    public LevelContainer getAllLevelsForMasterLevel(
            GetAllLevelsForMasterLevelRequest request);

    public LevelContainer getAllLevels();

    public MasterLevelContainer getAllMasterLevels();
}
