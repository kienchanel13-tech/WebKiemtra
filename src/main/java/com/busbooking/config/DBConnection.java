package com.busbooking.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection - Chi dung cho cac file Test thu cong (DatabaseInspector, DaoTest...).
 * Trong ung dung Spring Boot thuc, DataSource duoc quan ly boi HikariCP (application.properties).
 * KHONG su dung class nay trong Controller/Service/DAO cua Spring.
 */
public class DBConnection {

    private static final String URL =
            "jdbc:sqlserver://MSI\\MSSQLSERVER;"
                    + "databaseName=QuanLyVeXeKhach;"
                    + "instanceName=MSSQLSERVER;"
                    + "encrypt=true;"
                    + "trustServerCertificate=true;";

    private static final String USER = "busapp";
    private static final String PASSWORD = "10101010aA@";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        try (Connection connection = getConnection()) {
            System.out.println("KET NOI DATABASE THANH CONG!");
            System.out.println("Database: " + connection.getCatalog());
        } catch (SQLException e) {
            System.err.println("KET NOI DATABASE THAT BAI: " + e.getMessage());
        }
    }
}