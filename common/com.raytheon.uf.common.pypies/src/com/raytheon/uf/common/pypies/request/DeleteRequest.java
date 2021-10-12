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

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * TODO Add Description
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 10, 2010            njensen     Initial creation
 * Feb 12, 2013 1608       randerso    Added support for explicitly deleting grous and datasets
 * Sep 23, 2021 8608       mapeters    Add {@link #getType()}
 *
 * </pre>
 *
 * @author njensen
 */
@DynamicSerialize
public class DeleteRequest extends AbstractRequest {

    @DynamicSerializeElement
    private String[] datasets;

    @DynamicSerializeElement
    private String[] groups;

    public String[] getDatasets() {
        return datasets;
    }

    public void setDatasets(String[] datasets) {
        this.datasets = datasets;
    }

    public String[] getGroups() {
        return groups;
    }

    public void setGroups(String[] groups) {
        this.groups = groups;
    }

    @Override
    public RequestType getType() {
        return RequestType.DELETE;
    }
}
