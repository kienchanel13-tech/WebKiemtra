package com.busbooking.service;

import com.busbooking.dao.ReservationDAO;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Service
public class ReservationService {

    private final ReservationDAO reservationDAO;

    public ReservationService(ReservationDAO reservationDAO) {
        this.reservationDAO = reservationDAO;
    }

    public List<Map<String, Object>> getAvailableSeats(int tripId)
            throws SQLException {

        if (tripId <= 0) {
            throw new IllegalArgumentException(
                    "TripId phai lon hon 0."
            );
        }

        return reservationDAO.getAvailableSeats(tripId);
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

        if (customerId <= 0 || tripId <= 0
                || seatId <= 0 || ticketTypeId <= 0) {
            throw new IllegalArgumentException(
                    "CustomerId, TripId, SeatId va TicketTypeId "
                            + "phai lon hon 0."
            );
        }

        if (appointmentId != null && appointmentId <= 0) {
            throw new IllegalArgumentException(
                    "AppointmentId khong hop le."
            );
        }

        if (reservationId != null && reservationId <= 0) {
            throw new IllegalArgumentException(
                    "ReservationId khong hop le."
            );
        }

        if (passengerName == null
                || passengerName.isBlank()) {
            throw new IllegalArgumentException(
                    "Ten hanh khach khong duoc de trong."
            );
        }

        if (holdMinutes < 1 || holdMinutes > 120) {
            throw new IllegalArgumentException(
                    "Thoi gian giu ghe phai tu 1 den 120 phut."
            );
        }

        return reservationDAO.holdSeat(
                customerId,
                appointmentId,
                reservationId,
                tripId,
                seatId,
                ticketTypeId,
                passengerName.trim(),
                passengerPhone,
                holdMinutes
        );
    }

    public List<Map<String, Object>> confirmReservation(
            int reservationId,
            String bookingType,
            String paymentMethod,
            String transactionCode
    ) throws SQLException {

        if (reservationId <= 0) {
            throw new IllegalArgumentException(
                    "ReservationId khong hop le."
            );
        }

        if (!"ONE_WAY".equals(bookingType)
                && !"ROUND_TRIP".equals(bookingType)) {
            throw new IllegalArgumentException(
                    "BookingType khong hop le."
            );
        }

        if (!List.of(
                "CASH", "BANK_TRANSFER", "EWALLET", "CARD"
        ).contains(paymentMethod)) {
            throw new IllegalArgumentException(
                    "Phuong thuc thanh toan khong hop le."
            );
        }

        if (!"CASH".equals(paymentMethod)
                && (transactionCode == null
                || transactionCode.isBlank())) {
            throw new IllegalArgumentException(
                    "Can ma giao dich cho phuong thuc thanh toan nay."
            );
        }

        return reservationDAO.confirmReservation(
                reservationId,
                bookingType,
                paymentMethod,
                transactionCode
        );
    }
}