package org.verdictdb.core.execution;

import java.util.ArrayList;
import java.util.List;

import org.verdictdb.core.query.BaseTable;
import org.verdictdb.core.query.SelectQuery;

public class QueryExecutionNodeWithPlaceHolders extends QueryExecutionNode {
  
  List<BaseTable> placeholderTables = new ArrayList<>();

  public QueryExecutionNodeWithPlaceHolders(SelectQuery query) {
    super(query);
  }
  
  BaseTable createPlaceHolderTable(String aliasName) {
    BaseTable table = new BaseTable("placeholderSchemaName", "placeholderTableName", aliasName);
    placeholderTables.add(table);
    return table;
  }

  @Override
  public ExecutionResult executeNode(List<ExecutionResult> downstreamResults) {
    for (int i = 0; i < placeholderTables.size(); i++) {
      BaseTable t = placeholderTables.get(i);
      ExecutionResult r = downstreamResults.get(i);
      String schemaName = (String) r.getValue("schemaName");
      String tableName = (String) r.getValue("tableName");
      t.setSchemaName(schemaName);
      t.setTableName(tableName);
    }
    return null;
  }

}
