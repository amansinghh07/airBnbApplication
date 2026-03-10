package com.aman.projects.airBnbApp.controller;

import com.aman.projects.airBnbApp.dto.BookingDto;
import com.aman.projects.airBnbApp.dto.BookingRequest;
import com.aman.projects.airBnbApp.dto.GuestDto;
import com.aman.projects.airBnbApp.entity.Booking;
import com.aman.projects.airBnbApp.service.BookingService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class HotelBookingController {
    private final BookingService bookingService;
    @PostMapping("/init")
    public ResponseEntity<BookingDto>intialiseBooking(@RequestBody BookingRequest bookingRequest){
        return ResponseEntity.ok(bookingService.intialiseBooking(bookingRequest));
    }
    @PostMapping("{bookingId}/addGuests")
    public ResponseEntity<BookingDto>addGuests(@PathVariable Long bookingId,
                                               @RequestBody List<GuestDto> guests){
        return ResponseEntity.ok(bookingService.addGuests(bookingId,guests));
    }
    @PostMapping("{bookingId}/payments")
    public ResponseEntity<Map<String,String>>intiatePayments(@PathVariable Long bookingId){
        String sessionUrl=bookingService.intiatePayments(bookingId);
        return ResponseEntity.ok(Map.of("sessionUrl",sessionUrl));
    }
    @PostMapping("{bookingId}/cancel")
    public ResponseEntity<Void>cancelBooking(@PathVariable Long bookingId){
      bookingService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }


}
