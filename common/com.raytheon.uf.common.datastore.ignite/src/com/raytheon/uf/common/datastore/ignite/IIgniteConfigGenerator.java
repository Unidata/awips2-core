/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastore.ignite;

import org.apache.ignite.configuration.IgniteConfiguration;

/**
 * Interface for generating unique ignite config instances.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 1, 2021  8450       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IIgniteConfigGenerator {

    /**
     * Generates a fully new config instance for starting a new ignite instance
     * with. Shared state between config instances can cause issues.
     *
     * @return a unique config instance
     */
    IgniteConfiguration getNewConfig();
}
