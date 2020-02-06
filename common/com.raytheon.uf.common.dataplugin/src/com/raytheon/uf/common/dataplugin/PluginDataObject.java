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

package com.raytheon.uf.common.dataplugin;

import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.hibernate.annotations.Index;

import com.raytheon.uf.common.dataplugin.annotations.DataURI;
import com.raytheon.uf.common.dataplugin.annotations.DataURIFieldConverter;
import com.raytheon.uf.common.dataplugin.annotations.DataURIUtil;
import com.raytheon.uf.common.dataplugin.persist.DefaultPathProvider;
import com.raytheon.uf.common.dataplugin.persist.IHDFFilePathProvider;
import com.raytheon.uf.common.dataplugin.persist.PersistableDataObject;
import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;

/**
 * Abstract class from which all plugin specific data types inherit. A plugin
 * specific data type is a class found in each plugin with the naming convention
 * of <PluginType>Record.
 * <p>
 * For example, for a plugin that handled satellite images, the associated
 * plugin specific data type would be called SatelliteRecord.
 *
 * <pre>
 * Hibernate Annotation Requirements for "@Entity" annotated classes that are subclasses
 * of PluginDataObject
 *
 * 1) If it is not abstract and not a super class for "@Entity" annotated
 * subclasses, then add a SequenceGenerator annotation:
 * "@SequenceGenerator(initialValue = 1, name = PluginDataObject.ID_GEN, sequenceName = "
 * <tablename>seq")"
 *
 * 2) If it is abstract and a super class for @Entity annotated subclasses:
 *
 * - if there are "@ManyToOne" or "@OneToMany" relationships to the class, then
 * an "@Entity" annotation has to be used otherwise use a "@MappedSuperClass"
 * annotation
 *
 * - Add a @MappedSuperclass annotation
 *
 * - Add an "@Sequence" annotation
 * "@SequenceGenerator(name = PluginDataObject.ID_GEN)"
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 24, 2007  353      bphillip  Initial creation
 * Nov 29, 2007  472      jkorman   Added getDecoderGettable().
 * Feb 06, 2009  1990     bphillip  Added database index on dataURI
 * Mar 18, 2009  2105     jsanchez  Added getter for id.  Removed unused
 *                                  getIdentfier().
 * Mar 29, 2013  1638     mschenke  Added methods for loading from data map and
 *                                  creating data map from  dataURI fields
 * Apr 12, 2013  1857     bgonzale  Changed to MappedSuperclass, named
 *                                  generator,  GenerationType SEQUENCE, moved
 *                                  Indexes to getter  methods.
 * Apr 15, 2013  1868     bsteffen  Improved performance of createDataURIMap
 * May 02, 2013  1970     bgonzale  Moved Index annotation from getters to
 *                                  attributes.
 * May 07, 2013  1869     bsteffen  Remove dataURI column from PluginDataObject.
 * May 16, 2013  1869     bsteffen  Rewrite dataURI property mappings.
 * Aug 30, 2013  2298     rjpeter   Make getPluginName abstract
 * Apr 15, 2014  1869     bsteffen  Remove unused transient record field.
 * Jun 17, 2014  3165     bsteffen  Delete IDecoderGettable
 * Nov 05, 2015  5090     bsteffen  Add constants for datatime component ids.
 * Apr 05, 2018  6696     randerso  Added FCSTTIME_ID
 * Apr 24, 2019  6140     tgurney   Remove Inheritance annotation
 *                                  (Hibernate 5.4 fix)
 *
 * </pre>
 *
 */
@MappedSuperclass
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public abstract class PluginDataObject extends PersistableDataObject
        implements ISerializableObject {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(PluginDataObject.class);

    public static final class DataTimeURIConverter
            implements DataURIFieldConverter<DataTime> {
        @Override
        public String toString(DataTime field) {
            if (field == null) {
                return null;
            }
            return field.getURIString();
        }

        @Override
        public DataTime fromString(String string) {
            return new DataTime(string);
        }
    }

    private static final long serialVersionUID = 1L;

    public static final String PLUGIN_NAME_ID = "pluginName";

    public static final String DATATIME_ID = "dataTime";

    public static final String REFTIME_ID = "dataTime.refTime";

    public static final String FCSTTIME_ID = "dataTime.fcstTime";

    public static final String STARTTIME_ID = "dataTime.validPeriod.start";

    public static final String ENDTIME_ID = "dataTime.validPeriod.end";

    public static final String DATAURI_ID = "dataURI";

    public static final String ID_GEN = "idgen";

    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_GEN)
    @Id
    protected int id;

    /** The data time for this record */
    @Embedded
    @XmlElement
    @DynamicSerializeElement
    @DataURI(position = 0, converter = DataTimeURIConverter.class)
    protected DataTime dataTime;

    /**
     * The timestamp denoting when this record was inserted into the database
     */
    @Column(columnDefinition = "timestamp without time zone")
    @Index(name = "%TABLE%_insertTimeIndex")
    @XmlAttribute
    @DynamicSerializeElement
    protected Calendar insertTime;

    /** The raw data from the message */
    @Transient
    @DynamicSerializeElement
    protected Object messageData;

    @Transient
    protected transient String dataURI;

    /**
     * Default Constructor
     */
    public PluginDataObject() {

    }

    public PluginDataObject(String uri) {
        try {
            DataURIUtil.populatePluginDataObject(this, uri);
        } catch (PluginException e) {
            // this should never happen operationally
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
        this.dataURI = uri;
    }

    /**
     * @deprecated getDataURI will generate the datauri on demand, no need to
     *             construct it.
     */
    @Deprecated
    public void constructDataURI() throws PluginException {
        this.dataURI = null;
        getDataURI();
    }

    public DataTime getDataTime() {
        return dataTime;
    }

    public String getDataURI() {
        if (dataURI == null) {
            try {
                this.dataURI = DataURIUtil.createDataURI(this);
            } catch (PluginException e) {
                // this should never happen operationally
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }
        return this.dataURI;
    }

    /**
     * @deprecated the JAXB context used by this method is not guaranteed to
     *             work for all subclasses, JAXB should be used directly instead
     *             of using this method.
     */
    @Deprecated
    public String toXML() throws JAXBException {
        return SerializationUtil.marshalToXml(this);
    }

    public Calendar getInsertTime() {
        return insertTime;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setInsertTime(Calendar insertTime) {
        this.insertTime = insertTime;
    }

    public Object getMessageData() {
        return messageData;
    }

    public void setMessageData(Object messageData) {
        this.messageData = messageData;
    }

    public abstract String getPluginName();

    public void setDataTime(DataTime dataTime) {
        this.dataTime = dataTime;
    }

    @Override
    public String toString() {
        return this.getDataURI();
    }

    @Override
    public String getIdentifier() {
        return getDataURI();
    }

    @Override
    public void setIdentifier(Object obj) {
        if (obj instanceof String) {
            this.dataURI = (String) obj;
        }
    }

    public void setDataURI(String dataURI) {
        this.dataURI = dataURI;
    }

    public IHDFFilePathProvider getHDFPathProvider() {
        return DefaultPathProvider.getInstance();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PluginDataObject rhs = (PluginDataObject) obj;
        if (dataTime == null) {
            if (rhs.dataTime != null) {
                return false;
            }
        } else if (!dataTime.getRefTime()
                .equals(rhs.getDataTime().getRefTime())) {
            return false;
        } else if (dataTime.getFcstTime() != rhs.getDataTime().getFcstTime()) {
            return false;
        }
        String dataURI = getDataURI();
        String rhsDataURI = rhs.getDataURI();
        if (dataURI == null) {
            if (rhsDataURI != null) {
                return false;
            }
        } else if (!dataURI.equals(rhsDataURI)) {
            return false;
        }

        if (insertTime == null) {
            if (rhs.insertTime != null) {
                return false;
            }
        } else if (!insertTime.equals(rhs.insertTime)) {
            return false;
        }

        String pluginName = getPluginName();
        String rhsPluginName = rhs.getPluginName();
        if (pluginName == null) {
            if (rhsPluginName != null) {
                return false;
            }
        } else if (!pluginName.equals(rhsPluginName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (dataTime == null ? 0 : dataTime.hashCode());
        String dataUri = getDataURI();
        result = prime * result + (dataUri == null ? 0 : dataURI.hashCode());
        result = prime * result + id;
        result = prime * result
                + (insertTime == null ? 0 : insertTime.hashCode());
        String pluginName = getPluginName();
        result = prime * result
                + (pluginName == null ? 0 : pluginName.hashCode());
        return result;
    }

}
