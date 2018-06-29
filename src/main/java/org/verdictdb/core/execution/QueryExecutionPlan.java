/*
 * Copyright 2018 University of Michigan
 * 
 * You must contact Barzan Mozafari (mozafari@umich.edu) or Yongjoo Park (pyongjoo@umich.edu) to discuss
 * how you could use, modify, or distribute this code. By default, this code is not open-sourced and we do
 * not license this code.
 */

package org.verdictdb.core.execution;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;
import org.verdictdb.connection.DbmsConnection;
import org.verdictdb.core.query.SelectQuery;
import org.verdictdb.core.rewriter.ScrambleMeta;
import org.verdictdb.exception.VerdictDBException;
import org.verdictdb.exception.VerdictDBTypeException;
import org.verdictdb.exception.VerdictDBValueException;

public class QueryExecutionPlan {
  
//  SelectQuery query;
  
  ScrambleMeta scrambleMeta;
  
  QueryExecutionNode root;
  
  String scratchpadSchemaName;
  
  final int N_THREADS = 10;
  
//  PostProcessor postProcessor;
  
//  /**
//   * 
//   * @param queryString A select query
//   * @throws UnexpectedTypeException 
//   */
//  public AggQueryExecutionPlan(DbmsConnection conn, SyntaxAbstract syntax, String queryString) throws VerdictDbException {
//    this(conn, syntax, (SelectQueryOp) new NonValidatingSQLParser().toRelation(queryString));
//  }
  
  final int serialNum = ThreadLocalRandom.current().nextInt(0, 1000000);
  
  int identifierNum = 0;

  int tempTableNameNum = 0;
  
  public QueryExecutionPlan(String scratchpadSchemaName) {
    this.scratchpadSchemaName = scratchpadSchemaName;
    this.scrambleMeta = new ScrambleMeta();
  }
  
  public QueryExecutionPlan(String scratchpadSchemaName, ScrambleMeta scrambleMeta) {
    this.scratchpadSchemaName = scratchpadSchemaName;
    this.scrambleMeta = scrambleMeta;
  }
  
  public int getMaxNumberOfThreads() {
    return N_THREADS;
  }

  /**
   * 
   * @param query  A well-formed select query object
   * @throws VerdictDBValueException 
   * @throws VerdictDBException 
   */
  public QueryExecutionPlan(
      String scratchpadSchemaName,
      ScrambleMeta scrambleMeta,
      SelectQuery query) throws VerdictDBException {
    this(scratchpadSchemaName);
    setScrambleMeta(scrambleMeta);
    setSelectQuery(query);
  }
  
  public int getSerialNumber() {
    return serialNum;
  }
  
  public ScrambleMeta getScrambleMeta() {
    return scrambleMeta;
  }
  
  public void setScrambleMeta(ScrambleMeta scrambleMeta) {
    this.scrambleMeta = scrambleMeta;
  }
  
  public void setSelectQuery(SelectQuery query) throws VerdictDBException {
    if (!query.isAggregateQuery()) {
      throw new VerdictDBTypeException(query);
    }
    this.root = makePlan(query);
  }
  
  public String getScratchpadSchemaName() {
    return scratchpadSchemaName;
  }
  
  public QueryExecutionNode getRootNode() {
    return root;
  }
  
  public void setRootNode(QueryExecutionNode root) {
    this.root = root;
  }
  
  synchronized String generateUniqueIdentifier() {
    return String.format("%d_%d", serialNum, identifierNum++);
  }

  public String generateAliasName() {
    return String.format("verdictdbalias_%s", generateUniqueIdentifier());
  }

  public Pair<String, String> generateTempTableName() {
  //    return Pair.of(scratchpadSchemaName, String.format("verdictdbtemptable_%d", tempTableNameNum++));
      return Pair.of(scratchpadSchemaName, String.format("verdictdbtemptable_%s", generateUniqueIdentifier()));
    }

  /** 
   * Creates a tree in which each node is QueryExecutionNode. Each AggQueryExecutionNode corresponds to
   * an aggregate query, whether it is the main query or a subquery.
   * 
   * 1. Each QueryExecutionNode is supposed to run on a separate thread.
   * 2. Restrict the aggregate subqueries to appear in the where clause or in the from clause 
   *    (i.e., not in the select list, not in having or group-by)
   * 3. Each node cannot include any correlated predicate (i.e., the column that appears in outer queries).
   *   (1) In the future, we should convert a correlated subquery into a joined subquery (if possible).
   *   (2) Otherwise, the entire query including a correlated subquery must be the query of a single node.
   * 4. The results of AggNode and ProjectionNode are stored as a materialized view; the names of those
   *    materialized views are passed to their parents for potential additional processing or reporting.
   * 
   * @param conn
   * @param query
   * @return Pair of roots of the tree and post-processing interface.
   * @throws VerdictDBValueException 
   * @throws VerdictDBTypeException 
   */
  QueryExecutionNode makePlan(SelectQuery query) throws VerdictDBException {
    QueryExecutionNode root = SelectAllExecutionNode.create(this, query);
//    root = makeAsyncronousAggIfAvailable(root);
    return root;
  }
  
  public void execute(DbmsConnection conn, ExecutionTokenQueue queue) {
    // execute roots
    
    // after executions are all finished.
    cleanUp();
  }
  
  // clean up any intermediate materialized tables
  void cleanUp() {
    tempTableNameNum = 0;
  }
  
  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE)
        .append("root", root)
        .append("scrambleMeta", scrambleMeta)
        .toString();
  }

}
