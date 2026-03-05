package com.aman.projects.airBnbApp.dto;

import com.aman.projects.airBnbApp.entity.Hotel;
import com.aman.projects.airBnbApp.entity.Room;
import com.aman.projects.airBnbApp.entity.User;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequest {
    private Long hotelId;
    private Long roomId;
    private Integer roomsCount;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}
