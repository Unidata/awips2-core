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
package com.raytheon.uf.viz.core.maps.rsc;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.maps.Activator;
import com.raytheon.uf.viz.core.maps.MapManager;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;

/**
 * Add saved styles to AbstractMapResource
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 21, 2014  #3459     randerso    Restructured Map resource class hierarchy
 * May 31, 2018  6562      tgurney     T extends AbstractMapResourceData
 *
 * </pre>
 *
 * @author randerso
 */

public abstract class StyledMapResource<T extends AbstractMapResourceData, D extends IMapDescriptor>
        extends AbstractMapResource<T, D> {

    /**
     * @param resourceData
     * @param loadProperties
     */
    protected StyledMapResource(T resourceData, LoadProperties loadProperties) {
        super(resourceData, loadProperties);
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        super.initInternal(target);

        // retrieve the saved style for this map
        Capabilities saved = Activator.getDefault().getStylePreferences().get(
                MapManager.getInstance(descriptor).getPerspective(), getName());
        getCapabilities().setCapabilityClassCollection(
                saved.getCapabilityClassCollection());
    }
}
