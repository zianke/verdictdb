package org.verdictdb.core.execution;

import java.util.ArrayList;
import java.util.List;

import org.verdictdb.DbmsConnection;
import org.verdictdb.core.execution.ola.HyperTableCube;
import org.verdictdb.core.query.SelectQuery;
import org.verdictdb.exception.VerdictDBException;
import org.verdictdb.exception.VerdictDBValueException;

public class AggExecutionNode extends CreateTableAsSelectExecutionNode {

  List<HyperTableCube> cubes = new ArrayList<>();

  protected AggExecutionNode(QueryExecutionPlan plan) {
    super(plan);
  }
  
  public static AggExecutionNode create(QueryExecutionPlan plan, SelectQuery query) throws VerdictDBValueException {
    AggExecutionNode node = new AggExecutionNode(plan);
    SubqueriesToDependentNodes.convertSubqueriesToDependentNodes(query, node);
    node.setSelectQuery(query);
    
    return node;
  }
  
  public SelectQuery getSelectQuery() {
    return selectQuery;
  }

  @Override
  public ExecutionInfoToken executeNode(DbmsConnection conn, List<ExecutionInfoToken> downstreamResults) 
      throws VerdictDBException {
    ExecutionInfoToken result = super.executeNode(conn, downstreamResults);
    if (!cubes.isEmpty()) {
      result.setKeyValue("hyperTableCube", cubes);
    }
    return result;
  }

  @Override
  public QueryExecutionNode deepcopy() {
    AggExecutionNode node = new AggExecutionNode(plan);
    copyFields(this, node);
    return node;
  }

  public List<HyperTableCube> getCubes() {
    return cubes;
  }
}
