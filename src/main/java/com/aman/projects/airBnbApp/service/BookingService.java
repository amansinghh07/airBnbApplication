package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.BookingDto;
import com.aman.projects.airBnbApp.dto.BookingRequest;
import com.aman.projects.airBnbApp.dto.GuestDto;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface BookingService {
    public BookingDto intialiseBooking(BookingRequest bookingRequest);

    @Nullable BookingDto addGuests(Long bookingId, List<GuestDto> guests);
}
