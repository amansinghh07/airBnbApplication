package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.HotelDto;
import com.aman.projects.airBnbApp.dto.HotelPriceDto;
import com.aman.projects.airBnbApp.dto.HotelSearchRequestDto;
import com.aman.projects.airBnbApp.entity.Hotel;
import com.aman.projects.airBnbApp.entity.Inventory;
import com.aman.projects.airBnbApp.entity.Room;
import com.aman.projects.airBnbApp.repository.HotelMinPriceRepository;
import com.aman.projects.airBnbApp.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.loader.ast.spi.Loadable;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService{
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
}
