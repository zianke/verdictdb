package org.verdictdb.core.querying;

import org.verdictdb.core.sqlobject.SelectQuery;
import org.verdictdb.exception.VerdictDBException;

public class QueryExecutionPlanFactory {
  
  /**
   * Creates a node tree and return it as an instance of QueryExecutionPlan.
   * @param query
   * @return
   */
  public static QueryExecutionPlan create(SelectQuery query, IdCreator idCreator) throws VerdictDBException {
    ExecutableNodeBase root = SelectAllExecutionNode.create(idCreator, query);
    return null;
  }
  
  static ExecutableNodeBase createRootAndItsDependents(SelectQuery query) {
    
    // identify the query type and calls an appropriate function defined below.
    
    return null;
  }
  
  static SelectAllExecutionNode createSelectAllExecutionNodeAndItsDependents(SelectQuery query) {
    // move an existing static create() factory method here.
    return null;
  }
  
  // create more functions like this.

}
