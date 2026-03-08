package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.HotelDto;
import com.aman.projects.airBnbApp.dto.HotelInfoDto;
import com.aman.projects.airBnbApp.dto.RoomDto;
import com.aman.projects.airBnbApp.entity.Hotel;
import com.aman.projects.airBnbApp.entity.Room;
import com.aman.projects.airBnbApp.entity.User;
import com.aman.projects.airBnbApp.exceptions.ResourceNotFoundException;
import com.aman.projects.airBnbApp.exceptions.UnAuthorizedException;
import com.aman.projects.airBnbApp.repository.HotelRepository;
import com.aman.projects.airBnbApp.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService{
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;
    private final RoomRepository roomRepository;
//    private final
    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        log.info("Creating a new hotel with name: {}",hotelDto.getName());
        Hotel hotel=modelMapper.map(hotelDto,Hotel.class);
        hotel.setActive(false);
        User user=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        hotel.setOwner(user);
        hotel=hotelRepository.save(hotel);
        log.info("Created a new Hotel with ID:{}",hotel.getId());
        return modelMapper.map(hotel,HotelDto.class);
    }

    @Override
    public HotelDto getHotelById(Long id) {
        log.info("Getting the hotel with ID: {}",id);
        Hotel hotel=hotelRepository.findById(id)
                .orElseThrow
                        (()->new ResourceNotFoundException("There is no property Listed with the given id:"+id));
        User user=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(user.equals(hotel.getOwner())){
            throw new UnAuthorizedException("This user does not own this hotel with id: "+id);
        }
        return modelMapper.map(hotel,HotelDto.class);
    }

    @Override
    public HotelDto updateHotelById(Long id, HotelDto hotelDto) {
        log.info("Updating the hotel with ID: {}",id);
        Hotel hotel=hotelRepository.findById(id)
                .orElseThrow
                        (()->new ResourceNotFoundException("There is no property Listed with the given id:"+id));
        User user=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(user.equals(hotel.getOwner())){
            throw new UnAuthorizedException("This user does not own this hotel with id: "+id);
        }
        modelMapper.map(hotelDto,hotel);
        hotel.setId(id);
        hotel=hotelRepository.save(hotel);
        return modelMapper.map(hotel,HotelDto.class);
    }

    @Override
    @Transactional
    public void deleteHotelById(Long id) {
    /*    boolean exists = hotelRepository.existsById(id);
        if(!exists){
            throw new ResourceNotFoundException("There is no property Listed with the given id:"+id);
        }*/
        Hotel hotel=hotelRepository.findById(id)
                .orElseThrow
                        (()->new ResourceNotFoundException("There is no property Listed with the given id:"+id));
        //TODO:to delete the future inventories for this hotel
        User user=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(user.equals(hotel.getOwner())){
            throw new UnAuthorizedException("This user does not own this hotel with id: "+id);
        }
        for(Room room:hotel.getRooms()){
            inventoryService.deleteFutureInventories(room);
            roomRepository.deleteById(room.getId());
        }
        hotelRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void activateHotel(Long id) {
        log.info("Activating the hotel with id: {}",id);
        Hotel hotel=hotelRepository.findById(id)
                .orElseThrow
                        (()->new ResourceNotFoundException("There is no property Listed with the given id:"+id));
        User user=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(user.equals(hotel.getOwner())){
            throw new UnAuthorizedException("This user does not own this hotel with id: "+id);
        }
        hotel.setActive(true);
        //TODO: Create Inventory for all the rooms of this hotel
        for(Room room:hotel.getRooms()){
            inventoryService.intializeRoomForAYear(room);
        }
    }

    @Override
    public List<HotelDto> getAllHotels() {

       List<Hotel>hotels= hotelRepository.findAll();
      List<HotelDto>hotelDtoList= hotels.stream().map
              (elements->modelMapper.map(elements,HotelDto.class))
              .collect(Collectors.toList());
        return hotelDtoList;
    }

    @Override
    public HotelInfoDto getHotelInfo(Long hotelId) {
        Hotel hotel=hotelRepository.findById(hotelId)
                .orElseThrow
                        (()->new ResourceNotFoundException("There is no property Listed with the given id:"+hotelId));
        List<RoomDto> rooms = hotel.getRooms().
        stream().map(room -> modelMapper.map(room,RoomDto.class)).toList();
        return new HotelInfoDto(modelMapper.map(hotel,HotelDto.class),rooms);

    }
}
