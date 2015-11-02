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
package com.raytheon.uf.viz.core.rsc.interrogation;

/**
 * The purpose of this class is to provide a type safe key to access data from
 * an {@link Interrogatable}. The generic type is used to guarantee type safety
 * when calling interrogating because the generic type of the key will match the
 * value. This class can be used directly to provide identity based
 * interrogation or can be subclassed to provide more ad-hoc interrogation.
 * 
 * When an instance of this class is instantiated it can be used in
 * interrogation only using reference equality, meaning that both the
 * Interrogatable and the code performing interrogation must have access to the
 * exact same InterrogationKey instance in order to successfully share
 * information. The {@link #equals(Object)} method only uses
 * {@link Object#equals(Object)} so it is equivalent to ==.
 * 
 * When this class is subclassed, the keys can provide a more flexible contract
 * for interrogation because the Interrogatable and the calling code can
 * construct the keys separately but if the keys have object-equality they will
 * be able to successfully share information.
 * 
 * Any subclasses must be careful to implement {@link #hashCode()} and
 * {@link #equals(Object)} correctly and to ensure that the type safety of the
 * generic type can be guaranteed.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * May 15, 2014  2820     bsteffen  Initial creation
 * Oct 27, 2015  5018     bsteffen  Remove unnecessary hashCode/equals
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 * @param <T>
 *            the type of the Object that will be returned by an
 *            {@link Interrogatable} when using this key.
 * @see Interrogatable
 * @see ClassInterrogationKey
 * @see StringInterrogationKey
 */
public class InterrogationKey<T> {

    public InterrogationKey() {
        super();
    }

}
