package com.busbooking.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GenericDao: CRUD chung cho tat ca cac bang.
 * Su dung DataSource duoc Spring inject (HikariCP pool).
 */
@Component
public class GenericDao {

    private static final Set<String> ALLOWED_TABLES = Set.of(
            "Customers", "Employees", "VehicleTypes", "Vehicles",
            "Seats", "BusRoutes", "Trips", "TicketTypes", "TripFares",
            "Appointments", "Reservations", "ReservationDetails",
            "Bookings", "Tickets", "Payments", "TicketCancellations"
    );

    // Cache metadata de tranh query DB nhieu lan
    private static final Map<String, List<String>> columnsCache = new ConcurrentHashMap<>();
    private static final Map<String, String>       primaryKeyCache = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>>  identityColumnsCache = new ConcurrentHashMap<>();

    // DataSource duoc Spring Boot inject tu application.properties
    private final DataSource dataSource;

    /**
     * Constructor duoc goi khi dung GenericDao voi Spring (tu dong inject DataSource).
     */
    @Autowired
    public GenericDao(DataSource dataSource) {
        this.dataSource = dataSource;
        this.table = null; // Khong dung table mac dinh khi inject boi Spring
    }

    /**
     * Constructor khi muon tao GenericDao voi ten bang cu the (dung ngoai Spring context).
     */
    public GenericDao(DataSource dataSource, String table) {
        if (table == null || !ALLOWED_TABLES.contains(table)) {
            throw new IllegalArgumentException("Bang khong duoc phep truy cap: " + table);
        }
        this.dataSource = dataSource;
        this.table = table;
    }

    // Ten bang dang lam viec
    private final String table;

    // Factory method tien loi de tao instance cho tung bang
    public GenericDao forTable(String tableName) {
        return new GenericDao(this.dataSource, tableName);
    }

    private String quote(String identifier) {
        return "[" + identifier.replace("]", "]]") + "]";
    }

    private String getTable() {
        if (table == null) throw new IllegalStateException("Chua chi dinh bang. Dung forTable(\"TenBang\").");
        return table;
    }

    private List<String> getColumns(Connection conn) throws SQLException {
        String t = getTable();
        if (columnsCache.containsKey(t)) return columnsCache.get(t);
        List<String> columns = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, t, null)) {
            while (rs.next()) columns.add(rs.getString("COLUMN_NAME"));
        }
        if (columns.isEmpty()) throw new SQLException("Khong tim thay cot cua bang " + t);
        columnsCache.put(t, columns);
        return columns;
    }

    private String getPrimaryKey(Connection conn) throws SQLException {
        String t = getTable();
        if (primaryKeyCache.containsKey(t)) return primaryKeyCache.get(t);
        try (ResultSet rs = conn.getMetaData().getPrimaryKeys(conn.getCatalog(), null, t)) {
            if (rs.next()) {
                String pk = rs.getString("COLUMN_NAME");
                primaryKeyCache.put(t, pk);
                return pk;
            }
        }
        throw new SQLException("Bang khong co khoa chinh: " + t);
    }

    private boolean isIdentity(Connection conn, String column) throws SQLException {
        String t = getTable();
        Set<String> identityCols = identityColumnsCache.computeIfAbsent(t, k -> new HashSet<>());
        if (identityCols.contains(column)) return true;
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, t, column)) {
            if (rs.next() && "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"))) {
                identityCols.add(column);
                return true;
            }
        }
        return false;
    }

    private void validateColumns(Connection conn, Map<String, ?> data) throws SQLException {
        Set<String> valid = new HashSet<>(getColumns(conn));
        for (String col : data.keySet()) {
            if (!valid.contains(col))
                throw new IllegalArgumentException("Cot khong ton tai trong " + getTable() + ": " + col);
        }
    }

    // ─── READ: Lay tat ca ban ghi ─────────────────────────────────────
    public List<Map<String, Object>> findAll() throws SQLException {
        String sql = "SELECT * FROM " + quote(getTable());
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= md.getColumnCount(); i++)
                    row.put(md.getColumnLabel(i), rs.getObject(i));
                result.add(row);
            }
        }
        return result;
    }

    // ─── READ: Tim theo khoa chinh ────────────────────────────────────
    public Optional<Map<String, Object>> findById(Object id) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String pk  = getPrimaryKey(conn);
            String sql = "SELECT * FROM " + quote(getTable()) + " WHERE " + quote(pk) + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setObject(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    ResultSetMetaData md = rs.getMetaData();
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++)
                        row.put(md.getColumnLabel(i), rs.getObject(i));
                    return Optional.of(row);
                }
            }
        }
    }

    // ─── CREATE: Them ban ghi ─────────────────────────────────────────
    public Object insert(Map<String, ?> data) throws SQLException {
        if (data == null || data.isEmpty())
            throw new IllegalArgumentException("Du lieu them khong duoc rong");
        try (Connection conn = dataSource.getConnection()) {
            validateColumns(conn, data);
            Map<String, Object> insertData = new LinkedHashMap<>();
            for (Map.Entry<String, ?> e : data.entrySet()) {
                if (isIdentity(conn, e.getKey())) {
                    if (e.getValue() != null)
                        throw new IllegalArgumentException("Khong duoc truyen gia tri IDENTITY: " + e.getKey());
                    continue;
                }
                insertData.put(e.getKey(), e.getValue());
            }
            if (insertData.isEmpty()) throw new IllegalArgumentException("Khong co cot hop le de INSERT");
            StringJoiner cols = new StringJoiner(", "), pls = new StringJoiner(", ");
            for (String c : insertData.keySet()) { cols.add(quote(c)); pls.add("?"); }
            String sql = "INSERT INTO " + quote(getTable()) + " (" + cols + ") VALUES (" + pls + ")";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int idx = 1;
                for (Object v : insertData.values()) ps.setObject(idx++, v);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getObject(1);
                }
            }
        }
        return null;
    }

    // ─── UPDATE: Cap nhat theo khoa chinh ─────────────────────────────
    public boolean update(Object id, Map<String, ?> data) throws SQLException {
        if (data == null || data.isEmpty())
            throw new IllegalArgumentException("Du lieu cap nhat khong duoc rong");
        try (Connection conn = dataSource.getConnection()) {
            validateColumns(conn, data);
            String pk = getPrimaryKey(conn);
            Map<String, Object> updateData = new LinkedHashMap<>();
            for (Map.Entry<String, ?> e : data.entrySet()) {
                if (e.getKey().equalsIgnoreCase(pk))
                    throw new IllegalArgumentException("Khong cap nhat khoa chinh: " + pk);
                if (isIdentity(conn, e.getKey()))
                    throw new IllegalArgumentException("Khong cap nhat cot IDENTITY: " + e.getKey());
                updateData.put(e.getKey(), e.getValue());
            }
            if (updateData.isEmpty()) throw new IllegalArgumentException("Khong co cot de cap nhat");
            StringJoiner asgn = new StringJoiner(", ");
            for (String c : updateData.keySet()) asgn.add(quote(c) + " = ?");
            String sql = "UPDATE " + quote(getTable()) + " SET " + asgn + " WHERE " + quote(pk) + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Object v : updateData.values()) ps.setObject(idx++, v);
                ps.setObject(idx, id);
                return ps.executeUpdate() == 1;
            }
        }
    }

    // ─── DELETE: Xoa theo khoa chinh ──────────────────────────────────
    public boolean delete(Object id) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String pk  = getPrimaryKey(conn);
            String sql = "DELETE FROM " + quote(getTable()) + " WHERE " + quote(pk) + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setObject(1, id);
                return ps.executeUpdate() == 1;
            }
        }
    }
}