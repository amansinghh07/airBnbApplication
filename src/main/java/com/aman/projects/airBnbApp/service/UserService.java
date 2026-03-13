package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.BookingDto;
import com.aman.projects.airBnbApp.dto.ProfileUpdateRequestDto;
import com.aman.projects.airBnbApp.dto.UserDto;
import com.aman.projects.airBnbApp.entity.User;

import java.util.List;

public interface UserService {
    User getUserById(Long id);

    void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto);

    List<BookingDto> getMyBookings();

    UserDto getMyProfile();
}
