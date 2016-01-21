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

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

/**
 * A {@code KeyedPoolableObjectFactory} implementation for building ObjectMapper
 * instances for serializing and deserializing {@code Geometry} objects to and
 * from GeoJSON format.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *                         bclement     Initial creation
 * Oct 23, 2015  #5004     dgilling     Update to use commons-pool2 API.
 * Oct 27, 2015  4767      bclement     upgraded jackson to 1.9
 * Jan 19, 2016  5067      bclement    upgrade jackson to 2.6
 * 
 * </pre>
 * 
 * @author bclement
 */
public class JacksonFactory extends
        BaseKeyedPooledObjectFactory<Long, ObjectMapper> {


    @Override
    public ObjectMapper create(Long arg0) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping();
        mapper.registerModule(new JaxbAnnotationModule());
        mapper.registerModule(new GeoJsonModule());
        return mapper;
    }

    @Override
    public PooledObject<ObjectMapper> wrap(ObjectMapper arg0) {
        return new DefaultPooledObject<ObjectMapper>(arg0);
    }

}
