package com.busbooking.controller;

import com.busbooking.dto.ConfirmReservationRequest;
import com.busbooking.dto.HoldSeatRequest;
import com.busbooking.service.ReservationService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "Reservation Controller hoạt động!";
    }

    @GetMapping("/seats/{tripId}")
    @ResponseBody
    public List<Map<String, Object>> getAvailableSeats(
            @PathVariable int tripId) throws SQLException {

        return reservationService.getAvailableSeats(tripId);
    }

    @PostMapping("/hold-seat")
    @ResponseBody
    public Map<String, Object> holdSeat(
            @jakarta.validation.Valid @RequestBody HoldSeatRequest request)
            throws SQLException {

        return reservationService.holdSeat(
                request.getCustomerId(),
                request.getAppointmentId(),
                request.getReservationId(),
                request.getTripId(),
                request.getSeatId(),
                request.getTicketTypeId(),
                request.getPassengerName(),
                request.getPassengerPhone(),
                request.getHoldMinutes()
        );
    }

    @PostMapping("/confirm")
    @ResponseBody
    public List<Map<String, Object>> confirmReservation(
            @RequestBody ConfirmReservationRequest request)
            throws SQLException {

        return reservationService.confirmReservation(
                request.getReservationId(),
                request.getBookingType(),
                request.getPaymentMethod(),
                request.getTransactionCode()
        );
    }
}