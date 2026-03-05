package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.RoomDto;
import com.aman.projects.airBnbApp.entity.Hotel;
import com.aman.projects.airBnbApp.entity.Room;
import com.aman.projects.airBnbApp.exceptions.ResourceNotFoundException;
import com.aman.projects.airBnbApp.repository.HotelRepository;
import com.aman.projects.airBnbApp.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;

    @Override
    public RoomDto createNewRoom(Long hotelId,RoomDto roomDto) {
        log.info("Creating a new Room with ID:{}",hotelId);
        Hotel hotel=hotelRepository.findById(hotelId)
        .orElseThrow(()->new
        ResourceNotFoundException("There is no property Listed with the given id:"+hotelId));
        Room room=modelMapper.map(roomDto,Room.class);
        room.setHotel(hotel);
        room=roomRepository.save(room);
        //TODO: create inventory as soon as room is create and hotel is active
        if(hotel.isActive()){
                inventoryService.intializeRoomForAYear(room);
        }
        return modelMapper.map(room, RoomDto.class);
    }

    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Getting all rooms in hotel with ID: {}",hotelId);
        Hotel hotel=hotelRepository.findById(hotelId)
        .orElseThrow(()->new
        ResourceNotFoundException("There is no property Listed with the given id:"+hotelId));
        return hotel.getRooms().stream()
                .map((element)->modelMapper.map(element, RoomDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public RoomDto getRoomById(Long roomId) {
        log.info("Getting the room with ID: {}",roomId);
        Room room=roomRepository.findById(roomId)
                .orElseThrow(()->new ResourceNotFoundException("Room not found with id:"+roomId));
        return modelMapper.map(room, RoomDto.class);
    }

    @Override
    public void deleteRoomById(Long roomId) {
     log.info("Deleting the room with ID:{}",roomId);
  /*   boolean exists= roomRepository.existsById(roomId);
     if(!exists){
         throw new ResourceNotFoundException("Room not found with id:"+roomId);
     }*/
        Room room=roomRepository.findById(roomId)
                .orElseThrow(()->new ResourceNotFoundException("Room not found with id:"+roomId));
     inventoryService.deleteFutureInventories(room);
        roomRepository.deleteById(roomId);
    }
}
