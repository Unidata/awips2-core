/**********************************************************************
 *
 * The following software products were developed by Raytheon:
 *
 * ADE (AWIPS Development Environment) software
 * CAVE (Common AWIPS Visualization Environment) software
 * EDEX (Environmental Data Exchange) software
 * uFrame™ (Universal Framework) software
 *
 * Copyright (c) 2010 Raytheon Co.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/epl-v10.php
 *
 *
 * Contractor Name: Raytheon Company
 * Contractor Address:
 * 6825 Pine Street, Suite 340
 * Mail Stop B8
 * Omaha, NE 68106
 * 402.291.0100
 *
 **********************************************************************/
/**
 * 
 */
package com.raytheon.uf.common.json.jackson;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Object pool of ObjectMapper instances for serializing and deserializing
 * Geometry objects to and from GeoJSON format.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *                         bclement     Initial creation.
 * Oct 22, 2015  #5004     dgilling     Use commons-pool2 API.
 * 
 * </pre>
 * 
 * @author bclement
 */
public class JacksonPool implements KeyedObjectPool<Long, ObjectMapper> {

    private final ConcurrentMap<Long, ClassLoader> classLoaderPool;

    private final boolean poolClassloaders;

    private final GenericKeyedObjectPool<Long, ObjectMapper> objectPool;

    public JacksonPool() {
        this(new JacksonFactory(), false);
    }

    public JacksonPool(JacksonFactory jFactory) {
        this(jFactory, false);
    }

    public JacksonPool(boolean poolClassloaders) {
        this(new JacksonFactory(), poolClassloaders);
    }

    private JacksonPool(JacksonFactory jFactory, boolean poolClassloaders) {
        this.objectPool = new GenericKeyedObjectPool<>(jFactory);
        this.poolClassloaders = poolClassloaders;
        this.classLoaderPool = new ConcurrentHashMap<>();
    }

    @Override
    public void addObject(Long arg0) throws Exception, IllegalStateException,
            UnsupportedOperationException {
        objectPool.addObject(arg0);
    }

    @Override
    public ObjectMapper borrowObject(Long arg0) throws Exception,
            NoSuchElementException, IllegalStateException {
        ObjectMapper retVal = objectPool.borrowObject(arg0);

        if (poolClassloaders) {
            classLoaderPool.put(arg0, Thread.currentThread()
                    .getContextClassLoader());
        }

        return retVal;
    }

    @Override
    public void clear() throws Exception, UnsupportedOperationException {
        objectPool.clear();
    }

    @Override
    public void clear(Long arg0) throws Exception,
            UnsupportedOperationException {
        objectPool.clear(arg0);
    }

    @Override
    public void close() {
        objectPool.close();
    }

    @Override
    public int getNumActive() {
        return objectPool.getNumActive();
    }

    @Override
    public int getNumActive(Long arg0) {
        return objectPool.getNumActive(arg0);
    }

    @Override
    public int getNumIdle() {
        return objectPool.getNumIdle();
    }

    @Override
    public int getNumIdle(Long arg0) {
        return objectPool.getNumIdle(arg0);
    }

    @Override
    public void invalidateObject(Long arg0, ObjectMapper arg1) throws Exception {
        objectPool.invalidateObject(arg0, arg1);
    }

    @Override
    public void returnObject(Long arg0, ObjectMapper arg1) throws Exception {
        if (poolClassloaders) {
            ClassLoader classLoader = classLoaderPool.get(arg0);
            if (classLoader == null) {
                throw new Exception("Unable to find previous class loader for "
                        + arg0);
            }
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        objectPool.returnObject(arg0, arg1);
    }
}
