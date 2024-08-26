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
package com.raytheon.uf.viz.core.rsc.capabilities;

/**
 * Interface for providing access to a resource's capabilities, without needing
 * access to the resource itself or its associated resource data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 23, 2024 2037631    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public interface ICapabilityProvider {

    /**
     * Get the resource's capability of the given type, creating it if it does
     * not yet exist.
     *
     * @param <T>
     *            capability type
     * @param capability
     *            capability class
     * @return capability of the requested type
     */
    <T extends AbstractCapability> T getCapability(Class<T> capability);

    /**
     * Check if the resource has a capability of the given type.
     *
     * @param capability
     *            capability class
     * @return true if resource has the capability; false otherwise
     */
    boolean hasCapability(Class<? extends AbstractCapability> capability);
}
