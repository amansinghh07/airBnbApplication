package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.RoomDto;

import java.util.List;

public interface RoomService {
   RoomDto createNewRoom(Long hotelId,RoomDto roomDto);
   List<RoomDto> getAllRoomsInHotel(Long hotelId);
   RoomDto getRoomById(Long roomId);
   void deleteRoomById(Long roomId);

    RoomDto updateRoomById(Long roomId, Long hotelId, RoomDto roomDto);
}
