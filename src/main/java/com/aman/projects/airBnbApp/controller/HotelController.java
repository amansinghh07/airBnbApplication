package com.aman.projects.airBnbApp.controller;

import com.aman.projects.airBnbApp.dto.BookingDto;
import com.aman.projects.airBnbApp.dto.BookingReportDto;
import com.aman.projects.airBnbApp.dto.HotelDto;
import com.aman.projects.airBnbApp.service.BookingService;
import com.aman.projects.airBnbApp.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/hotels")
@RequiredArgsConstructor
@Slf4j
public class HotelController {
    private final HotelService hotelService;
    private final BookingService bookingService;
    @PostMapping
    public ResponseEntity<HotelDto> createNewHotel(@RequestBody @Valid HotelDto hotelDto){
        log.info("Attemptimg to create a new Hotel with name: "+hotelDto.getName());
        HotelDto hotel=hotelService.createNewHotel(hotelDto);
        return new ResponseEntity<>(hotel, HttpStatus.CREATED);
    }
    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelDto>getHotelById(@PathVariable Long hotelId){
        HotelDto hotelDto=hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(hotelDto);
    }
    @GetMapping
    public ResponseEntity<List<HotelDto>>getAllHotels(){
        List<HotelDto> hotelDtos=hotelService.getAllHotels();
        return ResponseEntity.ok(hotelDtos);
    }
    @PutMapping("/{hotelId}")
    public ResponseEntity<HotelDto>updateHotelById(@PathVariable Long hotelId ,@RequestBody HotelDto hotelDto){
        HotelDto hotelDto1=hotelService.updateHotelById(hotelId,hotelDto);
        return ResponseEntity.ok(hotelDto1);
    }
    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void>deleteHotelById(@PathVariable Long hotelId){
        hotelService.deleteHotelById(hotelId);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{hotelId}")
    public ResponseEntity<Void>activateHotel(@PathVariable long hotelId){
        hotelService.activateHotel(hotelId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{hotelId}/bookings")
    public ResponseEntity<List<BookingDto>> getAllBookings(@PathVariable Long hotelId){
        return ResponseEntity.ok(bookingService.getAllBookingByHotelId(hotelId));
    }
    @GetMapping("/{hotelId}/reports")
    public ResponseEntity<BookingReportDto> getHotelReport(@PathVariable long hotelId,
                                                           @RequestParam(required = false)LocalDate startDate,
                                                           @RequestParam(required = false)LocalDate endDate){
        if(startDate==null){
            startDate=LocalDate.now().minusMonths(1);
        }
        if(endDate==null){ endDate=LocalDate.now();}
        return ResponseEntity.ok(bookingService.getHotelReport(hotelId,startDate,endDate));
    }




}
