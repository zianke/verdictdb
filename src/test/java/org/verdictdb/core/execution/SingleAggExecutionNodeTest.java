package org.verdictdb.core.execution;

import static org.junit.Assert.*;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.verdictdb.connection.DbmsConnection;
import org.verdictdb.connection.JdbcConnection;
import org.verdictdb.core.query.AliasedColumn;
import org.verdictdb.core.query.AsteriskColumn;
import org.verdictdb.core.query.BaseColumn;
import org.verdictdb.core.query.BaseTable;
import org.verdictdb.core.query.ColumnOp;
import org.verdictdb.core.query.SelectItem;
import org.verdictdb.core.query.SelectQuery;
import org.verdictdb.exception.VerdictDbException;
import org.verdictdb.sql.syntax.H2Syntax;

public class SingleAggExecutionNodeTest {

  static String originalSchema = "originalschema";

  static String originalTable = "originalschema";

  static String newSchema = "newschema";

  static String newTable  = "newtable";

  static int aggblockCount = 2;

  static DbmsConnection conn;

  @BeforeClass
  public static void setupDbConnAndScrambledTable() throws SQLException, VerdictDbException {
    final String DB_CONNECTION = "jdbc:h2:mem:singleggnodetest;DB_CLOSE_DELAY=-1";
    final String DB_USER = "";
    final String DB_PASSWORD = "";
    conn = new JdbcConnection(DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD), new H2Syntax());
    conn.executeUpdate(String.format("CREATE SCHEMA \"%s\"", originalSchema));
    conn.executeUpdate(String.format("CREATE SCHEMA \"%s\"", newSchema));
    populateData(conn, originalSchema, originalTable);
  }

  @Test
  public void testExecuteNode() {
    BaseTable base = new BaseTable(originalSchema, originalTable, "t");
    SelectQuery query = SelectQuery.create(
        Arrays.<SelectItem>asList(
            new AliasedColumn(
                ColumnOp.sum(new BaseColumn("t", "value")), "a")), 
        base);
    QueryExecutionNode root = new SingleAggExecutionNode(conn, null, newSchema, newTable, query);
    root.execute();
    conn.executeUpdate(String.format("DROP TABLE \"%s\".\"%s\"", newSchema, newTable));
  }

  static void populateData(DbmsConnection conn, String schemaName, String tableName) throws SQLException {
    conn.executeUpdate(String.format("CREATE TABLE \"%s\".\"%s\"(\"id\" int, \"value\" double)", schemaName, tableName));
    for (int i = 0; i < 2; i++) {
      conn.executeUpdate(String.format("INSERT INTO \"%s\".\"%s\"(\"id\", \"value\") VALUES(%s, %f)",
          schemaName, tableName, i, (double) i+1));
    }
  }

}
