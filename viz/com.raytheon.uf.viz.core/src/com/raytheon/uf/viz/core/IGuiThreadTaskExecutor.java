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
package com.raytheon.uf.viz.core;

/**
 * Deprecated: Use the methods on VizApp instead.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jan 02, 2013  1449     djohnson  Initial creation
 * Nov 03, 2016  5976     bsteffen  Deprecate
 * 
 * </pre>
 * 
 * @deprecated
 * 
 * @author djohnson
 */
@Deprecated
public interface IGuiThreadTaskExecutor {

    /**
     * @deprecated Use {@link VizApp#runAsync(Runnable)}
     */
    @Deprecated
    void runAsync(Runnable aTask);

    /**
     * @deprecated Use {@link VizApp#runSync(Runnable)}
     */
    @Deprecated
    void runSync(Runnable task);

    /**
     * @deprecated USe {@link VizApp#runSyncIfWorkbench(Runnable)}
     */
    @Deprecated
    void runSyncIfWorkbench(Runnable task);
}
