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
package com.raytheon.uf.edex.database.health;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.SQLQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;

/**
 * Postgres implemetation of Database Bloat checking.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 10, 2016 4630       rjpeter     Initial creation
 * Jun 20, 2016 5679       rjpeter     Add admin database account
 * Sep 08, 2017 DR 20135   D. Friedman Rebuild constraint-backing indexes concurrently
 * Jun 29, 2018 20505      ryu         Attempt to obtain access exclusive locks before altering tables.
 * Aug 06, 2018 20505      edebebe     Re-factored the 'reindex()' method to speed up the table re-indexing
 *                                     logic and added three inner classes: 'ReindexJob', 'Action', 'WorkerThread'.
 * </pre>
 *
 * @author rjpeter
 */

public class PostgresBloatDao extends CoreDao implements BloatDao {

    /**
     * Pulled from github. Modified to only retrieve schema, table, real size,
     * bloat size, and bloat percent, and to exclude system tables / indexes.
     *
     * https://github.com/ioguix/pgsql-bloat-estimation
     *
     * This query is compatible with PostgreSQL 9.0 and more
     *
     * <pre>
     * SELECT schemaname, tblname, bs*tblpages AS real_size, (tblpages-est_tblpages_ff)*bs AS bloat_size,
     *   CASE WHEN tblpages - est_tblpages_ff > 0
     *     THEN 100 * (tblpages - est_tblpages_ff)/tblpages::float
     *     ELSE 0
     *   END AS bloat_ratio
     * FROM (
     *   SELECT ceil( reltuples / ( (bs-page_hdr)/tpl_size ) ) + ceil( toasttuples / 4 ) AS est_tblpages,
     *     ceil( reltuples / ( (bs-page_hdr)*fillfactor/(tpl_size*100) ) ) + ceil( toasttuples / 4 ) AS est_tblpages_ff,
     *     tblpages, fillfactor, bs, tblid, schemaname, tblname, heappages, toastpages, is_na
     *   FROM (
     *     SELECT
     *       ( 4 + tpl_hdr_size + tpl_data_size + (2*ma)
     *         - CASE WHEN tpl_hdr_size%ma = 0 THEN ma ELSE tpl_hdr_size%ma END
     *         - CASE WHEN ceil(tpl_data_size)::int%ma = 0 THEN ma ELSE ceil(tpl_data_size)::int%ma END
     *       ) AS tpl_size, bs - page_hdr AS size_per_block, (heappages + toastpages) AS tblpages, heappages,
     *       toastpages, reltuples, toasttuples, bs, page_hdr, tblid, schemaname, tblname, fillfactor, is_na
     *     FROM (
     *       SELECT
     *         tbl.oid AS tblid, ns.nspname AS schemaname, tbl.relname AS tblname, tbl.reltuples,
     *         tbl.relpages AS heappages, coalesce(toast.relpages, 0) AS toastpages,
     *         coalesce(toast.reltuples, 0) AS toasttuples,
     *         coalesce(substring(
     *           array_to_string(tbl.reloptions, ' ')
     *           FROM '%fillfactor=#"__#"%' FOR '#')::smallint, 100) AS fillfactor,
     *         current_setting('block_size')::numeric AS bs,
     *         CASE WHEN version()~'mingw32' OR version()~'64-bit|x86_64|ppc64|ia64|amd64' THEN 8 ELSE 4 END AS ma,
     *         24 AS page_hdr,
     *         23 + CASE WHEN MAX(coalesce(null_frac,0)) > 0 THEN ( 7 + count(*) ) / 8 ELSE 0::int END
     *           + CASE WHEN tbl.relhasoids THEN 4 ELSE 0 END AS tpl_hdr_size,
     *         sum( (1-coalesce(s.null_frac, 0)) * coalesce(s.avg_width, 1024) ) AS tpl_data_size,
     *         bool_or(att.atttypid = 'pg_catalog.name'::regtype) AS is_na
     *       FROM pg_attribute AS att
     *         JOIN pg_class AS tbl ON att.attrelid = tbl.oid
     *         JOIN pg_namespace AS ns ON ns.oid = tbl.relnamespace
     *         JOIN pg_stats AS s ON s.schemaname=ns.nspname
     *           AND s.tablename = tbl.relname AND s.inherited=false AND s.attname=att.attname
     *         LEFT JOIN pg_class AS toast ON tbl.reltoastrelid = toast.oid
     *       WHERE att.attnum > 0 AND NOT att.attisdropped
     *         AND tbl.relkind = 'r'
     *         AND ns.nspname NOT IN ('pg_catalog', 'information_schema')
     *       GROUP BY 1,2,3,4,5,6,7,8,9,10, tbl.relhasoids
     *       ORDER BY 2,3
     *     ) AS s
     *   ) AS s2
     * ) AS s3
     * WHERE NOT is_na
     * order by 1, 2;
     * </pre>
     */
    private static final String TABLE_BLOAT_SQL = "SELECT schemaname, tblname, bs*(tblpages)::bigint AS real_size, (tblpages-est_tblpages_ff)*bs AS bloat_size, "
            + "CASE WHEN tblpages - est_tblpages_ff > 0 THEN 100 * (tblpages - est_tblpages_ff)/tblpages::float "
            + "ELSE 0 END AS bloat_ratio FROM ("
            + "SELECT ceil( reltuples / ( (bs-page_hdr)/tpl_size ) ) + ceil( toasttuples / 4 ) AS est_tblpages, "
            + "ceil( reltuples / ( (bs-page_hdr)*fillfactor/(tpl_size*100) ) ) + ceil( toasttuples / 4 ) AS est_tblpages_ff, "
            + "tblpages, fillfactor, bs, tblid, schemaname, tblname, heappages, toastpages, is_na "
            + "FROM ( SELECT ( 4 + tpl_hdr_size + tpl_data_size + (2*ma) "
            + "- CASE WHEN tpl_hdr_size%ma = 0 THEN ma ELSE tpl_hdr_size%ma END "
            + "- CASE WHEN ceil(tpl_data_size)::int%ma = 0 THEN ma ELSE ceil(tpl_data_size)::int%ma END "
            + ") AS tpl_size, bs - page_hdr AS size_per_block, (heappages + toastpages) AS tblpages, heappages, "
            + "toastpages, reltuples, toasttuples, bs, page_hdr, tblid, schemaname, tblname, fillfactor, is_na "
            + "FROM ( SELECT tbl.oid AS tblid, ns.nspname AS schemaname, tbl.relname AS tblname, tbl.reltuples, "
            + "tbl.relpages AS heappages, coalesce(toast.relpages, 0) AS toastpages, "
            + "coalesce(toast.reltuples, 0) AS toasttuples, coalesce(substring(array_to_string(tbl.reloptions, ' ') "
            + "FROM '%fillfactor=#\"__#\"%' FOR '#')::smallint, 100) AS fillfactor, current_setting('block_size')::numeric AS bs, "
            + "CASE WHEN version()~'mingw32' OR version()~'64-bit|x86_64|ppc64|ia64|amd64' THEN 8 ELSE 4 END AS ma, "
            + "24 AS page_hdr, 23 + CASE WHEN MAX(coalesce(null_frac,0)) > 0 THEN ( 7 + count(*) ) / 8 ELSE 0::int END "
            + "+ CASE WHEN tbl.relhasoids THEN 4 ELSE 0 END AS tpl_hdr_size, "
            + "sum( (1-coalesce(s.null_frac, 0)) * coalesce(s.avg_width, 1024) ) AS tpl_data_size, "
            + "bool_or(att.atttypid = 'pg_catalog.name'::regtype) AS is_na "
            + "FROM pg_attribute AS att JOIN pg_class AS tbl ON att.attrelid = tbl.oid "
            + "JOIN pg_namespace AS ns ON ns.oid = tbl.relnamespace "
            + "JOIN pg_stats AS s ON s.schemaname=ns.nspname "
            + "AND s.tablename = tbl.relname AND s.inherited=false AND s.attname=att.attname "
            + "LEFT JOIN pg_class AS toast ON tbl.reltoastrelid = toast.oid "
            + "WHERE att.attnum > 0 AND NOT att.attisdropped  AND tbl.relkind = 'r' AND ns.nspname NOT IN ('pg_catalog', 'information_schema') "
            + "GROUP BY 1,2,3,4,5,6,7,8,9,10, tbl.relhasoids ORDER BY 2,3) AS s) AS s2) AS s3 WHERE NOT is_na order by 1, 2";

    /**
     * Pulled from github. Modified to only retrieve schema, table, index, real
     * size, bloat size, and bloat percent, and to exclude system tables /
     * indexes.
     *
     * https://github.com/ioguix/pgsql-bloat-estimation
     *
     * This query is compatible with PostgreSQL 9.0 and more
     *
     * <pre>
     * SELECT nspname AS schemaname, tblname, idxname, bs*(relpages)::bigint AS real_size,
     *   bs*(relpages-est_pages_ff) AS bloat_size,
     *   100 * (relpages-est_pages_ff)::float / relpages AS bloat_ratio
     * FROM (
     *   SELECT coalesce(1 +
     *        ceil(reltuples/floor((bs-pageopqdata-pagehdr)/(4+nulldatahdrwidth)::float)), 0 -- ItemIdData size + computed avg size of a tuple (nulldatahdrwidth)
     *     ) AS est_pages,
     *     coalesce(1 +
     *        ceil(reltuples/floor((bs-pageopqdata-pagehdr)*fillfactor/(100*(4+nulldatahdrwidth)::float))), 0
     *     ) AS est_pages_ff,
     *     bs, nspname, table_oid, tblname, idxname, relpages, fillfactor, is_na
     *   FROM (
     *     SELECT maxalign, bs, nspname, tblname, idxname, reltuples, relpages, relam, table_oid, fillfactor,
     *       ( index_tuple_hdr_bm +
     *           maxalign - CASE -- Add padding to the index tuple header to align on MAXALIGN
     *             WHEN index_tuple_hdr_bm%maxalign = 0 THEN maxalign
     *             ELSE index_tuple_hdr_bm%maxalign
     *           END
     *         + nulldatawidth + maxalign - CASE -- Add padding to the data to align on MAXALIGN
     *             WHEN nulldatawidth = 0 THEN 0
     *             WHEN nulldatawidth::integer%maxalign = 0 THEN maxalign
     *             ELSE nulldatawidth::integer%maxalign
     *           END
     *       )::numeric AS nulldatahdrwidth, pagehdr, pageopqdata, is_na
     *     FROM (
     *       SELECT
     *         i.nspname, i.tblname, i.idxname, i.reltuples, i.relpages, i.relam, a.attrelid AS table_oid,
     *         current_setting('block_size')::numeric AS bs, fillfactor,
     *         CASE -- MAXALIGN: 4 on 32bits, 8 on 64bits (and mingw32 ?)
     *           WHEN version() ~ 'mingw32' OR version() ~ '64-bit|x86_64|ppc64|ia64|amd64' THEN 8
     *           ELSE 4
     *         END AS maxalign,
     *         -- per page header, fixed size: 20 for 7.X, 24 for others
     *         24 AS pagehdr,
     *         -- per page btree opaque data
     *         16 AS pageopqdata,
     *         -- per tuple header: add IndexAttributeBitMapData if some cols are null-able
     *         CASE WHEN max(coalesce(s.null_frac,0)) = 0
     *           THEN 2 -- IndexTupleData size
     *           ELSE 2 + (( 32 + 8 - 1 ) / 8) -- IndexTupleData size + IndexAttributeBitMapData size ( max num filed per index + 8 - 1 /8)
     *         END AS index_tuple_hdr_bm,
     *         -- data len: we remove null values save space using it fractionnal part from stats
     *         sum( (1-coalesce(s.null_frac, 0)) * coalesce(s.avg_width, 1024)) AS nulldatawidth,
     *         max( CASE WHEN a.atttypid = 'pg_catalog.name'::regtype THEN 1 ELSE 0 END ) > 0 AS is_na
     *       FROM pg_attribute AS a
     *         JOIN (
     *           SELECT nspname, tbl.relname AS tblname, idx.relname AS idxname, idx.reltuples, idx.relpages, idx.relam,
     *             indrelid, indexrelid, indkey::smallint[] AS attnum,
     *             coalesce(substring(
     *               array_to_string(idx.reloptions, ' ')
     *                from 'fillfactor=([0-9]+)')::smallint, 90) AS fillfactor
     *           FROM pg_index
     *             JOIN pg_class idx ON idx.oid=pg_index.indexrelid
     *             JOIN pg_class tbl ON tbl.oid=pg_index.indrelid
     *             JOIN pg_namespace ON pg_namespace.oid = idx.relnamespace
     *           WHERE pg_index.indisvalid AND tbl.relkind = 'r' AND idx.relpages > 0 AND nspname NOT IN ('pg_catalog', 'information_schema')
     *         ) AS i ON a.attrelid = i.indexrelid
     *         JOIN pg_stats AS s ON s.schemaname = i.nspname
     *           AND ((s.tablename = i.tblname AND s.attname = pg_catalog.pg_get_indexdef(a.attrelid, a.attnum, TRUE)) -- stats from tbl
     *           OR   (s.tablename = i.idxname AND s.attname = a.attname))-- stats from functionnal cols
     *         JOIN pg_type AS t ON a.atttypid = t.oid
     *       WHERE a.attnum > 0
     *       GROUP BY 1, 2, 3, 4, 5, 6, 7, 8, 9
     *     ) AS s1
     *   ) AS s2
     *     JOIN pg_am am ON s2.relam = am.oid WHERE am.amname = 'btree'
     * ) AS sub
     * WHERE NOT is_na
     * ORDER BY 1, 2, 3;
     * </pre>
     */
    private static final String INDEX_BLOAT_SQL = "SELECT nspname AS schemaname, tblname, idxname, bs*(relpages)::bigint AS real_size,  bs*(relpages-est_pages_ff) AS bloat_size, "
            + "100 * (relpages-est_pages_ff)::float / relpages AS bloat_ratio FROM (SELECT coalesce(1 + "
            + "ceil(reltuples/floor((bs-pageopqdata-pagehdr)/(4+nulldatahdrwidth)::float)), 0) AS est_pages, coalesce(1 + ceil(reltuples/floor((bs-pageopqdata-pagehdr)*fillfactor/(100* "
            + "(4+nulldatahdrwidth)::float))), 0) AS est_pages_ff, bs, nspname, table_oid, tblname, "
            + "idxname, relpages, fillfactor, is_na FROM (SELECT maxalign, bs, nspname, tblname, "
            + "idxname, reltuples, relpages, relam, table_oid, fillfactor, ( index_tuple_hdr_bm + "
            + "maxalign - CASE WHEN index_tuple_hdr_bm%maxalign = 0 THEN maxalign ELSE "
            + "index_tuple_hdr_bm%maxalign END + nulldatawidth + maxalign - CASE WHEN nulldatawidth = 0 "
            + "THEN 0 WHEN nulldatawidth::integer%maxalign = 0 THEN maxalign ELSE "
            + "nulldatawidth::integer%maxalign END)::numeric AS nulldatahdrwidth, pagehdr, pageopqdata, "
            + "is_na FROM (SELECT i.nspname, i.tblname, i.idxname, i.reltuples, i.relpages, i.relam, "
            + "a.attrelid AS table_oid, current_setting('block_size')::numeric AS bs, fillfactor, CASE "
            + "WHEN version() ~ 'mingw32' OR version() ~ '64-bit|x86_64|ppc64|ia64|amd64' THEN 8 "
            + "ELSE 4 END AS maxalign, 24 AS pagehdr, 16 AS pageopqdata, CASE WHEN max( "
            + "coalesce(s.null_frac,0)) = 0 THEN 2 ELSE 2 + (( 32 + 8 - 1 ) / 8) END AS "
            + "index_tuple_hdr_bm, sum( (1-coalesce(s.null_frac, 0)) * coalesce(s.avg_width, 1024)) AS "
            + "nulldatawidth, max( CASE WHEN a.atttypid = 'pg_catalog.name'::regtype THEN 1 ELSE 0 END ) "
            + "> 0 AS is_na FROM pg_attribute AS a JOIN (SELECT nspname, tbl.relname AS tblname, "
            + "idx.relname AS idxname, idx.reltuples, idx.relpages, idx.relam, indrelid, indexrelid, "
            + "indkey::smallint[] AS attnum, coalesce(substring(array_to_string(idx.reloptions, ' ') "
            + "from 'fillfactor=([0-9]+)')::smallint, 90) AS fillfactor FROM pg_index JOIN pg_class idx "
            + "ON idx.oid=pg_index.indexrelid JOIN pg_class tbl ON tbl.oid=pg_index.indrelid JOIN "
            + "pg_namespace ON pg_namespace.oid = idx.relnamespace WHERE pg_index.indisvalid AND "
            + "tbl.relkind = 'r' AND idx.relpages > 0 AND nspname NOT IN ('pg_catalog', 'information_schema')) "
            + "AS i ON a.attrelid = i.indexrelid JOIN pg_stats "
            + "AS s ON s.schemaname = i.nspname AND ((s.tablename = i.tblname AND s.attname = "
            + "pg_catalog.pg_get_indexdef(a.attrelid, a.attnum, TRUE)) OR (s.tablename = i.idxname AND "
            + "s.attname = a.attname)) JOIN pg_type AS t ON a.atttypid = t.oid WHERE a.attnum > 0 "
            + "GROUP BY 1, 2, 3, 4, 5, 6, 7, 8, 9) AS s1) AS s2 JOIN pg_am am ON s2.relam = am.oid WHERE "
            + "am.amname = 'btree') AS sub WHERE NOT is_na  ORDER BY 1, 2, 3;";

    /**
     * For a given name (table/constraint/index) in a schema, grab the oid and
     * index definition.
     */
    private static final String SELECT_INDEX_INFO = "select tbl.oid, pg_get_indexdef(tbl.oid) as indexdef "
            + "from pg_class tbl join pg_namespace ns "
            + "on ns.oid = tbl.relnamespace where tbl.relname = :name and ns.nspname = :schema";

    /**
     * For a given name, check if it is a constraint.
     */
    private static final String SELECT_CONSTRAINT_INFO = "select con.oid, con.contype "
            + "from pg_constraint con join pg_namespace ns "
            + "on ns.oid = con.connamespace where con.conname = :name and ns.nspname = :schema";

    /**
     * Fetch foreign constraints that use the same index as the given
     * constraint.
     */
    private static final String SELECT_INDEX_FCONS = "select "
            + "fcon.conname as fconname, fconrel.relname as fconrelname, fconns.nspname as fconns, "
            + "fcon.contype as fcontype, pg_get_constraintdef(fcon.oid) as fcondef "
            + "from pg_constraint fcon join pg_class fconrel on fcon.conrelid = fconrel.oid "
            + "join pg_namespace fconns on fcon.connamespace = fconns.oid "
            + "where fcon.conindid = :conindid";

    private static final String SELECT_INDEX_FCONS_EXCLUSION = " and fcon.oid <> :conid";

    protected final Pattern INDEX_REGEX = Pattern
            .compile("(.+?) INDEX \"?.+?\"? (ON .+)");

    protected final String database;

    private static final String REINDEXING_DELAY_PROPERTY = "database.health.reindex.delay";

    private final long delay;

    private final ReindexThread workerThread;

    public PostgresBloatDao(String database) {
        super(DaoConfig.forDatabase(database, true));
        this.database = database;

        this.delay = Long.getLong(REINDEXING_DELAY_PROPERTY, 60000);

        workerThread = ReindexThread.getInstance();
        if (!workerThread.isAlive()) {
            workerThread.setDaemon(true);
            workerThread.start();
        }
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public List<TableBloat> getTableBloatData() {
        Object[] rows = executeSQLQuery(TABLE_BLOAT_SQL);
        List<TableBloat> rval = null;

        if (rows != null) {
            rval = new ArrayList<>(rows.length);
            for (Object row : rows) {
                Object[] cols = (Object[]) row;
                TableBloat info = new TableBloat();
                info.setSchema(String.valueOf(cols[0]));
                info.setTableName(String.valueOf(cols[1]));
                info.setRealSizeBytes(((Number) cols[2]).longValue());
                info.setBloatBytes(((Number) cols[3]).longValue());
                info.setBloatPercent(((Number) cols[4]).doubleValue());
                rval.add(info);
            }
        } else {
            rval = Collections.emptyList();
        }

        return rval;
    }

    @Override
    public List<IndexBloat> getIndexBloatData() {
        Object[] rows = executeSQLQuery(INDEX_BLOAT_SQL);
        List<IndexBloat> rval = null;

        if (rows != null) {
            rval = new ArrayList<>(rows.length);
            for (Object row : rows) {
                Object[] cols = (Object[]) row;
                IndexBloat info = new IndexBloat();
                info.setSchema(String.valueOf(cols[0]));
                info.setTableName(String.valueOf(cols[1]));
                info.setIndexName(String.valueOf(cols[2]));
                info.setRealSizeBytes(((Number) cols[3]).longValue());
                info.setBloatBytes(((Number) cols[4]).longValue());
                info.setBloatPercent(((Number) cols[5]).doubleValue());
                rval.add(info);
            }
        } else {
            rval = Collections.emptyList();
        }

        return rval;
    }

    @Override
    public void vacuumTable(TableBloat info) {
        throw new IllegalArgumentException("Not Implemented");
    }

    @Override
    public void reindex(final IndexBloat info) {
        PostgresBloatDao myDao = this;
        txTemplate.execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                String schema = info.getSchema();
                String indexName = info.getIndexName();

                List<Action> actionListForReindexJob = new ArrayList<>();
                String fqnTableName = "\"" + schema + "\".\""
                        + info.getTableName() + "\"";
                String fqnIndexName = "\"" + schema + "\".\"" + indexName
                        + "\"";
                Map<String, Object> paramMap = new HashMap<>(2, 1);
                paramMap.put("name", indexName);
                paramMap.put("schema", schema);
                Session sess = getCurrentSession();

                SQLQuery query = sess.createSQLQuery(SELECT_INDEX_INFO)
                        .addScalar("oid", IntegerType.INSTANCE)
                        .addScalar("indexdef", StringType.INSTANCE);
                addParamsToQuery(query, paramMap);
                ScrollableResults queryResult = query.scroll();

                Integer indexId = null;
                String indexDef = null;
                if (queryResult.next()) {
                    indexId = queryResult.getInteger(0);
                    indexDef = queryResult.getString(1);
                }
                if (indexId == null || indexDef == null) {
                    logger.warn(
                            "Could not look up OID and definition for index: "
                                    + fqnIndexName);
                    return 0;
                }

                logger.info("Index definition: " + indexDef);
                Matcher matcher = INDEX_REGEX.matcher(indexDef);
                if (!matcher.matches()) {
                    logger.warn(
                            "Could not parse index definition.  Manual reindex required. Definition ["
                                    + indexDef + "]");
                    return 0;
                }

                /* update index name to a tmp name */
                String tmpName = TMP_INDEX_PREFIX + indexName;
                if (tmpName.length() > 64) {
                    tmpName.substring(0, 64);
                }
                String fqnTmpName = "\"" + schema + "\".\"" + tmpName + "\"";

                // Drop the temp index if it exists
                Action actionDropIndexIfExistsTmp = new Action();
                actionDropIndexIfExistsTmp.addLock(LockLevel.ACCESS_EXCLUSIVE,
                        fqnTableName);
                actionDropIndexIfExistsTmp
                        .addStatement("DROP INDEX IF EXISTS " + fqnTmpName);
                actionListForReindexJob.add(actionDropIndexIfExistsTmp);

                /*
                 * The index may be backing a primary key or unique constraint.
                 * Recreate the constraint if that's the case.
                 */
                query = sess.createSQLQuery(SELECT_CONSTRAINT_INFO)
                        .addScalar("oid", IntegerType.INSTANCE)
                        .addScalar("contype", StringType.INSTANCE);
                addParamsToQuery(query, paramMap);
                queryResult = query.scroll();

                boolean isConstraint = queryResult.next();
                String indexTypeForConstraint = null;
                if (isConstraint) {
                    String constraintType = queryResult.getString(1);
                    if ("p".equals(constraintType)) {
                        indexTypeForConstraint = "PRIMARY KEY";
                    } else if ("u".equals(constraintType)) {
                        indexTypeForConstraint = "UNIQUE";
                    } else {
                        logger.warn(String.format(
                                "Can not recreate index %s for constraint type '%s'",
                                indexName, constraintType));
                        return 0;
                    }
                }

                /*
                 * Recreate foreign constraints that depend on this index.
                 */
                query = sess.createSQLQuery(SELECT_INDEX_FCONS
                        + (isConstraint ? SELECT_INDEX_FCONS_EXCLUSION : ""))
                        .addScalar("fconname", StringType.INSTANCE)
                        .addScalar("fconrelname", StringType.INSTANCE)
                        .addScalar("fconns", StringType.INSTANCE)
                        .addScalar("fcontype", StringType.INSTANCE)
                        .addScalar("fcondef", StringType.INSTANCE);

                paramMap.clear();
                paramMap.put("conindid", indexId);
                if (isConstraint) {
                    Integer conId = queryResult.getInteger(0);
                    paramMap.put("conid", conId);
                }
                addParamsToQuery(query, paramMap);
                queryResult = query.scroll();

                Action fkAlterAction = new Action();
                List<Action> fkValidActions = new ArrayList<>();

                /*
                 * Use temporary lists to track adds and drops so they can be
                 * performed in the correct order.
                 */
                List<String> dropStatements = new ArrayList<>();
                List<String> addStatements = new ArrayList<>();
                while (queryResult.next()) {
                    String fconType = queryResult.getString(3);
                    String fkConName = queryResult.getString(0);
                    if ("f".equals(fconType)) {
                        String fqnFkTable = String.format("\"%s\".\"%s\"",
                                queryResult.getString(2),
                                queryResult.getString(1));
                        fkAlterAction.addLock(LockLevel.ACCESS_EXCLUSIVE,
                                fqnFkTable);
                        dropStatements.add(String.format(
                                "ALTER TABLE %s DROP CONSTRAINT %s;",
                                fqnFkTable, fkConName));
                        addStatements.add(String.format(
                                "ALTER TABLE %s ADD CONSTRAINT %s %s NOT VALID;",
                                fqnFkTable, fkConName,
                                queryResult.getString(4)));

                        Action fkValidAction = new Action();
                        fkValidAction.addLock(LockLevel.SHARE_UPDATE_EXCLUSIVE,
                                fqnFkTable);
                        fkValidAction.addLock(LockLevel.ROW_SHARE,
                                fqnTableName);
                        fkValidAction.addStatement(String.format(
                                "ALTER TABLE %s VALIDATE CONSTRAINT %s;",
                                fqnFkTable, fkConName));
                        fkValidActions.add(fkValidAction);
                    } else {
                        logger.warn(String.format(
                                "Constraint %s appears to depend on %s, but do not know how to handle it. "
                                        + "Manually rebuilding the index is recommended",
                                fkConName, indexName));
                        return 0;
                    }
                }

                /*
                 * Drop all the foreign keys before reindexing
                 */
                for (String drop : dropStatements) {
                    fkAlterAction.addStatement(drop);
                }
                /*
                 * Recreate the index either by recreating the constraint(s) or
                 * by renaming the temporary index.
                 */
                fkAlterAction.addLock(LockLevel.ACCESS_EXCLUSIVE, fqnTableName);
                if (isConstraint) {
                    fkAlterAction.addStatement("ALTER TABLE " + fqnTableName
                            + " DROP CONSTRAINT \"" + indexName
                            + "\", ADD CONSTRAINT \"" + indexName + "\" "
                            + indexTypeForConstraint + " USING INDEX \""
                            + tmpName + "\";");
                } else {
                    fkAlterAction.addStatement(
                            "DROP INDEX IF EXISTS " + fqnIndexName + ";");
                    fkAlterAction.addStatement("ALTER INDEX " + fqnTmpName
                            + " RENAME TO \"" + indexName + "\";");
                }
                /*
                 * Add all the foreign keys back
                 */
                for (String add : addStatements) {
                    fkAlterAction.addStatement(add);
                }
                /*
                 * Create temp index concurrently, cannot happen in a
                 * transaction block
                 */
                Action actionCreateTempIndex = new Action();
                actionCreateTempIndex.addStatement(
                        "COMMIT; " + matcher.group(1) + " INDEX CONCURRENTLY \""
                                + tmpName + "\" " + matcher.group(2));
                actionListForReindexJob.add(actionCreateTempIndex);

                actionListForReindexJob.add(fkAlterAction);
                for (Action validateAction : fkValidActions) {
                    actionListForReindexJob.add(validateAction);
                }

                /*
                 * Create the ReindexJob object and insert it into the
                 * DelayQueue
                 */
                try {
                    ReindexJob reindexJob = new ReindexJob(indexName, delay,
                            actionListForReindexJob, myDao);
                    workerThread.queueJob(reindexJob);
                } catch (Exception e) {
                    logger.error(
                            "Error when adding ReindexJob object to DelayQueue: ",
                            e);
                }

                return null;
            }
        });
    }

    /*
     * Represents a single Reindexing job that is saved to a DelayQueue
     */
    private class ReindexJob implements Delayed {

        protected final IUFStatusHandler logger = UFStatus
                .getHandler(getClass());

        private final String indexName;

        private List<Action> actionList = new ArrayList<>();

        private final long startTime;

        private long expireTime;

        private final long delay;

        private final PostgresBloatDao myDao;

        public ReindexJob(String indexName, long delay, List<Action> actionList,
                PostgresBloatDao myDao) {
            this.indexName = indexName;
            this.delay = delay;
            this.startTime = System.currentTimeMillis();
            this.expireTime = this.startTime;
            this.actionList = actionList;
            this.myDao = myDao;
        }

        /*
         * Executes list of actions
         */
        public int executeActions() {

            int status = 0;
            ListIterator<Action> itr = actionList.listIterator();
            Action nextAction = null;

            int actionLength = actionList.size();
            int actionCount = 0;
            long start = System.currentTimeMillis();
            logger.info("Processing actions for reindexing of " + indexName
                    + "...");
            // Iterate through the List of Actions
            while (itr.hasNext() && status == 0) {
                actionCount++;

                // Execute the Action
                nextAction = itr.next();
                logger.info("Processing action " + actionCount + " of "
                        + actionLength + "...");
                status = processAction(nextAction);

                /*
                 * Remove the Action from the List if it was executed
                 * successfully
                 */
                if (status == 0) {
                    itr.remove();
                }
            }
            long finish = System.currentTimeMillis();
            logger.info("Processed " + actionCount + " actions from for "
                    + indexName + "in " + (finish - start) + "ms.");
            return status;
        }

        /*
         * Processes each Action object
         */
        public int processAction(Action action) {

            int status = 0;
            final ActionStatus actionStatus = new ActionStatus();

            try {
                myDao.txTemplate.execute(new TransactionCallback<Integer>() {

                    @Override
                    public Integer doInTransaction(TransactionStatus status) {

                        Session sess = getCurrentSession();

                        // Obtain table locks
                        SortedMap<LockLevel, Set<String>> tableLocks = action
                                .getLocks();
                        if (!tableLocks.isEmpty()) {
                            actionStatus.setLockFailure(true);
                            for (LockLevel lockLevel : tableLocks.keySet()) {
                                for (String table : tableLocks.get(lockLevel)) {
                                    String lockStatement = "LOCK TABLE " + table
                                            + " IN " + lockLevel.level
                                            + " MODE NOWAIT;";
                                    actionStatus.setLastTask(lockStatement);

                                    logger.info("Executing statement: "
                                            + lockStatement);
                                    SQLQuery queryLockTables = sess
                                            .createSQLQuery(lockStatement);
                                    queryLockTables.executeUpdate();
                                }
                            }
                        }

                        // Execute all the statements
                        actionStatus.setLockFailure(false);
                        for (String statement : action.getStatementList()) {
                            actionStatus.setLastTask(statement);

                            logger.info("Executing statement: " + statement);
                            SQLQuery queryStatement = sess
                                    .createSQLQuery(statement);
                            queryStatement.executeUpdate();
                        }

                        return 0;
                    }
                });
            } catch (Exception e) {

                if (actionStatus.isLockFailure()) {
                    status = 1;
                    logger.info("Failed to execute action during command '"
                            + actionStatus.getLastTask() + "'. ReindexJob for "
                            + indexName + " will be delayed and reattempted.");
                } else {
                    status = 2;
                    logger.error("Failed to execute action during command '"
                            + actionStatus.getLastTask() + "'.", e);
                }
            }
            return status;
        }

        @Override
        public long getDelay(TimeUnit unit) {

            long diff = expireTime - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {

            if (this.getDelay(TimeUnit.MILLISECONDS) < o
                    .getDelay(TimeUnit.MILLISECONDS)) {
                return -1;
            }
            if (this.getDelay(TimeUnit.MILLISECONDS) > o
                    .getDelay(TimeUnit.MILLISECONDS)) {
                return 1;
            }
            return 0;
        }

        @Override
        public String toString() {

            String formatedExpireTime = formatDate(expireTime);

            return "ReindexJob [indexName=" + indexName + ", expireTime="
                    + formatedExpireTime + ", actionList=" + actionList + "]";
        }

        /*
         * Utility method for formatting date and time
         */
        String formatDate(long dateInMillis) {
            Date date = new Date(dateInMillis);
            return DateFormat.getDateTimeInstance(DateFormat.LONG,
                    DateFormat.LONG, Locale.getDefault()).format(date);
        }

        /*
         * Getter and Setter methods
         */
        public String getIndexName() {
            return indexName;
        }

        public void updateExpireTime() {
            /*
             * expireTime is currentTime+delay, so if delay of 60 seconds is
             * required expiration from queue will happen after currenttime + 60
             * seconds
             */
            this.expireTime = System.currentTimeMillis() + delay;
        }
    }

    public enum LockLevel {
        ACCESS_EXCLUSIVE("ACCESS EXCLUSIVE"),
        EXCLUSIVE("EXCLUSIVE"),
        SHARE_ROW_EXCLUSIVE("SHARE ROW EXCLUSIVE"),
        SHARE("SHARE"),
        SHARE_UPDATE_EXCLUSIVE("SHARE UPDATE EXCLUSIVE"),
        ROW_EXCLUSIVE("ROW EXCLUSIVE"),
        ROW_SHARE("ROW SHARE"),
        ACCESS_SHARE("ACCESS SHARE");

        private final String level;

        LockLevel(String level) {
            this.level = level;
        }
    }

    /*
     * Wrapper for string containing the last command executed in an action.
     * Used to track why reindex jobs get delayed.
     */
    private class ActionStatus {
        private String lastTask;

        private boolean lockFailure;

        public String getLastTask() {
            return lastTask;
        }

        public void setLastTask(String lastTask) {
            this.lastTask = lastTask;
        }

        public boolean isLockFailure() {
            return lockFailure;
        }

        public void setLockFailure(boolean lockFailure) {
            this.lockFailure = lockFailure;
        }
    }

    /*
     * Contains the SQL Query to be executed, the List of tables to be locked
     * for each Action, the table lock mode and a unique ActionId to identify
     * each Action object
     */
    private class Action {

        private final SortedMap<LockLevel, Set<String>> locks = new TreeMap<>();

        private final List<String> statementList = new ArrayList<>();

        // private final String actionId; // Used for logging only

        public Action() {
        }

        public void addLock(LockLevel lock, String table) {
            Set<String> tableList = locks.get(lock);
            if (tableList == null) {
                tableList = new HashSet<>();
                locks.put(lock, tableList);
            }
            tableList.add(table);
        }

        public void addStatement(String statement) {
            statementList.add(statement);
        }

        public SortedMap<LockLevel, Set<String>> getLocks() {
            return locks;
        }

        public List<String> getStatementList() {
            return statementList;
        }
    }

    /*
     * This class represents a single worker thread which waits to process each
     * ReindexJob object placed on the DelayQueue
     */
    private static class ReindexThread extends Thread {

        /** Singleton instance of this class */
        private static ReindexThread instance = null;

        private final BlockingQueue<ReindexJob> delayQueue;

        private final Set<String> queuedJobs;

        protected final IUFStatusHandler loggerForWorker = UFStatus
                .getHandler(getClass());

        private ReindexThread() {
            setName("ReindexThread");
            this.delayQueue = new DelayQueue<>();
            queuedJobs = Collections.synchronizedSet(new HashSet<>());
        }

        public static ReindexThread getInstance() {
            if (instance == null) {
                instance = new ReindexThread();
            }
            return instance;
        }

        public void queueJob(ReindexJob reindexJob)
                throws InterruptedException {
            if (!queuedJobs.contains(reindexJob.getIndexName())) {
                delayQueue.put(reindexJob);
                queuedJobs.add(reindexJob.getIndexName());
            }
        }

        @Override
        public void run() {

            ReindexJob reindexJob = null;
            int status = 0;

            while (!EDEXUtil.isShuttingDown()) {
                try {
                    /*
                     * Wait and then process a job from the DelayQueue once a
                     * job has been placed in the queue
                     */
                    loggerForWorker.info(
                            "Waiting to retrieve a job from DelayQueue...");
                    reindexJob = delayQueue.take();
                    status = reindexJob.executeActions();
                    String indexName = reindexJob.getIndexName();

                    /*
                     * Status of executing actions could be success (0), lock
                     * failure (1), or non-lock failure (2). On a lock-failure,
                     * put the job back in the queue to be retried later. If it
                     * fails for any reason other than unable to retrain a lock,
                     * then log an error and remove the job from the queue.
                     */
                    if (status == 1) {
                        reindexJob.updateExpireTime();
                        delayQueue.put(reindexJob);
                    } else if (status == 2) {
                        queuedJobs.remove(indexName);
                        loggerForWorker
                                .error("Error occured in processing ReindexJob: "
                                        + reindexJob);
                    } else {
                        queuedJobs.remove(indexName);
                        loggerForWorker
                                .info("Reindex of " + indexName + " completed "
                                        + (System.currentTimeMillis()
                                                - reindexJob.startTime)
                                        + "ms after job queued.");
                    }
                } catch (Exception e) {
                    loggerForWorker.handle(Priority.INFO,
                            "Error occured in processing ReindexJob: "
                                    + reindexJob,
                            e);
                }
            }
        }
    }
}
