package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.BookingDto;
import com.aman.projects.airBnbApp.dto.BookingRequest;
import com.aman.projects.airBnbApp.dto.GuestDto;
import com.aman.projects.airBnbApp.entity.*;
import com.aman.projects.airBnbApp.entity.enums.BookingStatus;
import com.aman.projects.airBnbApp.exceptions.ResourceNotFoundException;
import com.aman.projects.airBnbApp.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService{
    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;
    @Override
    @Transactional
    public BookingDto intialiseBooking(BookingRequest bookingRequest) {
        log.info("Intialising booking for hotel : {}, room: {}, date {} {}",bookingRequest.getHotelId(),
                bookingRequest.getRoomId(),bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate());
        Hotel hotel=hotelRepository.findById(bookingRequest.getHotelId()).orElseThrow(()->
                new ResourceNotFoundException("Hotel not found with id: "+bookingRequest.getHotelId()));
        Room room=roomRepository.findById(bookingRequest.getRoomId()).orElseThrow(()->
                new ResourceNotFoundException("Room not found with id: "+bookingRequest.getRoomId()));
        List<Inventory> inventoryList=inventoryRepository.findAndLockAvailableInventory(room.getId(),
                bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate(),bookingRequest.getRoomsCount());
        long daysCount= ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate())+1;
        if(inventoryList.size()!=daysCount){
            throw new IllegalStateException("Room is not available anymore");
        }
        for(Inventory inventory:inventoryList){
            inventory.setReservedCount(inventory.getReservedCount()+bookingRequest.getRoomsCount());
        }
        inventoryRepository.saveAll(inventoryList);
        //Creating the booking

        //TODO: CALCULATE DYNAMIC AMOUNT

        Booking booking=Booking.builder().
                bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel).
                room(room).
                checkInDate(bookingRequest.getCheckInDate()).
                checkOutDate(bookingRequest.getCheckOutDate()).
                user(getCurrentUser()).
                roomsCount(bookingRequest.getRoomsCount()).
                amount(BigDecimal.TEN).
                build();
        booking=bookingRepository.save(booking);
        return modelMapper.map(booking,BookingDto.class);
    }

    @Override
    public BookingDto addGuests(Long bookingId, List<GuestDto> guests) {
        log.info("Adding Guests for booking with id: {}", bookingId);
        Booking booking=bookingRepository.findById(bookingId).orElseThrow(
                ()->new ResourceNotFoundException("Booking not found with id: "+bookingId));
        if(hasBookingExpired(booking)){
            throw new IllegalStateException("booking has Already Expired");
        }
        if(booking.getBookingStatus()!=BookingStatus.RESERVED){
            throw new IllegalStateException("Booking is under reserved status, cannot add guests");
        }
        for(GuestDto guestDto:guests){
            Guest guest=modelMapper.map(guestDto, Guest.class);
            guest.setUser(getCurrentUser());
            guest= guestRepository.save(guest);
            booking.getGuests().add(guest);
        }
        booking.setBookingStatus(BookingStatus.GUEST_ADDED);
        booking=bookingRepository.save(booking);
        return modelMapper.map(booking,BookingDto.class);
    }
    public  boolean hasBookingExpired(Booking booking){
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }
    public User getCurrentUser(){
        User user=new User();    // TODO: REMOVE DUMMY USER
        user.setId(1L);
        return user;
    }


}
