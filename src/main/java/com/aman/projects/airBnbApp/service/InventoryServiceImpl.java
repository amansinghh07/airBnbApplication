package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.*;
import com.aman.projects.airBnbApp.entity.Hotel;
import com.aman.projects.airBnbApp.entity.Inventory;
import com.aman.projects.airBnbApp.entity.Room;
import com.aman.projects.airBnbApp.entity.User;
import com.aman.projects.airBnbApp.exceptions.ResourceNotFoundException;
import com.aman.projects.airBnbApp.repository.HotelMinPriceRepository;
import com.aman.projects.airBnbApp.repository.InventoryRepository;
import com.aman.projects.airBnbApp.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.loader.ast.spi.Loadable;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.aman.projects.airBnbApp.utils.AppUtils.getCurrentUser;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService{
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;
    private final HotelMinPriceRepository hotelMinPriceRepository;
    @Override
    public void intializeRoomForAYear(Room room) {
        LocalDate today=LocalDate.now();
        LocalDate endDate=today.plusYears(1);
        for(;!today.isAfter(endDate);today=today.plusDays(1)){
            Inventory inventory=Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .city(room.getHotel().getCity())
                    .date(today)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            inventoryRepository.save(inventory);
        }
    }

    @Override
    public void deleteFutureInventories(Room room) {
        log.info("Deleting the inventories of room with id: {}",room.getId());
     inventoryRepository.deleteByRoom(room);
    }

    @Override
    public Page<HotelPriceDto> searchHotels(HotelSearchRequestDto hotelSearchRequestDto) {
        log.info("Searching hotels for {} city,from {} to {}",hotelSearchRequestDto.getCity(),
                hotelSearchRequestDto.getStartDate(),hotelSearchRequestDto.getEndDate());
        Pageable pageable= PageRequest.of(hotelSearchRequestDto.getPage(),hotelSearchRequestDto.getSize());
        long dateCount= ChronoUnit.DAYS.between
                (hotelSearchRequestDto.getStartDate(),hotelSearchRequestDto.getEndDate())+1;
        //Business Logic
        Page<HotelPriceDto>hotelPage=hotelMinPriceRepository.findHotelsWithAvailableInventory(hotelSearchRequestDto.getCity(),
                hotelSearchRequestDto.getStartDate(),hotelSearchRequestDto.getEndDate(),hotelSearchRequestDto.getRoomsCount(),
                dateCount,pageable);
        return hotelPage;
    }

    @Override
    public List<InventoryDto> getAllInventoryByRoom(Long roomId) {
        log.info("Getting all inventories for room with id: {}",roomId);
        Room room=roomRepository.findById(roomId).orElseThrow(()->
                new ResourceNotFoundException("Room not found with id: "+roomId));
        User user=getCurrentUser();
        if(!user.getId().equals(room.getHotel().getOwner().getId())){
            throw new AccessDeniedException("You are not the owner of this room with ID: "+roomId);
        }
        return inventoryRepository.findByRoomOrderByDate(room).stream().
                map((element) -> modelMapper.map(element, InventoryDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateInventory(Long roomId, UpdateInventoryRequestDto updateInventoryRequestDto) {
        log.info("Updating the inventories for room with id: {} between date range:{} - {}",roomId
        ,updateInventoryRequestDto.getStartDate(),updateInventoryRequestDto.getEndDate());
/*        Room room=roomRepository.findById(roomId).orElseThrow(()->new ResourceNotFoundException
                ("Room not found with id: "+roomId));*/
        Room room=roomRepository.findRoomWithHotelAndOwner(roomId);
        User user=getCurrentUser();
        if(!user.equals(room.getHotel().getOwner())){
         throw new AccessDeniedException("You are not the owner of this room with ID: "+roomId);
        }
        inventoryRepository.getInventoryAndLockBeforeUpdate(roomId,updateInventoryRequestDto.getStartDate()
                ,updateInventoryRequestDto.getEndDate());
        inventoryRepository.updateInventory(roomId,updateInventoryRequestDto.getStartDate(),
                updateInventoryRequestDto.getEndDate(), updateInventoryRequestDto.getClosed(),
                updateInventoryRequestDto.getSurgeFactor());
    }
}
