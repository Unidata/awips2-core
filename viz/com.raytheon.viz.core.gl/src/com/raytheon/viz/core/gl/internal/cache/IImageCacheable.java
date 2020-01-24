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
package com.raytheon.viz.core.gl.internal.cache;

import com.raytheon.uf.common.util.cache.ICacheObject;

/**
 *
 * Interface for objects in the image cache
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------------------------
 * Aug 03, 2011           bsteffen  Initial creation
 * Jan 21, 2020  73572    tjensen   Add sizeManagement arg to disposeTexture
 *
 * </pre>
 *
 * @author bsteffen
 */
public interface IImageCacheable extends ICacheObject {

    public abstract void disposeTextureData();

    public abstract void disposeTexture(boolean sizeManagement);

}
