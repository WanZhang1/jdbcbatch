/*
 * Copyright 2017 StreamSets Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embrace.stage.executor.jdbcbatch;

import com.streamsets.pipeline.api.Batch;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.base.BaseExecutor;
import com.streamsets.pipeline.api.base.OnRecordErrorException;
import com.streamsets.pipeline.api.el.ELEval;
import com.streamsets.pipeline.api.el.ELVars;
import com.streamsets.pipeline.lib.el.RecordEL;
import com.streamsets.pipeline.stage.common.DefaultErrorRecordHandler;
import com.streamsets.pipeline.stage.common.ErrorRecordHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class JdbcQueryExecutor extends BaseExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcQueryExecutor.class);

    private JdbcQueryExecutorConfig config;
    private ErrorRecordHandler errorRecordHandler;

    public JdbcQueryExecutor(JdbcQueryExecutorConfig config) {
        this.config = config;
    }

    @Override
    public List<ConfigIssue> init() {
        List<ConfigIssue> issues = super.init();
        config.init(getContext(), issues);
        errorRecordHandler = new DefaultErrorRecordHandler(getContext());
        return issues;
    }

    @Override
    public void write(Batch batch) throws StageException {
        ELVars variables = getContext().createELVars();
        ELEval eval = getContext().createELEval("query");

        try (Connection connection = config.getConnection()) {
            connection.setAutoCommit(false);//close auto commit

            Iterator<Record> it = batch.getRecords();
            Statement stmt = connection.createStatement();//one Statement for one batch

            try {
                while (it.hasNext()) {
                    Record record = it.next();
                    RecordEL.setRecordInContext(variables, record);
                    String query = eval.eval(variables, config.query, String.class);
                    LOG.debug("Executing query: {}", query);
                    stmt.addBatch(query); //change stmt.execute(query) to  stmt.addBatch(query)
                }
                int[] updateCounts= stmt.executeBatch(); //execute batch
                LOG.info("execute batch count:"+ updateCounts.length);
            } catch (SQLException ex) {
                LOG.error("Execute batch query failed", ex);
            } finally {
                try {
                    if (stmt != null)
                        stmt.close();
                } catch (SQLException se2) {
                }
            }
            if (config.batchCommit) {
                connection.commit();
            }
        } catch (SQLException ex) {
            LOG.error("Can't get connection", ex);
            throw new StageException(QueryExecErrors.QUERY_EXECUTOR_002, ex.getMessage());
        }
    }

    @Override
    public void destroy() {
        config.destroy();
        super.destroy();
    }
}
