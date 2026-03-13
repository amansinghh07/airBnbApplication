package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.BookingDto;
import com.aman.projects.airBnbApp.dto.BookingReportDto;
import com.aman.projects.airBnbApp.dto.BookingRequest;
import com.aman.projects.airBnbApp.dto.GuestDto;
import com.stripe.model.Event;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    public BookingDto intialiseBooking(BookingRequest bookingRequest);

    @Nullable BookingDto addGuests(Long bookingId, List<GuestDto> guests);

    String intiatePayments(Long bookingId);

    void capturePayment(Event event);

    void cancelBooking(Long bookingId);

    List<BookingDto> getAllBookingByHotelId(Long hotelId);

    BookingReportDto getHotelReport(long hotelId, LocalDate startDate, LocalDate endDate);
}
