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
package com.raytheon.uf.common.dataplugin.maps.dataaccess;

import java.util.Map;

import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.dataaccess.exception.DataRetrievalException;
import com.raytheon.uf.common.dataaccess.exception.IncompatibleRequestException;
import com.raytheon.uf.common.dataaccess.geom.IGeometryData;
import com.raytheon.uf.common.dataaccess.impl.AbstractGeometryTimeAgnosticDatabaseFactory;
import com.raytheon.uf.common.dataplugin.maps.dataaccess.MapsQueryAssembler.IDENTIFIERS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

/**
 * A data factory for retrieving data from the maps database. Currently, the
 * name of the table to retrieve data from and the name of the geometry field of
 * interest are required identifiers.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 28, 2013            bkowal      Initial creation
 * Feb 14, 2013 1614       bsteffen    Refactor data access framework to use
 *                                     single request.
 * Jul 14, 2014 3184       njensen     Overrode getAvailableLevels()
 * Jul 30, 2014 3184       njensen     Added optional identifiers
 * Feb 03, 2015 4009       mapeters    Moved getAvailableLevels() override to super
 * Aug 13, 2015 4705       bkowal      Make parameters optional. Fixed implementation of optional
 *                                     location field.
 * Apr 26, 2016 5587       tgurney     Support getIdentifierValues()
 * Feb 19, 2018 7220       mapeters    Improve filtering of available identifier values
 * Jul 02, 2018 7327       mapeters    Overrode isColumnIdentifier() to include geomField
 *
 * </pre>
 *
 * @author bkowal
 */
public class MapsGeometryFactory
        extends AbstractGeometryTimeAgnosticDatabaseFactory {
    private static final String[] REQUIRED_IDENTIFIERS = new String[] {
            MapsQueryAssembler.REQUIRED_IDENTIFIERS.IDENTIFIER_TABLE,
            MapsQueryAssembler.REQUIRED_IDENTIFIERS.IDENTIFIER_GEOM_FIELD };

    private static final String MAPS_DATABASE = "maps";

    private static final ThreadLocal<WKBReader> wkbReaderWrapper = new ThreadLocal<WKBReader>() {
        @Override
        protected WKBReader initialValue() {
            return new WKBReader();
        }
    };

    /**
     * Constructor
     */
    public MapsGeometryFactory() {
        super(MAPS_DATABASE, REQUIRED_IDENTIFIERS,
                new String[] { COL_NAME_OPTION });
    }

    @Override
    protected void validateParameters(IDataRequest request)
            throws IncompatibleRequestException {
        /*
         * The {@link MapsGeometryFactory} will allow {@link IDataRequest}s
         * without parameters.
         */
        // Do Nothing.
    }

    @Override
    protected IGeometryData makeGeometry(Object[] data, String[] paramNames,
            Map<String, Object> attrs) {
        // order selected geom field, location, and other parameters

        // build the geometry
        Geometry geometry = null;
        Object object = data[0];
        int dataIndex = 1;
        if (!(object instanceof byte[])) {
            throw new DataRetrievalException(
                    "Retrieved Geometry was not the expected type; was expecting byte[], received: "
                            + object.getClass().getName());
        }
        try {
            geometry = (wkbReaderWrapper.get()).read((byte[]) object);
        } catch (ParseException e) {
            throw new DataRetrievalException("Failed to parse the geometry.",
                    e);
        }
        String location = null;
        /*
         * Determine if location information should be present.
         */
        if (attrs.containsKey(IDENTIFIERS.IDENTIFIER_LOCATION_FIELD)) {
            location = (String) data[1];
            ++dataIndex;
        }

        return super.buildGeometryData(null, null, geometry, location, attrs,
                dataIndex, data, paramNames);
    }

    @Override
    protected String assembleGetData(IDataRequest request) {
        return MapsQueryAssembler.assembleGetData(request);
    }

    @Override
    protected String assembleGetAvailableLocationNames(IDataRequest request) {
        return MapsQueryAssembler.assembleGetAvailableLocationNames(request);
    }

    @Override
    protected String assembleGetIdentifierValues(IDataRequest request,
            String identifierKey) {
        String query;
        String table = MapsQueryAssembler.REQUIRED_IDENTIFIERS.IDENTIFIER_TABLE;
        String geomField = MapsQueryAssembler.REQUIRED_IDENTIFIERS.IDENTIFIER_GEOM_FIELD;
        if (identifierKey.equals(table)) {
            query = assembleGetTableNames();
        } else if (identifierKey.equals(geomField)) {
            query = assembleGetGeomFieldNames(request);
        } else {
            query = assembleGetColumnValues(request, extractTableName(request),
                    identifierKey);
        }
        return query;
    }

    private String assembleGetGeomFieldNames(IDataRequest request) {
        String[] tableParsed = splitTableName(request);
        String schema = tableParsed[0];
        String tableName = tableParsed[1];
        return String.format("select distinct column_name "
                + "from information_schema.columns "
                + "where table_name = '%s' " + "and table_schema = '%s'"
                + "and column_name like 'the_geom%%' " + "order by column_name",
                tableName, schema);
    }

    @Override
    protected boolean isColumnIdentifier(String identifier) {
        return super.isColumnIdentifier(identifier)
                && !MapsQueryAssembler.REQUIRED_IDENTIFIERS.IDENTIFIER_GEOM_FIELD
                        .equals(identifier);
    }
}