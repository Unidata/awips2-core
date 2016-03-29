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
package com.raytheon.uf.common.localization;

/**
 * Observer to listen for changes on paths corresponding to ILocalizationFiles.
 * These observers are registered on an IPathManager instance and will be
 * triggered whenever an ILocalizationFile with a matching path is changed.
 * 
 * Please note that the IPathManager will trigger the event regardless of
 * LocalizationContext. It is the responsibility of implementations to determine
 * if any action needs to be taken based on the context or other attributes of
 * the ILocalizationFile. This is intentional to encourage support of concepts
 * such as incremental overrides where multiple localization levels must be used
 * together and watched.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 12, 2015  4834      njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public interface ILocalizationPathObserver {

    public void fileChanged(ILocalizationFile file);

}
