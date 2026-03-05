package com.aman.projects.airBnbApp.dto;

import com.aman.projects.airBnbApp.entity.Booking;
import com.aman.projects.airBnbApp.entity.User;
import com.aman.projects.airBnbApp.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;
@Data
public class GuestDto {
    private Long id;
    private User user;
    private String name;
    private Gender gender;
    private Integer age;
//    private Set<Booking> bookings;
}
