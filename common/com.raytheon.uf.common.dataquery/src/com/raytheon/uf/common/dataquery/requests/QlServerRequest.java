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
package com.raytheon.uf.common.dataquery.requests;

import java.util.Map;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * IServerRequest object for making a sql or hql request.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Feb 16, 2011  8070     ekladstrup  Initial creation
 * Dec 18, 2013  2579     bsteffen    Remove ISerializableObject
 * Jul 13, 2015  4500     rjpeter     Added paramMap, demystified interface.
 * Oct 12, 2015  4932     njensen     Truncate toString() on long maps queries
 * Jan 31, 2018  6945     tgurney     Add maxResults
 *
 * </pre>
 *
 * @author ekladstrup
 */
@DynamicSerialize
public class QlServerRequest implements IServerRequest {

    /** The language of the query */
    public static enum QueryLanguage {
        SQL, HQL
    }

    /** The type of the query */
    public static enum QueryType {
        QUERY, STATEMENT
    }

    /**
     * The language to use. If not specified will default to sql.
     */
    @DynamicSerializeElement
    private QueryLanguage lang;

    /**
     * The type the of sql/hql. If not specified will default to query.
     */
    @DynamicSerializeElement
    private QueryType type;

    /**
     * The database to run the query against. If not specified will default to
     * metadata.
     */
    @DynamicSerializeElement
    private String database;

    @DynamicSerializeElement
    private String query;

    @DynamicSerializeElement
    private Map<String, Object> paramMap;

    /**
     * Maximum number of rows/objects to return. null or less than 1 means
     * unlimited. Only used for HQL queries
     */
    @DynamicSerializeElement
    private Integer maxResults;

    public QlServerRequest() {

    }

    public QlServerRequest(String query) {
        this.query = query;
    }

    public QlServerRequest(String query, Map<String, Object> paramMap) {
        this.query = query;
        this.paramMap = paramMap;
    }

    public QueryLanguage getLang() {
        return lang;
    }

    public void setLang(QueryLanguage lang) {
        this.lang = lang;
    }

    public QueryType getType() {
        return type;
    }

    public void setType(QueryType type) {
        this.type = type;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, Object> getParamMap() {
        return paramMap;
    }

    public void setParamMap(Map<String, Object> paramMap) {
        this.paramMap = paramMap;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    @Override
    public String toString() {
        StringBuilder msg = new StringBuilder(80);
        msg.append("QlServerRequest [");
        if (database != null) {
            msg.append("database=").append(database).append(", ");
        }
        msg.append("query=");
        if ("maps".equals(database) && query.length() > 80) {
            msg.append(query.substring(0, 80)).append("...");
        } else {
            msg.append(query);
        }
        msg.append(']');

        return msg.toString();
    }
}
