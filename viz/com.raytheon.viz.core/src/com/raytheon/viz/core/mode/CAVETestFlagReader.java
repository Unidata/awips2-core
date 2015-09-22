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

package com.raytheon.viz.core.mode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Manages the life cycle of the GFE Perspectives
 *
 * Installs a perspective watcher that handles the transitions in and out of the
 * GFE perspectives.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Aug 31, 2015 #17970      yteng       Initial creation
 * </pre>
 *
 * @author yteng
 * @version 1.0
 */


public class CAVETestFlagReader {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(CAVETestFlagReader.class);

    private static String FLAG_NAME = "ENABLE_DRT_TEST";

    private boolean testFlag = false;

    public CAVETestFlagReader(String filepath, String filename) {

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(new File(filepath, filename)));
        } catch (FileNotFoundException e) {
            statusHandler.handle(Priority.INFO,
                    "Optional file " + filename + " under " + filepath + " doesn't exist");
            return;
        }

        try {
            String line = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(FLAG_NAME)) {
                    if (line.endsWith("true")) {
                        testFlag = true;
                        br.close();
                        return;
                    }
                    br.close();
                    return;  // default to false
                }
            }
            br.close();
        } catch(IOException ex) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error in parsing file " + filename);
        }
    }

    public boolean getTestFlag() {
        return testFlag;
    }
}
