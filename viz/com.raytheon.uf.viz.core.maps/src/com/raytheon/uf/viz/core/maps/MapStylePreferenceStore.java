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
package com.raytheon.uf.viz.core.maps;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.ILocalizationFileObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.jaxb.JAXBClassLocator;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.reflect.SubClassLocator;
import com.raytheon.uf.viz.core.rsc.capabilities.AbstractCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;

/**
 * Class to store map style preferences
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 7, 2010             randerso    Initial creation
 * Jan 25, 2013 DR 15649   D. Friedman Clone capabilities in get/put.
 *                                     Stored preferences in a sub-directory
 *                                     and observe changes.
 * Nov 08, 2013 2361       njensen     Use JAXBManager for XML
 * Feb 24, 2015 3978       njensen     Removed OBE code
 * Jun 26, 2015 4598       randerso    Added better XML annotations
 * 
 * </pre>
 * 
 * @author randerso
 * @version 1.0
 */

@XmlRootElement()
@XmlAccessorType(XmlAccessType.NONE)
public class MapStylePreferenceStore {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(MapStylePreferenceStore.class);

    private static final String MAPSTYLE_FILENAME = "mapStyles/mapstylepreferences.xml";

    private static JAXBManager jaxb;

    @XmlAccessorType(XmlAccessType.NONE)
    private static class MapStylePreferenceKey {
        @XmlElement
        private String perspective;

        @XmlElement
        private String mapName;

        private MapStylePreferenceKey() {
        }

        public MapStylePreferenceKey(String perspective, String mapName) {
            this();
            this.perspective = perspective;
            this.mapName = mapName;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = (prime * result)
                    + ((mapName == null) ? 0 : mapName.hashCode());
            result = (prime * result)
                    + ((perspective == null) ? 0 : perspective.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MapStylePreferenceKey other = (MapStylePreferenceKey) obj;
            if (mapName == null) {
                if (other.mapName != null) {
                    return false;
                }
            } else if (!mapName.equals(other.mapName)) {
                return false;
            }
            if (perspective == null) {
                if (other.perspective != null) {
                    return false;
                }
            } else if (!perspective.equals(other.perspective)) {
                return false;
            }
            return true;
        }
    }

    private Map<MapStylePreferenceKey, Capabilities> combinedPreferences;

    @XmlElement
    private Map<MapStylePreferenceKey, Capabilities> preferences;

    LocalizationFile siteLf, userLf;

    @XmlTransient
    boolean needToLoad = true;

    public static MapStylePreferenceStore load() {
        MapStylePreferenceStore store = new MapStylePreferenceStore();
        store.loadFiles();
        return store;
    }

    private synchronized void loadFiles() {
        if (needToLoad) {
            needToLoad = false;
        } else {
            return;
        }

        IPathManager pathMgr = PathManagerFactory.getPathManager();

        if (siteLf == null) {
            siteLf = pathMgr.getLocalizationFile(pathMgr.getContext(
                    LocalizationType.CAVE_STATIC, LocalizationLevel.SITE),
                    MAPSTYLE_FILENAME);

            userLf = pathMgr.getLocalizationFile(pathMgr.getContext(
                    LocalizationType.CAVE_STATIC, LocalizationLevel.USER),
                    MAPSTYLE_FILENAME);

            ILocalizationFileObserver obs = new ILocalizationFileObserver() {
                @Override
                public void fileUpdated(FileUpdatedMessage message) {
                    synchronized (MapStylePreferenceStore.this) {
                        needToLoad = true;
                    }
                }
            };

            siteLf.addFileUpdatedObserver(obs);
            userLf.addFileUpdatedObserver(obs);
        }

        if (siteLf.exists()) {
            try {
                combinedPreferences = getJaxbManager().unmarshalFromXmlFile(
                        MapStylePreferenceStore.class, siteLf.getFile()).preferences;
            } catch (Exception e) {
                statusHandler
                        .handle(Priority.PROBLEM,
                                "Exception while loading site map style preferences",
                                e);
            }
        } else {
            combinedPreferences = new HashMap<MapStylePreferenceKey, Capabilities>();
        }

        if (userLf.exists()) {
            try {
                preferences = getJaxbManager().unmarshalFromXmlFile(
                        MapStylePreferenceStore.class, userLf.getFile()).preferences;

                // merge user into site
                for (Entry<MapStylePreferenceKey, Capabilities> entry : preferences
                        .entrySet()) {
                    combinedPreferences.put(entry.getKey(), entry.getValue());
                }

            } catch (Exception e) {
                statusHandler
                        .handle(Priority.PROBLEM,
                                "Exception while loading user map style preferences",
                                e);
            }
        } else {
            preferences = new HashMap<MapStylePreferenceKey, Capabilities>();
        }
    }

    private static synchronized JAXBManager getJaxbManager()
            throws JAXBException {
        if (jaxb == null) {
            SubClassLocator locator = new SubClassLocator();
            Collection<Class<?>> classes = JAXBClassLocator.getJAXBClasses(
                    locator, AbstractCapability.class,
                    MapStylePreferenceStore.class);
            jaxb = new JAXBManager(classes.toArray(new Class<?>[0]));
        }
        return jaxb;
    }

    private MapStylePreferenceStore() {
    }

    public synchronized Capabilities get(String perspective, String mapName) {
        MapStylePreferenceKey key = new MapStylePreferenceKey(perspective,
                mapName);

        loadFiles();

        Capabilities value = combinedPreferences.get(key);
        if (value == null) {
            value = new Capabilities();
        } else {
            value = value.clone();
        }
        return value;
    }

    public synchronized Capabilities put(String perspective, String mapName,
            Capabilities value) {
        MapStylePreferenceKey key = new MapStylePreferenceKey(perspective,
                mapName);

        value = value.clone();

        Capabilities oldValue = combinedPreferences.put(key, value);
        preferences.put(key, value);

        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationFile lf = pathMgr.getLocalizationFile(pathMgr.getContext(
                LocalizationType.CAVE_STATIC, LocalizationLevel.USER),
                MAPSTYLE_FILENAME);

        File file = lf.getFile();
        try {
            getJaxbManager().marshalToXmlFile(this, file.getAbsolutePath());
            lf.save();
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Exception while storing map style preferences", e);
        }
        return oldValue;
    }
}
