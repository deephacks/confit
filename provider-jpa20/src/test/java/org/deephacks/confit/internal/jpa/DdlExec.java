package org.deephacks.confit.internal.jpa;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DdlExec {

    public static void execute(List<String> commands, String url, String username, String password,
            boolean ignoreSqlEx) throws SQLException, IOException {
        Connection c = getConnection(url, username, password);
        execute(commands, c, ignoreSqlEx);
    }

    public static void execute(File file, String url, String username, String password,
            boolean ignoreSqlEx) throws SQLException, IOException {
        Connection c = getConnection(url, username, password);
        execute(Files.readLines(file, Charset.defaultCharset()), c, ignoreSqlEx);
    }

    private static Connection getConnection(String url, String username, String password)
            throws SQLException {
        Properties connectionProps = new Properties();
        connectionProps.put("user", username);
        connectionProps.put("password", password);
        Connection conn = DriverManager.getConnection(url, connectionProps);
        conn.setAutoCommit(true);
        return conn;
    }

    private static void execute(List<String> lines, Connection c, boolean ignoreSqlEx)
            throws SQLException, IOException {
        try {
            List<String> sqlStmts = new ArrayList<String>();
            StringBuilder sb = new StringBuilder();
            for (String input : lines) {
                if (input == null || "".equals(input.trim()) || input.startsWith("--")) {
                    continue;
                }
                sb.append(input);
                if (input.endsWith(";")) {
                    sqlStmts.add(sb.substring(0, sb.length() - 1));
                    sb = new StringBuilder();
                }
            }
            for (String sql : sqlStmts) {
                PreparedStatement stmt = c.prepareStatement(sql);
                stmt.execute();
            }

        } catch (SQLException e) {
            if (!ignoreSqlEx) {
                throw e;
            }
        } finally {
            if (c != null) {
                try {
                    if(!c.getAutoCommit()) {
                        c.commit();
                    }
                    c.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}