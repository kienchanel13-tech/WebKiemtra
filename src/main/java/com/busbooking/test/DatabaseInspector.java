
package com.busbooking.test;

import com.busbooking.config.DBConnection;

import java.sql.*;
import java.util.*;

public class DatabaseInspector {

    // Giới hạn số dòng dữ liệu in ra cho mỗi bảng
    private static final int ROW_LIMIT = 10;

    public static void main(String[] args) {

        try (Connection conn = DBConnection.getConnection()) {

            DatabaseMetaData meta = conn.getMetaData();

            System.out.println("======================================");
            System.out.println(" KIEM TRA DATABASE SQL SERVER");
            System.out.println("======================================");
            System.out.println("Database : " + conn.getCatalog());
            System.out.println("SQL Server: " + meta.getDatabaseProductName());
            System.out.println("JDBC Driver: " + meta.getDriverVersion());
            System.out.println("Trang thai: DA KET NOI");
            System.out.println("======================================");

            List<String[]> tables = new ArrayList<>();

            // 1. Lay danh sach tat ca bang
            try (ResultSet rs = meta.getTables(
                    conn.getCatalog(), null, "%",
                    new String[]{"TABLE"})) {

                while (rs.next()) {
                    String schema = rs.getString("TABLE_SCHEM");
                    String table = rs.getString("TABLE_NAME");

                    // Bo qua bang he thong neu co
                    if (schema != null
                            && !schema.equalsIgnoreCase("sys")
                            && !schema.equalsIgnoreCase("INFORMATION_SCHEMA")) {
                        tables.add(new String[]{schema, table});
                    }
                }
            }

            tables.sort(Comparator.comparing(
                    t -> t[0] + "." + t[1],
                    String.CASE_INSENSITIVE_ORDER));

            if (tables.isEmpty()) {
                System.out.println("Khong tim thay bang nao!");
                return;
            }

            System.out.println("\nTONG SO BANG: " + tables.size());

            // 2. Kiem tra tung bang
            for (String[] tableInfo : tables) {
                String schema = tableInfo[0];
                String table = tableInfo[1];

                String fullTableName =
                        quoteIdentifier(schema) + "."
                                + quoteIdentifier(table);

                System.out.println("\n\n");
                System.out.println("======================================");
                System.out.println("BANG: " + schema + "." + table);
                System.out.println("======================================");

                printColumns(meta, conn.getCatalog(), schema, table);

                printRowCount(conn, fullTableName);

                printData(conn, fullTableName);
            }

            System.out.println("\n\n======================================");
            System.out.println("HOAN TAT KIEM TRA DATABASE");
            System.out.println("So bang da kiem tra: " + tables.size());
            System.out.println("======================================");

        } catch (SQLException e) {
            System.err.println("LOI KIEM TRA DATABASE!");
            System.err.println("Ma loi: " + e.getErrorCode());
            System.err.println("Chi tiet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Tao ten bang/cot an toan theo cu phap SQL Server
    private static String quoteIdentifier(String name) {
        return "[" + name.replace("]", "]]") + "]";
    }

    // 3. In ten cot, kieu du lieu, NULL, khoa chinh
    private static void printColumns(
            DatabaseMetaData meta,
            String catalog,
            String schema,
            String table) throws SQLException {

        Set<String> primaryKeys = new HashSet<>();

        try (ResultSet rs =
                     meta.getPrimaryKeys(catalog, schema, table)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }

        System.out.println("\n--- CAU TRUC COT ---");

        try (ResultSet rs =
                     meta.getColumns(catalog, schema, table, "%")) {

            System.out.printf(
                    "%-25s %-20s %-12s %-12s %-12s%n",
                    "TEN COT", "KIEU DU LIEU",
                    "DO DAI", "NULL?", "KHOA CHINH");

            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String typeName = rs.getString("TYPE_NAME");
                int size = rs.getInt("COLUMN_SIZE");
                String nullable = rs.getString("IS_NULLABLE");
                String pk = primaryKeys.contains(columnName)
                        ? "YES" : "-";

                System.out.printf(
                        "%-25s %-20s %-12d %-12s %-12s%n",
                        columnName, typeName, size,
                        nullable, pk);
            }
        }
    }

    // 4. Dem so ban ghi that trong bang
    private static void printRowCount(
            Connection conn,
            String fullTableName) {

        String sql = "SELECT COUNT_BIG(*) FROM " + fullTableName;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                System.out.println(
                        "\nTONG SO BAN GHI: " + rs.getLong(1));
            }

        } catch (SQLException e) {
            System.out.println(
                    "\nKhong dem duoc ban ghi: " + e.getMessage());
        }
    }

    // 5. Doc toi da 10 dong du lieu that
    private static void printData(
            Connection conn,
            String fullTableName) {

        String sql = "SELECT TOP (" + ROW_LIMIT + ") * FROM "
                + fullTableName;

        System.out.println("\n--- DU LIEU THAT (TOI DA "
                + ROW_LIMIT + " DONG) ---");

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData rsMeta = rs.getMetaData();
            int columnCount = rsMeta.getColumnCount();

            // In ten cot
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(rsMeta.getColumnLabel(i));

                if (i < columnCount) {
                    System.out.print(" | ");
                }
            }
            System.out.println();

            System.out.println("-".repeat(100));

            int rowCount = 0;

            while (rs.next()) {
                rowCount++;

                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);

                    String text;

                    if (value == null) {
                        text = "NULL";
                    } else if (value instanceof byte[]) {
                        text = "[BINARY DATA]";
                    } else {
                        text = value.toString();
                    }

                    // Khong de mot o du lieu qua dai
                    text = text.replace("\n", " ")
                            .replace("\r", " ");

                    if (text.length() > 80) {
                        text = text.substring(0, 77) + "...";
                    }

                    System.out.print(text);

                    if (i < columnCount) {
                        System.out.print(" | ");
                    }
                }

                System.out.println();
            }

            if (rowCount == 0) {
                System.out.println("(Bang chua co du lieu)");
            }

        } catch (SQLException e) {
            System.out.println(
                    "Khong doc duoc du lieu: " + e.getMessage());
        }
    }
}