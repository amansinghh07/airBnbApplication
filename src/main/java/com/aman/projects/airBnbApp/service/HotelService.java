package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.HotelDto;
import com.aman.projects.airBnbApp.dto.HotelInfoDto;
import com.aman.projects.airBnbApp.entity.Hotel;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface HotelService {
    HotelDto createNewHotel(HotelDto hotelDto);
    HotelDto getHotelById(Long id);
    HotelDto updateHotelById(Long id,HotelDto hotelDto);
    void deleteHotelById(Long id);
    void activateHotel(Long id);
    List<HotelDto> getAllHotels();

     HotelInfoDto getHotelInfo(Long hotelId);
}
