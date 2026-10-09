package com.busbooking.dao;

import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Repository
public class ReservationDAO {

    private final DataSource dataSource;

    public ReservationDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Chuyen ResultSet thanh danh sach Map.
     */
    private List<Map<String, Object>> readRows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    private void setNullableInt(CallableStatement cs, int index, Integer value) throws SQLException {
        if (value == null) {
            cs.setNull(index, Types.INTEGER);
        } else {
            cs.setInt(index, value);
        }
    }

    /**
     * Lay danh sach ghe con trong cua mot chuyen xe.
     */
    public List<Map<String, Object>> getAvailableSeats(int tripId) throws SQLException {
        String sql = "{call sp_GetAvailableSeats(?)}";

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, tripId);
            boolean hasResult = cs.execute();
            while (true) {
                if (hasResult) {
                    try (ResultSet rs = cs.getResultSet()) {
                        if (rs != null) {
                            return readRows(rs);
                        }
                    }
                } else if (cs.getUpdateCount() == -1) {
                    break;
                }
                hasResult = cs.getMoreResults();
            }
            return Collections.emptyList();
        }
    }

    public Map<String, Object> holdSeat(
            int customerId,
            Integer appointmentId,
            Integer reservationId,
            int tripId,
            int seatId,
            int ticketTypeId,
            String passengerName,
            String passengerPhone,
            int holdMinutes
    ) throws SQLException {
        String sql = "{call sp_HoldSeat(?, ?, ?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, customerId);
            setNullableInt(cs, 2, appointmentId);
            setNullableInt(cs, 3, reservationId);
            cs.setInt(4, tripId);
            cs.setInt(5, seatId);
            cs.setInt(6, ticketTypeId);
            cs.setNString(7, passengerName);

            if (passengerPhone == null || passengerPhone.isBlank()) {
                cs.setNull(8, Types.VARCHAR);
            } else {
                cs.setString(8, passengerPhone);
            }

            cs.setInt(9, holdMinutes);

            boolean hasResult = cs.execute();
            while (true) {
                if (hasResult) {
                    try (ResultSet rs = cs.getResultSet()) {
                        if (rs != null && rs.next()) {
                            Map<String, Object> result = new LinkedHashMap<>();
                            ResultSetMetaData meta = rs.getMetaData();
                            for (int i = 1; i <= meta.getColumnCount(); i++) {
                                result.put(meta.getColumnLabel(i), rs.getObject(i));
                            }
                            return result;
                        }
                    }
                } else if (cs.getUpdateCount() == -1) {
                    break;
                }
                hasResult = cs.getMoreResults();
            }

            throw new SQLException("sp_HoldSeat khong tra ve thong tin giu ghe.");
        }
    }

    /**
     * Xac nhan reservation va tao booking/ticket/payment
     */
    public List<Map<String, Object>> confirmReservation(
            int reservationId,
            String bookingType,
            String paymentMethod,
            String transactionCode
    ) throws SQLException {
        String sql = "{call sp_ConfirmReservation(?, ?, ?, ?)}";

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, reservationId);
            cs.setString(2, bookingType);
            cs.setString(3, paymentMethod);

            if (transactionCode == null || transactionCode.isBlank()) {
                cs.setNull(4, Types.VARCHAR);
            } else {
                cs.setString(4, transactionCode);
            }

            boolean hasResult = cs.execute();
            while (true) {
                if (hasResult) {
                    try (ResultSet rs = cs.getResultSet()) {
                        if (rs != null) {
                            return readRows(rs);
                        }
                    }
                } else if (cs.getUpdateCount() == -1) {
                    break;
                }
                hasResult = cs.getMoreResults();
            }

            return Collections.emptyList();
        }
    }
}