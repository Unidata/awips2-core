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
package com.raytheon.uf.edex.esb.camel;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.component.bean.DefaultParameterMappingStrategy;
import org.apache.camel.component.bean.ParameterMappingStrategy;

import com.raytheon.edex.esb.Headers;

/**
 * Camel Parameter Mapping Strategy to support receiving message headers in bean
 * method invocations in addition to the message object.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 5, 2009             chammack    Initial creation
 * Mar 4, 2021  8326       tgurney     Rewrite for Camel 3
 *
 * </pre>
 *
 */

public class EDEXParameterMappingStrategy implements ParameterMappingStrategy {

    @Override
    public Expression getDefaultParameterTypeExpression(
            Class<?> parameterType) {
        if (Headers.class.equals(parameterType)) {
            return new HeadersExpression();
        }
        return DefaultParameterMappingStrategy.INSTANCE
                .getDefaultParameterTypeExpression(parameterType);
    }

    private static class HeadersExpression implements Expression {
        @SuppressWarnings("unchecked")
        @Override
        public <T> T evaluate(Exchange exchange, Class<T> c) {
            Headers edexHeaders = new Headers();

            Map<String, Object> camelHeaders = exchange.getIn().getHeaders();
            if (camelHeaders != null) {
                for (Entry<String, Object> e : camelHeaders.entrySet()) {
                    String k = e.getKey();
                    Object v = e.getValue();
                    if ("CamelFileName".equalsIgnoreCase(k)) {
                        edexHeaders.put("traceId", v);
                    } else {
                        edexHeaders.put(k, v);
                    }

                }
            }
            return (T) edexHeaders;
        }

        @Override
        public String toString() {
            return "headers";
        }
    }

}
