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
package com.raytheon.uf.common.serialization.comm;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface for handling client requests to the server
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 03, 2009            mschenke    Initial creation
 * Feb 02, 2017  6111      njensen     Added getRequestType()
 * 
 * </pre>
 * 
 * @author mschenke
 */

public interface IRequestHandler<P extends IServerRequest> {

    /**
     * This function will be executed on the edex server and should expect a
     * certain request type
     * 
     * @param request
     *            the server request to process
     * @return An object which the caller of the handler will be expecting
     * @throws Exception
     *             all exceptions will be caught by the server and the message
     *             will be returned to the client
     */
    public Object handleRequest(P request) throws Exception;

    /**
     * Uses reflection to get the concrete type of P (see above
     * IRequestHandler<P extends IServerRequest). This is a convenience method
     * to allow automatic detection of the IServerRequest that should be tied to
     * this instance of the IRequestHandler. This is not safe if P is an
     * interface, an abstract class, or there are multiple handleRequest(P)
     * methods in a single class due to inheritance. If necessary, you can
     * implement this method to return P.class.
     * 
     * @return the concrete class of the IServerRequest P, or null if it cannot
     *         be determined
     * @author bsteffen
     */
    public default Class<?> getRequestType() {
        Class<?> clazz = this.getClass();
        /* Simple case that this class directly implements the interface */
        for (Type interfaze : clazz.getGenericInterfaces()) {
            if (interfaze instanceof ParameterizedType) {
                ParameterizedType parameterizedInterface = (ParameterizedType) interfaze;
                if (parameterizedInterface.getRawType()
                        .equals(IRequestHandler.class)) {
                    Type actualParameterType = parameterizedInterface
                            .getActualTypeArguments()[0];
                    if (actualParameterType instanceof Class) {
                        return (Class<?>) actualParameterType;
                    }
                    return null;

                }
            }
        }
        Type superType = clazz.getGenericSuperclass();
        if (!(superType instanceof ParameterizedType)) {
            /*
             * The type of request must be a generic parameter on the super
             * type. Which means that the type of request to handle is not
             * inherited, it must be declared directly on this class.
             */
            return null;
        }

        Class<?> superClass = clazz.getSuperclass();
        Type[] actualTypeArguments = ((ParameterizedType) superType)
                .getActualTypeArguments();
        TypeVariable<?>[] typeParameters = superClass.getTypeParameters();

        Map<Type, Type> resolvedTypes = new HashMap<>();
        for (int i = 0; i < actualTypeArguments.length; i++) {
            resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
        }
        /*
         * Loop over the super types, matching generic parameters on the class
         * to generic parameters on the superclass. The loop terminates if no
         * generic parameters can be matched.
         */
        while (!resolvedTypes.isEmpty()) {
            for (Type interfaze : superClass.getGenericInterfaces()) {
                if (interfaze instanceof ParameterizedType) {
                    ParameterizedType parameterizedInterface = (ParameterizedType) interfaze;
                    if (parameterizedInterface.getRawType()
                            .equals(IRequestHandler.class)) {
                        Type actualParameterType = parameterizedInterface
                                .getActualTypeArguments()[0];
                        actualParameterType = resolvedTypes
                                .get(actualParameterType);
                        if (actualParameterType instanceof Class) {
                            return (Class<?>) actualParameterType;
                        }
                        return null;
                    }
                }
            }
            superType = clazz.getGenericSuperclass();
            if (!(superType instanceof ParameterizedType)) {
                return null;
            }
            superClass = superClass.getSuperclass();
            actualTypeArguments = ((ParameterizedType) superType)
                    .getActualTypeArguments();
            typeParameters = superClass.getTypeParameters();
            /*
             * Match generic parameters on this superclass back to the
             * parameters on the original class.
             */
            Map<Type, Type> deepResolvedTypes = new HashMap<>();
            for (int i = 0; i < actualTypeArguments.length; i++) {
                Type resolvedType = resolvedTypes.get(actualTypeArguments[i]);
                if (resolvedType != null) {
                    deepResolvedTypes.put(typeParameters[i], resolvedType);
                }
            }
            resolvedTypes = deepResolvedTypes;
        }
        return null;
    }
}
