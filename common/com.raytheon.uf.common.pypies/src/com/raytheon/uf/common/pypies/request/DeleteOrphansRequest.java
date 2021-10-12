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
package com.raytheon.uf.common.pypies.request;

import java.util.Date;
import java.util.Map;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Request to delete orphaned data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 27, 2015 1574       nabowle     Initial creation
 * Feb 24, 2016 5389       nabowle     oldestDate is now oldestDateMap.
 * Sep 23, 2021 8608       mapeters    Add {@link #getType()}
 *
 * </pre>
 *
 * @author nabowle
 */
@DynamicSerialize
public class DeleteOrphansRequest extends AbstractRequest {

    @DynamicSerializeElement
    private Map<String, Date> oldestDateMap;

    /**
     * Constructor.
     */
    public DeleteOrphansRequest() {
        super();
    }

    /**
     * Constructor.
     *
     * @param pluginName
     *            The plugin name.
     * @param oldestDateMap
     *            The oldest dates which should be kept per distinct path.
     *            Anything older than the dates will be deleted for the given
     *            path. The plugin name should be used if specific dates do not
     *            apply.
     */
    public DeleteOrphansRequest(String pluginName,
            Map<String, Date> oldestDateMap) {
        super();
        setFilename(pluginName);
        this.oldestDateMap = oldestDateMap;
    }

    /**
     * @return the oldestDate
     */
    public Map<String, Date> getOldestDateMap() {
        return oldestDateMap;
    }

    /**
     * @param oldestDate
     *            the oldestDate to set
     */
    public void setOldestDateMap(Map<String, Date> oldestDateMap) {
        this.oldestDateMap = oldestDateMap;
    }

    @Override
    public RequestType getType() {
        return RequestType.DELETE;
    }
}
