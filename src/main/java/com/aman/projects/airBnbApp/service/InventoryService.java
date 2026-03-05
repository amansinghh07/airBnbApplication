package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.HotelDto;
import com.aman.projects.airBnbApp.dto.HotelPriceDto;
import com.aman.projects.airBnbApp.dto.HotelSearchRequestDto;
import com.aman.projects.airBnbApp.entity.Room;
import org.springframework.data.domain.Page;

public interface InventoryService {
    void intializeRoomForAYear(Room room);
    void deleteFutureInventories(Room room);

    Page<HotelPriceDto> searchHotels(HotelSearchRequestDto hotelSearchRequestDto);
}
