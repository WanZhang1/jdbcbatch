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

import com.streamsets.pipeline.api.*;
import com.streamsets.pipeline.api.base.configurablestage.DExecutor;

@StageDef(
    version = 2,
    label = "Batch JDBC Query",
    description = "Executes queries against JDBC compliant database",
    upgrader = JdbcQueryExecutorUpgrader.class,
    icon = "rdbms-executor.png",
    onlineHelpRefUrl ="index.html?contextID=task_ym2_3cv_sx"
)
@ConfigGroups(value = Groups.class)
@GenerateResourceBundle
@HideConfigs(
  "config.hikariConfigBean.readOnly"
)
@PipelineLifecycleStage
public class JdbcQueryDExecutor extends DExecutor {

  @ConfigDefBean
  public JdbcQueryExecutorConfig config;

  @Override
  protected Executor createExecutor() {
    // The executor is meant to issue changing queries so having connection in read only mode doesn't make sense
    config.hikariConfigBean.readOnly = false;

    return new JdbcQueryExecutor(config);
  }

}
