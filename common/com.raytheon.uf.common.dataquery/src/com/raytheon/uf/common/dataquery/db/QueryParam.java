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

package com.raytheon.uf.common.dataquery.db;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;

import com.raytheon.uf.common.dataplugin.annotations.NullFloat;
import com.raytheon.uf.common.dataplugin.annotations.NullString;

/**
 * Encapsulates the query parameters for a database query.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 29, 2008 875        bphillip    Initial Creation
 * Oct 07, 2013 2392       rjpeter     Updated to auto handle passing a null value to an equal operand.
 * Aug 20, 2015 4360       rferrel     Created {@link #checkForNullValueReplacement(String, String)} to determine value to use in place of null.
 * Sep 21, 2015 4486       rjpeter     Update checkForNullValueReplacement to handle null classname.
 * Jun 30, 2016 5725       tgurney     Add NOT IN
 * Mar 06, 2017 6142       bsteffen    Support isNull with null replacement.
 * </pre>
 * 
 * @author bphillip
 */
public class QueryParam {

    /** Enumeration containing the logic operands */
    public enum QueryOperand {
        EQUALS,
        NOTEQUALS,
        LESSTHAN,
        LESSTHANEQUALS,
        GREATERTHAN,
        GREATERTHANEQUALS,
        IN,
        LIKE,
        ILIKE,
        BETWEEN,
        ISNULL,
        ISNOTNULL,
        NOTIN
    };

    /**
     * A mapping between the enumeration and the string representation of an
     * operand
     */
    private static HashMap<String, QueryOperand> operandMap = new HashMap<>();
    static {
        operandMap.put("=", QueryOperand.EQUALS);
        operandMap.put("!=", QueryOperand.NOTEQUALS);
        operandMap.put("<", QueryOperand.LESSTHAN);
        operandMap.put("<=", QueryOperand.LESSTHANEQUALS);
        operandMap.put(">", QueryOperand.GREATERTHAN);
        operandMap.put(">=", QueryOperand.GREATERTHANEQUALS);
        operandMap.put("in", QueryOperand.IN);
        operandMap.put("like", QueryOperand.LIKE);
        operandMap.put("ilike", QueryOperand.ILIKE);
        operandMap.put("between", QueryOperand.BETWEEN);
        operandMap.put("isNotNull", QueryOperand.ISNOTNULL);
        operandMap.put("isNull", QueryOperand.ISNULL);
        operandMap.put("not in", QueryOperand.NOTIN);
    }

    /** The query field */
    private String field;

    /** The query value */
    private Object value;

    /** The query operand */
    private String operand = "=";

    private String className;

    /**
     * Creates a new QueryParam. Operand defaults to equals, unless value is
     * null, then operand is isNull.
     * 
     * @param field
     *            The field
     * @param value
     *            The value
     */
    public QueryParam(String field, Object value) {
        this(field, value, "=", null);
    }

    /**
     * Creates a new QueryParam. If value is null and operand is =, operand is
     * updated to isNull.
     * 
     * @param field
     *            The field
     * @param value
     *            The value
     * @param operand
     *            The operand
     */
    public QueryParam(String field, Object value, String operand) {
        this(field, value, operand, null);
    }

    /**
     * Creates a new QueryParam. If value is null and operand is =, operand is
     * updated to isNull.
     * 
     * @param field
     * @param value
     * @param operand
     * @param className
     */
    public QueryParam(String field, Object value, String operand,
            String className) {
        this.field = field;
        this.value = value;

        if ("isNull".equals(operand)
                || (value == null && "=".equals(operand))) {
            checkForNullValueReplacement(field, className);
        } else {
            this.operand = operand;
        }

        this.className = className;
    }

    /**
     * Creates a new QueryParam. If value is null and operand is =, operand is
     * updated to isNull.
     * 
     * @param field
     * @param value
     * @param operand
     */
    public QueryParam(String field, Object value, QueryOperand operand) {
        this(field, value, operand, null);
    }

    /**
     * Creates a new QueryParam. If value is null and operand is =, operand is
     * updated to isNull.
     * 
     * @param field
     * @param value
     * @param operand
     * @param className
     */
    public QueryParam(String field, Object value, QueryOperand operand,
            String className) {
        this.field = field;
        this.value = value;

        if (QueryOperand.ISNULL.equals(operand)
                || (value == null && QueryOperand.EQUALS.equals(operand))) {
            checkForNullValueReplacement(field, className);
        } else {
            this.operand = QueryParam.reverseTranslateOperand(operand);
        }

        this.className = className;
    }

    /**
     * Translates the string representation of an operand to the enumeration
     * value
     * 
     * @param operand
     *            The string representation of an operand
     * @return The enumeration value of the operand
     */
    public static QueryOperand translateOperand(String operand) {
        return operandMap.get(operand);
    }

    public static String reverseTranslateOperand(QueryOperand operand) {
        String key = null;
        for (Iterator<String> it = operandMap.keySet().iterator(); it
                .hasNext();) {
            key = it.next();
            if (operandMap.get(key).equals(operand)) {
                return key;
            }
        }
        return "=";
    }

    @Override
    public String toString() {
        return new StringBuffer().append(field).append(" ").append(this.operand)
                .append(" ").append(this.value).toString();
    }

    public String getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }

    public String getOperand() {
        return operand;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setOperand(String operand) {
        this.operand = operand;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Determine for the field if there is an annotation to override the null
     * value.
     * 
     * @param fieldStr
     * @param className
     */
    private void checkForNullValueReplacement(String fieldStr,
            String className) {
        if (className == null) {
            this.operand = "isNull";
            return;
        }

        Class<?> clazz = null;
        this.operand = "=";
        try {
            clazz = Class.forName(className);
            String fieldName = null;

            // Drill down to the class that contains the field.
            while (fieldStr.contains(".")) {
                String[] tmp = fieldStr.split("\\.", 2);
                fieldName = tmp[0];
                fieldStr = tmp[1];
                clazz = clazz.getDeclaredField(fieldName).getType();
            }

            Field field = clazz.getDeclaredField(fieldStr);
            if (field.isAnnotationPresent(NullString.class)) {
                this.value = field.getAnnotation(NullString.class).value();
            } else if (field.isAnnotationPresent(NullFloat.class)) {
                this.value = field.getAnnotation(NullFloat.class).value();
            } else if (clazz.getDeclaredField(fieldStr).getType()
                    .equals(String.class)) {
                this.operand = "isNull";
            }
        } catch (ClassNotFoundException | SecurityException
                | IllegalArgumentException | NoSuchFieldException e) {
            if (clazz != null && clazz.equals(String.class)) {
                this.operand = "isNull";
            }
        }
    }
}
