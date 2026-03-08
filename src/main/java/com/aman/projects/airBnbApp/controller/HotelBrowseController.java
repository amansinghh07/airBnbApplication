package com.aman.projects.airBnbApp.controller;

import com.aman.projects.airBnbApp.dto.HotelDto;
import com.aman.projects.airBnbApp.dto.HotelInfoDto;
import com.aman.projects.airBnbApp.dto.HotelPriceDto;
import com.aman.projects.airBnbApp.dto.HotelSearchRequestDto;
import com.aman.projects.airBnbApp.service.HotelService;
import com.aman.projects.airBnbApp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelBrowseController {
    private final InventoryService inventoryService;
    private final HotelService hotelService;
    @GetMapping("/search")
    public ResponseEntity<Page<HotelPriceDto>>searchHotels(@RequestBody HotelSearchRequestDto hotelSearchRequestDto){
       Page<HotelPriceDto> page= inventoryService.searchHotels(hotelSearchRequestDto);
      return ResponseEntity.ok(page);
    }
    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDto>getHotelInfo(@PathVariable Long hotelId){
        return ResponseEntity.ok(hotelService.getHotelInfo(hotelId));
    }


}
