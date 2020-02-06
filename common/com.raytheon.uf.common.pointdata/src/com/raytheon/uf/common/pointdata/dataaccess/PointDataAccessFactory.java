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
package com.raytheon.uf.common.pointdata.dataaccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.measure.Unit;
import javax.measure.UnitConverter;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

import com.raytheon.uf.common.dataaccess.DataAccessLayer;
import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.dataaccess.exception.DataRetrievalException;
import com.raytheon.uf.common.dataaccess.exception.InvalidIdentifiersException;
import com.raytheon.uf.common.dataaccess.geom.IGeometryData;
import com.raytheon.uf.common.dataaccess.geom.IGeometryData.Type;
import com.raytheon.uf.common.dataaccess.impl.AbstractDataPluginFactory;
import com.raytheon.uf.common.dataaccess.impl.DefaultGeometryData;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataplugin.level.LevelFactory;
import com.raytheon.uf.common.dataplugin.level.MasterLevel;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.message.response.ResponseMessageCatalog;
import com.raytheon.uf.common.pointdata.PointDataConstants;
import com.raytheon.uf.common.pointdata.PointDataContainer;
import com.raytheon.uf.common.pointdata.PointDataDescription;
import com.raytheon.uf.common.pointdata.PointDataServerRequest;
import com.raytheon.uf.common.pointdata.PointDataView;
import com.raytheon.uf.common.serialization.comm.RequestRouter;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.common.units.UnitConv;

import tec.uom.se.AbstractConverter;

/**
 * Data Access Factory for retrieving point data as a geometry.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 31, 2013  2502     bsteffen  Initial creation
 * Nov 26, 2013  2537     bsteffen  Minor code cleanup.
 * Jan,14, 2014  2667     mnash     Remove getGridData method
 * Feb 06, 2014  2672     bsteffen  Add envelope support
 * Sep 09, 2014  3356     njensen   Remove CommunicationException
 * Sep 10, 2014  3615     nabowle   Add support for null count and level
 *                                  parameters.
 * Feb 19, 2015  4147     mapeters  Override getAvailableParameters().
 * Feb 27, 2015  4179     mapeters  Use super's getAvailableValues().
 * Jan 28, 2016  5275     bsteffen  Avoid creating clutter in level database.
 * Jun 09, 2016  5587     bsteffen  Support datatype specific optional
 *                                  identifiers.
 * Jun 13, 2016  5574     tgurney   Support RequestConstraint as identifier
 *                                  value
 * Jul 22, 2016  2416     tgurney   Add dataURI as optional identifier
 * Nov 08, 2016  5986     tgurney   Handle reftime stored in seconds and
 *                                  forecast time stored in hours
 * Mar 06, 2017  6142     bsteffen  Remove dataURI as optional identifier
 * Sep 23, 2019  7939     tgurney   Fix levels unit conversion
 *
 * </pre>
 *
 * @author bsteffen
 */
public class PointDataAccessFactory extends AbstractDataPluginFactory {

    private static class TwoDimensionalParameterGroup {

        public final String countParameter;

        public final String levelParameter;

        public final String levelType;

        public final String[] parameters;

        public TwoDimensionalParameterGroup(String countParameter,
                String levelParameter, String levelType, String[] parameters) {
            super();
            this.countParameter = countParameter;
            this.levelParameter = levelParameter;
            this.levelType = levelType;
            this.parameters = parameters;
        }

    }

    private String locationDatabaseKey = "location.stationId";

    private String locationPointDataKey = PointDataConstants.DATASET_STATIONID;

    private String latitudeDatabaseKey = "location.latitude";

    private String latitudePointDataKey = "latitude";

    private String longitudeDatabaseKey = "location.longitude";

    private String longitudePointDataKey = "longitude";

    private String refTimePointDataKey = PointDataConstants.DATASET_REFTIME;

    private String fcstHrPointDataKey = PointDataConstants.DATASET_FORECASTHR;

    private final Map<String, TwoDimensionalParameterGroup> parameters2D = new HashMap<>();

    private String[] optionalIdentifiers = {};

    @Override
    public String[] getAvailableLocationNames(IDataRequest request) {
        return getAvailableValues(request, locationDatabaseKey, String.class);
    }

    @Override
    public String[] getAvailableParameters(IDataRequest request) {
        validateRequest(request, false);

        Map<String, RequestConstraint> constraints = new HashMap<>();
        constraints.put(PointDataServerRequest.REQUEST_MODE_KEY,
                new RequestConstraint(
                        PointDataServerRequest.REQUEST_MODE_PARAMETERS));
        constraints.put(DBQUERY_PLUGIN_NAME_KEY,
                new RequestConstraint(request.getDatatype()));
        PointDataServerRequest serverRequest = new PointDataServerRequest(
                constraints);

        /*
         * Store request string before constraints are removed during execution
         * of the request (used if exception is thrown).
         */
        String serverRequestString = serverRequest.toString();

        ResponseMessageCatalog response;
        try {
            response = (ResponseMessageCatalog) RequestRouter
                    .route(serverRequest);
        } catch (Exception e) {
            throw new DataRetrievalException(
                    "Failed to retrieve available parameters for request: "
                            + serverRequestString,
                    e);
        }

        return response.getValues();
    }

    @Override
    public IGeometryData[] getGeometryData(IDataRequest request,
            DataTime... times) {
        /*
         * Point data uses PointDataServerRequest instead of the DbQueryRequest
         * that is used in AbstractDataPluginFactory. Override this method so
         * the DbQueryRequest can be converted to a PointDataServerRequest
         */
        validateRequest(request);
        DbQueryRequest dbQueryRequest = this.buildDbQueryRequest(request,
                times);
        return getGeometryData(request, dbQueryRequest);
    }

    @Override
    public IGeometryData[] getGeometryData(IDataRequest request,
            TimeRange timeRange) {
        /*
         * Point data uses PointDataServerRequest instead of the DbQueryRequest
         * that is used in AbstractDataPluginFactory. Override this method so
         * the DbQueryRequest can be converted to a PointDataServerRequest
         */
        validateRequest(request);
        DbQueryRequest dbQueryRequest = this.buildDbQueryRequest(request,
                timeRange);
        return getGeometryData(request, dbQueryRequest);
    }

    @Override
    public String[] getOptionalIdentifiers(IDataRequest request) {
        return optionalIdentifiers;
    }

    @Override
    public String[] getIdentifierValues(IDataRequest request,
            String identifierKey) {
        if (!Arrays.asList(getOptionalIdentifiers(request))
                .contains(identifierKey)) {
            throw new InvalidIdentifiersException(request.getDatatype(), null,
                    Arrays.asList(new String[] { identifierKey }));
        }
        List<String> idValStrings;
        Object[] idValues = getAvailableValues(request, identifierKey,
                Object.class);
        idValStrings = new ArrayList<>(idValues.length);
        for (Object idValue : idValues) {
            idValStrings.add(idValue.toString());
        }
        return idValStrings.toArray(new String[0]);
    }

    @Override
    protected Map<String, RequestConstraint> buildConstraintsFromRequest(
            IDataRequest request) {
        Map<String, RequestConstraint> rcMap = new HashMap<>();
        String[] locations = request.getLocationNames();
        if (locations != null && locations.length != 0) {
            rcMap.put(locationDatabaseKey, new RequestConstraint(locations));
        }
        Map<String, Object> identifiers = request.getIdentifiers();
        if (identifiers != null) {
            for (Entry<String, Object> entry : identifiers.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof RequestConstraint) {
                    rcMap.put(entry.getKey(), (RequestConstraint) value);
                } else {
                    rcMap.put(entry.getKey(),
                            new RequestConstraint(value.toString()));
                }
            }
        }
        Envelope envelope = request.getEnvelope();
        if (envelope != null) {
            String minLon = Double.toString(envelope.getMinX());
            String maxLon = Double.toString(envelope.getMaxX());
            rcMap.put(longitudeDatabaseKey,
                    new RequestConstraint(minLon, maxLon));
            String minLat = Double.toString(envelope.getMinY());
            String maxLat = Double.toString(envelope.getMaxY());
            rcMap.put(latitudeDatabaseKey,
                    new RequestConstraint(minLat, maxLat));
        }
        return rcMap;
    }

    /**
     *
     * Request point data from the server and convert to {@link IGeometryData}
     *
     * @param request
     *            the original request from the {@link DataAccessLayer}
     * @param dbQueryRequest
     *            the request generated by {@link AbstractDataPluginFactory},
     *            this will be converted into a {@link PointDataServerRequest}.
     * @return {@link IGeometryData}
     */
    protected IGeometryData[] getGeometryData(IDataRequest request,
            DbQueryRequest dbQueryRequest) {
        PointDataServerRequest serverRequest = convertRequest(request,
                dbQueryRequest);

        PointDataContainer pdc = null;
        try {
            pdc = (PointDataContainer) RequestRouter.route(serverRequest);
        } catch (Exception e) {
            throw new DataRetrievalException(
                    "Unable to complete the PointDataRequestMessage for request: "
                            + request,
                    e);
        }
        if (pdc == null) {
            return new IGeometryData[0];
        }
        LevelFactory lf = LevelFactory.getInstance();
        /* Convert the point data container into a list of IGeometryData */
        List<IGeometryData> result = new ArrayList<>(pdc.getAllocatedSz());
        for (int i = 0; i < pdc.getCurrentSz(); i += 1) {
            PointDataView pdv = pdc.readRandom(i);
            DefaultGeometryData data = createNewGeometryData(pdv);
            data.setLevel(lf.getLevel(LevelFactory.UNKNOWN_LEVEL, 0.0));
            Set<TwoDimensionalParameterGroup> parameters2D = new HashSet<>();
            for (String parameter : request.getParameters()) {
                if (pdc.getParameters().contains(parameter)) {
                    int dim = pdc.getDimensions(parameter);
                    if (dim == 1) {
                        Unit<?> unit = pdv.getUnit(parameter);
                        PointDataDescription.Type type = pdv.getType(parameter);
                        if (type == PointDataDescription.Type.STRING) {
                            data.addData(parameter, pdv.getString(parameter),
                                    Type.STRING, unit);
                        } else {
                            data.addData(parameter, pdv.getNumber(parameter),
                                    unit);
                        }
                    } else if (this.parameters2D.containsKey(parameter)) {
                        parameters2D.add(this.parameters2D.get(parameter));
                    } else {
                        throw new DataRetrievalException(
                                "PointDataAccessFactory cannot handle " + dim
                                        + "D parameters: " + parameter);
                    }
                }
            }
            for (TwoDimensionalParameterGroup p2d : parameters2D) {
                result.addAll(make2DData(request, p2d, pdv));
            }
            if (!data.getParameters().isEmpty()) {
                result.add(data);
            }
        }
        return result.toArray(new IGeometryData[0]);
    }

    /**
     * Pull the constraints ouf of a {@link DbQueryRequest} and combine the
     * information with an {@link IDataRequest} to build a
     * {@link PointDataServerRequest}. This is done because
     * {@link AbstractDataPluginFactory} makes really nice DbQueryRequests but
     * we can't use them for point data.
     *
     * @param request
     * @param dbQueryRequest
     * @return
     */
    private PointDataServerRequest convertRequest(IDataRequest request,
            DbQueryRequest dbQueryRequest) {
        Map<String, RequestConstraint> constraints = dbQueryRequest
                .getConstraints();
        constraints.put(PointDataServerRequest.REQUEST_MODE_KEY,
                new RequestConstraint(PointDataServerRequest.REQUEST_MODE_2D));
        /*
         * Figure out what parameters we actually need.
         */
        Set<String> parameters = new HashSet<>();

        for (String parameter : request.getParameters()) {
            /*
             * Make sure that any 2D parameters also have the count parameter
             * requested.
             */
            TwoDimensionalParameterGroup p2d = this.parameters2D.get(parameter);
            if (p2d != null) {
                if (p2d.countParameter != null) {
                    parameters.add(p2d.countParameter);
                }
                if (p2d.levelParameter != null) {
                    parameters.add(p2d.levelParameter);
                }
            }
            parameters.add(parameter);
        }
        /* Always request location parameters */
        parameters.add(locationPointDataKey);
        parameters.add(latitudePointDataKey);
        parameters.add(longitudePointDataKey);
        parameters.add(refTimePointDataKey);
        if (fcstHrPointDataKey != null) {
            parameters.add(fcstHrPointDataKey);
        }

        constraints.put(PointDataServerRequest.REQUEST_PARAMETERS_KEY,
                new RequestConstraint(parameters));

        return new PointDataServerRequest(constraints);
    }

    /**
     * Pull out location and time data from a {@link PointDataView} to build a
     * {@link DefaultGeometryData}.
     *
     * @param pdv
     *            view for a single record
     * @return {@link DefaultGeometryData} with locationName, time, and geometry
     *         set.
     */
    private DefaultGeometryData createNewGeometryData(PointDataView pdv) {
        DefaultGeometryData data = new DefaultGeometryData();
        data.setLocationName(pdv.getString(locationPointDataKey));
        data.setDataTime(pdv.getDataTime(false));
        Coordinate c = new Coordinate(pdv.getFloat(longitudePointDataKey),
                pdv.getFloat(latitudePointDataKey));
        data.setGeometry(new GeometryFactory().createPoint(c));
        return data;
    }

    /**
     * Make a {@link IGeometryData} object for each level in a 2 dimensional
     * data set.
     *
     * @param request
     *            the original request
     * @param p2d
     *            The 2d Parameter group
     * @param pdv
     *            pdv containing data.
     * @return One IGeometryData for each valid level in the 2d group.
     */
    private List<IGeometryData> make2DData(IDataRequest request,
            TwoDimensionalParameterGroup p2d, PointDataView pdv) {
        List<String> requestParameters = Arrays.asList(request.getParameters());
        LevelFactory lf = LevelFactory.getInstance();

        int count;
        if (p2d.countParameter == null) {
            count = getMaxCount(requestParameters, p2d, pdv);
        } else {
            count = pdv.getInt(p2d.countParameter);
            // Some count fields of bufrua (and maybe others) come back as -9999
            // instead of 0 if there's no level data. In this case, clamp to
            // zero so initialing result doesn't throw an exception
            if (count < 0) {
                count = 0;
            }
        }

        List<IGeometryData> result = new ArrayList<>(count);

        Unit<?> levelUnit;
        Number[] levelValues;
        if (p2d.levelParameter == null) {
            levelUnit = null;
            levelValues = new Number[0];
        } else {
            levelUnit = pdv.getUnit(p2d.levelParameter);
            levelValues = pdv.getNumberAllLevels(p2d.levelParameter);
        }
        MasterLevel masterLevel = lf.getMasterLevel(p2d.levelType);
        Unit<?> masterUnit = masterLevel.getUnit();
        UnitConverter levelUnitConverter = AbstractConverter.IDENTITY;
        if (levelUnit != null && masterUnit != null) {
            levelUnitConverter = UnitConv.getConverterToUnchecked(levelUnit,
                    masterUnit);
        }

        String[] stringValues;
        Number[] numberValues;
        for (int j = 0; j < count; j += 1) {
            /* Clone the data, not level or parameters though */
            DefaultGeometryData leveldata = createNewGeometryData(pdv);

            if (j < levelValues.length) {
                /*
                 * Do not create a level from the level factory. For observed
                 * data this leads to a cluttered level database and there is no
                 * need have this level backed by the database.
                 */
                Level level = new Level();
                double levelValue = levelUnitConverter
                        .convert(levelValues[j].doubleValue());
                level.setMasterLevel(masterLevel);
                level.setLevelonevalue(levelValue);
                leveldata.setLevel(level);
            }

            for (String parameter : p2d.parameters) {
                if (requestParameters.contains(parameter)) {
                    Unit<?> unit = pdv.getUnit(parameter);
                    PointDataDescription.Type type = pdv.getType(parameter);
                    if (type == PointDataDescription.Type.STRING) {
                        stringValues = pdv.getStringAllLevels(parameter);
                        if (j < stringValues.length) {
                            leveldata.addData(parameter, stringValues[j],
                                    Type.STRING, unit);
                        }
                    } else {
                        numberValues = pdv.getNumberAllLevels(parameter);
                        if (j < numberValues.length) {
                            leveldata.addData(parameter, numberValues[j], unit);
                        }
                    }
                }
            }
            result.add(leveldata);
        }

        return result;
    }

    /**
     * Get the maximum number of values for all requested values of the 2d
     * parameter group.
     *
     * @param requestParameters
     *            The requested parameters.
     * @param p2d
     *            The 2d Parameter group.
     * @param pdv
     *            pdv containing data.
     */
    private int getMaxCount(List<String> requestParameters,
            TwoDimensionalParameterGroup p2d, PointDataView pdv) {
        int maxCount = 0;
        int tempCount;
        for (String parameter : p2d.parameters) {
            if (requestParameters.contains(parameter)) {
                PointDataDescription.Type type = pdv.getType(parameter);
                if (type == PointDataDescription.Type.STRING) {
                    tempCount = pdv.getStringAllLevels(parameter).length;
                } else {
                    tempCount = pdv.getNumberAllLevels(parameter).length;
                }
                if (tempCount > maxCount) {
                    maxCount = tempCount;
                }
            }
        }
        return maxCount;
    }

    /**
     * Point data types with 2 dimensions need to register so the 2d parameters
     * can be grouped appropriately
     *
     * @param countParameter
     *            parameter name of an integer parameter identifying the number
     *            of valid levels.
     * @param levelParameter
     *            parameter which should be used to build the level object in
     *            IGeometryData, for example "pressure"
     * @param levelType
     *            {@link MasterLevel} name for the levelParameter, for example
     *            "MB"
     * @param parameters
     *            all the parameters that are valid on the same 2D levels.
     * @return countParameter is returned so spring can have a bean.
     */
    public String register2D(String countParameter, String levelParameter,
            String levelType, String[] parameters) {
        TwoDimensionalParameterGroup td = new TwoDimensionalParameterGroup(
                countParameter, levelParameter, levelType, parameters);
        for (String parameter : parameters) {
            parameters2D.put(parameter, td);
        }
        return countParameter;
    }

    /**
     * @param locationDatabaseKey
     *            The hibernate field name of the field that is used to identify
     *            location names. Default values is "location.stationId"
     */
    public void setLocationDatabaseKey(String locationDatabaseKey) {
        this.locationDatabaseKey = locationDatabaseKey;
    }

    /**
     * @param locationPointDataKey
     *            The point data key that matches the location database key.
     *            Defaults to "stationId"
     */
    public void setLocationPointDataKey(String locationPointDataKey) {
        this.locationPointDataKey = locationPointDataKey;
    }

    /**
     * @param latitudePointDataKey
     *            The point data key of the station latitude. Default value is
     *            "latitude"
     */
    public void setLatitudePointDataKey(String latitudePointDataKey) {
        this.latitudePointDataKey = latitudePointDataKey;
    }

    /**
     * @param longitudePointDataKey
     *            The point data key of the station longitude. Default value is
     *            "longitude"
     */
    public void setLongitudePointDataKey(String longitudePointDataKey) {
        this.longitudePointDataKey = longitudePointDataKey;
    }

    /**
     * @param latitudeDatabaseKey
     *            The hibernate field name of the field that is used to identify
     *            latitude. Default values is "location.latitude"
     */
    public void setLatitudeDatabaseKey(String latitudeDatabaseKey) {
        this.latitudeDatabaseKey = latitudeDatabaseKey;
    }

    /**
     * @param longitudeDatabaseKey
     *            The hibernate field name of the field that is used to identify
     *            longitude. Default values is "location.longitude"
     */
    public void setLongitudeDatabaseKey(String longitudeDatabaseKey) {
        this.longitudeDatabaseKey = longitudeDatabaseKey;
    }

    /**
     * @param refTimePointDataKey
     *            The point data key of the reference time. Default value is
     *            "refTime"
     */
    public void setRefTimePointDataKey(String refTimePointDataKey) {
        this.refTimePointDataKey = refTimePointDataKey;
    }

    /**
     * @param fcstHrPointDataKey
     *            The point data key of the forecast hour. Default value is
     *            "forecastHr". For live data with no forecast times this can be
     *            set to null so that it is not retrieved.
     */
    public void setFcstHrPointDataKey(String fcstHrPointDataKey) {
        this.fcstHrPointDataKey = fcstHrPointDataKey;
    }

    /**
     *
     * @param optionalIdentifiers
     *            The hibernate field names of any fields that can be used as
     *            identifiers.
     */
    public void setOptionalIdentifiers(String[] optionalIdentifiers) {
        this.optionalIdentifiers = optionalIdentifiers;
    }
}
