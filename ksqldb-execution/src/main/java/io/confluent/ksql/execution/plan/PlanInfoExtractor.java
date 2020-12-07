/*
 * Copyright 2020 Confluent Inc.
 *
 * Licensed under the Confluent Community License (final the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.execution.plan;

/**
 * Visitor for extracting {@link PlanInfo} from an execution step plan,
 * to be passed to a {@link PlanBuilder} for use when translating the
 * execution step plan into a physical plan.
 *
 * <p>See {@link PlanInfo} description for more.
 */
public class PlanInfoExtractor {
  
  public <K> PlanInfo visitStreamFilter(final StreamFilter<K> streamFilter) {
    return visitSingleSourceStep(streamFilter);
  }

  public <K> PlanInfo visitStreamGroupBy(final StreamGroupBy<K> streamGroupBy) {
    return visitSingleSourceStep(streamGroupBy);
  }

  public PlanInfo visitStreamGroupByKey(final StreamGroupByKey streamGroupByKey) {
    return visitSingleSourceStep(streamGroupByKey);
  }

  public PlanInfo visitStreamAggregate(final StreamAggregate streamAggregate) {
    return visitSingleSourceStep(streamAggregate);
  }

  public <K> PlanInfo visitStreamSelect(final StreamSelect<K> streamSelect) {
    return visitSingleSourceStep(streamSelect);
  }

  public <K> PlanInfo visitFlatMap(final StreamFlatMap<K> streamFlatMap) {
    return visitSingleSourceStep(streamFlatMap);
  }

  public PlanInfo visitStreamSelectKey(final StreamSelectKeyV1 streamSelectKey) {
    return visitSingleSourceStep(streamSelectKey);
  }

  public <K> PlanInfo visitStreamSelectKey(final StreamSelectKey<K> streamSelectKey) {
    return visitSingleSourceStep(streamSelectKey);
  }

  public <K> PlanInfo visitStreamSink(final StreamSink<K> streamSink) {
    return visitSingleSourceStep(streamSink);
  }

  public PlanInfo visitStreamSource(final StreamSource streamSource) {
    return visitSourceStep(streamSource, false);
  }

  public PlanInfo visitWindowedStreamSource(final WindowedStreamSource windowedStreamSource) {
    return visitSourceStep(windowedStreamSource, false);
  }

  public <K> PlanInfo visitStreamStreamJoin(final StreamStreamJoin<K> streamStreamJoin) {
    final PlanInfo leftInfo = streamStreamJoin.getSources().get(0).extractPlanInfo(this);
    final PlanInfo rightInfo = streamStreamJoin.getSources().get(1).extractPlanInfo(this);
    return leftInfo.merge(rightInfo);
  }

  public <K> PlanInfo visitStreamTableJoin(final StreamTableJoin<K> streamTableJoin) {
    final PlanInfo leftInfo = streamTableJoin.getSources().get(0).extractPlanInfo(this);
    final PlanInfo rightInfo = streamTableJoin.getSources().get(1).extractPlanInfo(this);
    rightInfo.setMaterializeTableSource();
    return leftInfo.merge(rightInfo);
  }

  public PlanInfo visitTableSource(final TableSource tableSource) {
    return visitSourceStep(tableSource, true);
  }

  public PlanInfo visitWindowedTableSource(final WindowedTableSource windowedTableSource) {
    return visitSourceStep(windowedTableSource, true);
  }

  public PlanInfo visitStreamWindowedAggregate(
      final StreamWindowedAggregate streamWindowedAggregate
  ) {
    return visitSingleSourceStep(streamWindowedAggregate);
  }

  public PlanInfo visitTableAggregate(final TableAggregate tableAggregate) {
    final PlanInfo sourceInfo = visitSingleSourceStep(tableAggregate);
    sourceInfo.setMaterializeTableSource();
    return sourceInfo;
  }

  public <K> PlanInfo visitTableFilter(final TableFilter<K> tableFilter) {
    return visitSingleSourceStep(tableFilter);
  }

  public <K> PlanInfo visitTableGroupBy(final TableGroupBy<K> tableGroupBy) {
    return visitSingleSourceStep(tableGroupBy);
  }

  public <K> PlanInfo visitTableSelect(final TableSelect<K> tableSelect) {
    return visitSingleSourceStep(tableSelect);
  }

  public <K> PlanInfo visitTableSelectKey(final TableSelectKey<K> tableSelectKey) {
    final PlanInfo sourceInfo = visitSingleSourceStep(tableSelectKey);
    sourceInfo.setRepartitionTable();
    return sourceInfo;
  }

  public <K> PlanInfo visitTableSink(final TableSink<K> tableSink) {
    return visitSingleSourceStep(tableSink);
  }

  public <K> PlanInfo visitTableSuppress(final TableSuppress<K> tableSuppress) {
    return visitSingleSourceStep(tableSuppress);
  }

  public <K> PlanInfo visitTableTableJoin(final TableTableJoin<K> tableTableJoin) {
    final PlanInfo leftInfo = tableTableJoin.getSources().get(0).extractPlanInfo(this);
    final PlanInfo rightInfo = tableTableJoin.getSources().get(1).extractPlanInfo(this);
    leftInfo.setMaterializeTableSource();
    rightInfo.setMaterializeTableSource();
    return leftInfo.merge(rightInfo);
  }

  private PlanInfo visitSourceStep(final ExecutionStep<?> step, final boolean isTable) {
    return new PlanInfo(step, isTable);
  }

  private PlanInfo visitSingleSourceStep(final ExecutionStep<?> step) {
    return step.getSources().get(0).extractPlanInfo(this);
  }
}
