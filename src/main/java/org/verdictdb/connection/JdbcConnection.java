package org.verdictdb.connection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.verdictdb.coordinator.VerdictSingleResult;
import org.verdictdb.exception.VerdictDBDbmsException;
import org.verdictdb.jdbc41.VerdictResultSet;
import org.verdictdb.sqlsyntax.HiveSyntax;
import org.verdictdb.sqlsyntax.PostgresqlSyntax;
import org.verdictdb.sqlsyntax.SparkSyntax;
import org.verdictdb.sqlsyntax.SqlSyntax;
import org.verdictdb.sqlsyntax.SqlSyntaxList;

public class JdbcConnection implements DbmsConnection {
  
  Connection conn;

  SqlSyntax syntax;
  
  String currentSchema = null;
  
//  JdbcQueryResult jrs = null;
  
  public static JdbcConnection create(Connection conn) throws VerdictDBDbmsException {
    String connectionString = null;
    try {
      connectionString = conn.getMetaData().getURL();
    } catch (SQLException e) {
      throw new VerdictDBDbmsException(e);
    }
    
    String dbName = connectionString.split(":")[1];
    SqlSyntax syntax = SqlSyntaxList.getSyntaxFor(dbName);
    
    return new JdbcConnection(conn, syntax);
  }
  
  public JdbcConnection(Connection conn, SqlSyntax syntax) {
    this.conn = conn;
    try {
      this.currentSchema = conn.getCatalog();
    } catch (SQLException e) {
      e.printStackTrace();
      // leave currentSchema as null
    }
    this.syntax = syntax;
  }
  
  @Override
  public void close() {
    try {
      this.conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
  
  @Override
  public DbmsQueryResult execute(String sql) throws VerdictDBDbmsException {
//    System.out.println("About to issue this query: " + sql);
    try {
      Statement stmt = conn.createStatement();
      JdbcQueryResult jrs = null;
      boolean doesResultExist = stmt.execute(sql);
      if (doesResultExist) {
        ResultSet rs = stmt.getResultSet();
        jrs = new JdbcQueryResult(rs);
        rs.close();
      } else {
        jrs = null;
      }
      stmt.close();
      return jrs;
    } catch (SQLException e) {
      e.printStackTrace();
      throw new VerdictDBDbmsException(e.getMessage());
    }
  }

//  @Override
//  public DbmsQueryResult getResult() {
//    return jrs;
//  }
  
  public DbmsQueryResult executeQuery(String sql) throws VerdictDBDbmsException {
    return execute(sql);
  }

//  @Override
//  public DbmsQueryResult executeQuery(String query) throws VerdictDBDbmsException {
//    System.out.println("About to issue this query: " + query);
//    try {
//      Statement stmt = conn.createStatement();
//      ResultSet rs = stmt.executeQuery(query);
//      JdbcQueryResult jrs = new JdbcQueryResult(rs);
//      rs.close();
//      stmt.close();
//      return jrs;
//    } catch (SQLException e) {
//      throw new VerdictDBDbmsException(e.getMessage());
//    }
//  }
//
//  @Override
//  public int executeUpdate(String query) throws VerdictDBDbmsException {
//    System.out.println("About to issue this query: " + query);
//    try {
//      Statement stmt = conn.createStatement();
//      int r = stmt.executeUpdate(query);
//      stmt.close();
//      return r;
//    } catch (SQLException e) {
//      throw new VerdictDBDbmsException(e);
////      e.printStackTrace();
////      return 0;
//    }
//  }

  @Override
  public SqlSyntax getSyntax() {
    return syntax;
  }

  public Connection getConnection() {return conn;}

  @Override
  public List<String> getSchemas() throws VerdictDBDbmsException{
    List<String> schemas = new ArrayList<>();
    DbmsQueryResult queryResult = executeQuery(syntax.getSchemaCommand());
    VerdictSingleResult verdictResult = new VerdictSingleResult(queryResult);
    VerdictResultSet jdbcResultSet = new VerdictResultSet(verdictResult);
    try {
      while (jdbcResultSet.next()) {
        schemas.add(jdbcResultSet.getString(syntax.getSchemaNameColumnIndex()+1));
      }
    } catch (SQLException e) {
      throw new VerdictDBDbmsException(e);
    } finally {
      jdbcResultSet.close();
    }
    return schemas;
  }

  @Override
  public List<String> getTables(String schema) throws VerdictDBDbmsException {
    List<String> tables = new ArrayList<>();
    DbmsQueryResult queryResult = executeQuery(syntax.getTableCommand(schema));
    VerdictSingleResult verdictResult = new VerdictSingleResult(queryResult);
    VerdictResultSet jdbcQueryResult = new VerdictResultSet(verdictResult);
    try {
      while (jdbcQueryResult.next()) {
        tables.add(jdbcQueryResult.getString(syntax.getTableNameColumnIndex()+1));
      }
    } catch (SQLException e) {
      throw new VerdictDBDbmsException(e);
    } finally {
      jdbcQueryResult.close();
    }
    return tables;
  }

  @Override
  public List<Pair<String, String>> getColumns(String schema, String table) throws VerdictDBDbmsException {
    List<Pair<String, String>> columns = new ArrayList<>();
    DbmsQueryResult queryResult = executeQuery(syntax.getColumnsCommand(schema, table));
    VerdictSingleResult verdictResult = new VerdictSingleResult(queryResult);
    VerdictResultSet jdbcQueryResult = new VerdictResultSet(verdictResult);
    try {
      while (jdbcQueryResult.next()) {
        String type = jdbcQueryResult.getString(syntax.getColumnTypeColumnIndex()+1);
        type = type.toLowerCase();
        
//        // remove the size of type
//        type = type.replaceAll("\\(.*\\)", "");
        
        columns.add(
            new ImmutablePair<>(jdbcQueryResult.getString(syntax.getColumnNameColumnIndex()+1), type));
      }
    } catch (SQLException e) {
      throw new VerdictDBDbmsException(e);
    } finally {
      jdbcQueryResult.close();
    }
    return columns;
  }

  @Override
  public List<String> getPartitionColumns(String schema, String table) throws VerdictDBDbmsException {
    List<String> partition = new ArrayList<>();
    DbmsQueryResult queryResult = executeQuery(syntax.getPartitionCommand(schema, table));
    VerdictSingleResult verdictResult = new VerdictSingleResult(queryResult);
    VerdictResultSet jdbcQueryResult = new VerdictResultSet(verdictResult);
    
    try {
      // the result of postgresql is a vector of column index
      if (syntax instanceof PostgresqlSyntax) {
        jdbcQueryResult.next();
        Object o = jdbcQueryResult.getObject(1);
        String[] arr = o.toString().split(" ");
        List<Pair<String, String>> columns = getColumns(schema, table);
        for (int i=0; i<arr.length; i++) {
          partition.add(columns.get(Integer.valueOf(arr[i])-1).getKey());
        }
      }
      // Hive and Spark append partition information at the end of the "DESCRIBE TABLE" statement.
      else if (syntax instanceof HiveSyntax || syntax instanceof SparkSyntax) {
        boolean hasPartitionInfoStarted = false;
        while (jdbcQueryResult.next()) {
          String name = queryResult.getString(0);
          if (hasPartitionInfoStarted && (name.equalsIgnoreCase("# col_name") == false)) {
            partition.add(name);
          } else if (name.equalsIgnoreCase("# Partition Information")) {
            hasPartitionInfoStarted = true;
          }
        }
      }
      else {
        while (jdbcQueryResult.next()) {
          partition.add(jdbcQueryResult.getString(1));
        }
      }
    } catch (SQLException e) {
      throw new VerdictDBDbmsException(e);
    } finally {
      jdbcQueryResult.close();
    }
    jdbcQueryResult.close();
    return partition;
  }

  @Override
  public String getDefaultSchema() {
    return currentSchema;
  }

  @Override
  public void setDefaultSchema(String schema) {
    currentSchema = schema;
  }

  public DatabaseMetaData getMetadata() throws VerdictDBDbmsException {
    try {
      return conn.getMetaData();
    } catch (SQLException e) {
      throw new VerdictDBDbmsException(e);
    }
  }

}
