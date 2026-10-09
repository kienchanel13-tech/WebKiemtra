package com.busbooking.test;

import com.busbooking.dao.ReservationDAO;
import com.busbooking.service.ReservationService;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import java.util.Map;

public class ReservationTest {

    public static void main(String[] args) {

        // Cấu hình DataSource thủ công cho file Test (Vì chạy ngoài Spring Boot)
        SQLServerDataSource ds = new SQLServerDataSource();
        ds.setURL("jdbc:sqlserver://MSI\\MSSQLSERVER;databaseName=QuanLyVeXeKhach;instanceName=MSSQLSERVER;encrypt=true;trustServerCertificate=true;");
        ds.setUser("busapp");
        ds.setPassword("10101010aA@");

        ReservationDAO reservationDAO = new ReservationDAO(ds);
        ReservationService reservationService =
                new ReservationService(reservationDAO);

        // THAY CAC ID BANG ID THUC TE TRONG DATABASE
        int customerId = 1;
        int tripId = 1;
        int seatId = 1;
        int ticketTypeId = 1;

        try {

            // =====================================================
            // 1. XEM GHE CON TRONG
            // =====================================================

            System.out.println("=== GHE CON TRONG ===");

            reservationService
                    .getAvailableSeats(tripId)
                    .forEach(System.out::println);


            // =====================================================
            // 2. GIU GHE TRONG 15 PHUT
            // =====================================================

            System.out.println("=== GIU GHE ===");

            Map<String, Object> held =
                    reservationService.holdSeat(
                            customerId,
                            null,       // AppointmentId
                            null,       // ReservationId moi
                            tripId,
                            seatId,
                            ticketTypeId,
                            "Nguyen Van A",
                            "0912345678",
                            15
                    );

            held.forEach((key, value) ->
                    System.out.println(key + " = " + value)
            );

            System.out.println(
                    "Giu ghe thanh cong. Hay luu ReservationId."
            );

        } catch (Exception e) {

            System.err.println("NGHIEP VU THAT BAI:");
            System.err.println(e.getMessage());

            e.printStackTrace();
        }
    }
}