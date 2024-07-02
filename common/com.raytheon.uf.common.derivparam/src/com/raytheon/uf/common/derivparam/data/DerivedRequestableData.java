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
package com.raytheon.uf.common.derivparam.data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.derivparam.library.DerivedParameterGenerator;
import com.raytheon.uf.common.derivparam.library.DerivedParameterRequest;
import com.raytheon.uf.common.inventory.TimeAndSpace;
import com.raytheon.uf.common.inventory.data.AbstractRequestableData;
import com.raytheon.uf.common.inventory.data.AggregateRequestableData;
import com.raytheon.uf.common.inventory.data.CubeRequestableData;
import com.raytheon.uf.common.inventory.exception.DataCubeException;
import com.raytheon.uf.common.inventory.tree.CubeLevel;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;

/**
 * Requestable data for derived data
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Mar 17, 2010           bsteffen    Initial creation
 * Jun 04, 2013  2041     bsteffen    Switch derived parameters to use
 *                                    concurrent python for threading.
 * Jan 14, 2014  2661     bsteffen    Make vectors u,v only
 * Jan 26, 2022  8741     njensen     Added performance logging
 * Jul 15, 2024  2037624  mapeters    Override getTimeAndSpace to check for
 *                                    virtual dependencies
 *
 * </pre>
 *
 * @author bsteffen
 */
public class DerivedRequestableData extends AbstractRequestableData {

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler("DerivedRequestableData");

    private final Map<Object, WeakReference<DerivedParameterRequest>> cache = Collections
            .synchronizedMap(
                    new HashMap<Object, WeakReference<DerivedParameterRequest>>());

    private DerivedParameterRequest request;

    public DerivedRequestableData(AbstractRequestableData baseRequester,
            DerivedParameterRequest request) {
        super(baseRequester);
        this.request = request;
    }

    public DerivedRequestableData(DerivedParameterRequest request) {
        this.request = request;
    }

    public DerivedParameterRequest getRequest() {
        return request;
    }

    @Override
    public Object getDataValue(Object arg) throws DataCubeException {
        DerivedParameterRequest request = createDerparRequest(arg);
        try {
            List<IDataRecord> finalResult = DerivedParameterGenerator
                    .calculate(request);
            if (finalResult != null && !finalResult.isEmpty()) {
                for (IDataRecord rec : finalResult) {
                    rec.setName(request.getParameterAbbreviation());
                }
                return finalResult.toArray(new IDataRecord[0]);
            }
        } catch (ExecutionException e) {
            throw new DataCubeException("Error executing Derived Parameter.",
                    e);
        }
        return null;
    }

    /**
     *
     * @param obj
     *            the pdo which needs to be derived
     * @param cache
     *            a map of data uri's to objects which have already been
     *            retrieved
     * @return
     * @throws VizException
     */
    private synchronized DerivedParameterRequest createDerparRequest(Object arg)
            throws DataCubeException {
        if (cache.containsKey(arg)) {
            DerivedParameterRequest request = cache.get(arg).get();
            if (request != null) {
                return request;
            }
        }
        DerivedParameterRequest request = new DerivedParameterRequest(
                this.request);
        List<Object> baseParams = request.getBaseParams();
        List<Object> arguments = new ArrayList<>(baseParams.size());
        for (Object param : baseParams) {
            arguments.add(getArgument(param, arg));
        }
        request.setArgumentRecords(arguments.toArray(new Object[] {}));
        cache.put(arg, new WeakReference<>(request));
        return request;
    }

    private Object getArgument(Object param, Object frameworkArg)
            throws DataCubeException {
        if (param instanceof DerivedRequestableData) {
            return ((DerivedRequestableData) param)
                    .createDerparRequest(frameworkArg);
        } else if (param instanceof AggregateRequestableData) {
            List<AbstractRequestableData> recs = ((AggregateRequestableData) param)
                    .getSourceRecords();
            List<Object> arg = new ArrayList<>(recs.size());
            for (AbstractRequestableData rec : recs) {
                arg.add(getArgument(rec, frameworkArg));
            }
            return arg;
        } else if (param instanceof CubeRequestableData) {
            long cubeStart = System.currentTimeMillis();
            Map<Level, CubeLevel<AbstractRequestableData, AbstractRequestableData>> dataMap = ((CubeRequestableData) param)
                    .getDataMap();
            List<CubeLevel<Object, Object>> arg = new ArrayList<>(
                    dataMap.size());
            long totalPressureTime = 0;
            long totalParameterTime = 0;
            Collection<CubeLevel<AbstractRequestableData, AbstractRequestableData>> cubeLevels = dataMap
                    .values();
            int cubeLevelCount = cubeLevels.size();
            for (CubeLevel<AbstractRequestableData, AbstractRequestableData> cubeLevel : cubeLevels) {
                if (cubeLevel.getParam() != null
                        && cubeLevel.getPressure() != null) {
                    long t0 = System.currentTimeMillis();
                    Object argPressure = getArgument(cubeLevel.getPressure(),
                            frameworkArg);
                    long t1 = System.currentTimeMillis();
                    Object argParam = getArgument(cubeLevel.getParam(),
                            frameworkArg);
                    long t2 = System.currentTimeMillis();
                    arg.add(new CubeLevel<>(argPressure, argParam));
                    totalPressureTime += t1 - t0;
                    totalParameterTime += t2 - t1;
                }
            }
            long cubeEnd = System.currentTimeMillis();
            StringBuilder sb = new StringBuilder();
            sb.append("Building cube of ").append(cubeLevelCount)
                    .append(" levels took ").append(cubeEnd - cubeStart)
                    .append(" ms with pressure taking ")
                    .append(totalPressureTime)
                    .append(" ms and parameter taking ")
                    .append(totalParameterTime).append(" ms");
            perfLog.log(sb.toString());
            return arg;
        } else if (param instanceof AbstractRequestableData) {
            return ((AbstractRequestableData) param).getDataValue(frameworkArg);
        } else if (param instanceof float[]
                || param instanceof FloatDataRecord) {
            return param;
        }
        throw new DataCubeException(
                "Unknown BaseParam for DerivedParam of type: "
                        + param.getClass().getSimpleName());
    }

    @Override
    public TimeAndSpace getTimeAndSpace() {
        /*
         * If any dependency is using virtual data, then the derived data is
         * virtual as well. So check for virtual dependencies first and use
         * their time and space if possible.
         */
        TimeAndSpace tas = super.getTimeAndSpace();
        Set<TimeAndSpace> virtualTasSet = new HashSet<>();
        for (AbstractRequestableData dep : getDependencies()) {
            TimeAndSpace depTas = dep.getTimeAndSpace();
            if (depTas.isVirtual()) {
                virtualTasSet.add(depTas);
            }
        }
        if (virtualTasSet.size() == 1) {
            TimeAndSpace virtTas = virtualTasSet.iterator().next();
            if (virtTas.matches(tas)) {
                tas = virtTas;
            } else {
                throw new RuntimeException(
                        "Virtual dependency's time/space don't match derived param's time/space for "
                                + this + ": Dependency: " + virtTas
                                + "; Derived: " + tas);
            }
        } else if (virtualTasSet.size() > 1) {
            throw new RuntimeException("Different virtual dependencies used by "
                    + this + ": " + virtualTasSet);
        }

        return tas;
    }

    @Override
    public List<AbstractRequestableData> getDependencies() {
        List<AbstractRequestableData> results = new ArrayList<>();
        for (Object param : request.getBaseParams()) {
            if (param instanceof AbstractRequestableData) {
                results.add((AbstractRequestableData) param);
            }
        }
        return results;
    }
}
