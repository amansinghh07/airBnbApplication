package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.BookingDto;
import com.aman.projects.airBnbApp.dto.BookingRequest;
import com.aman.projects.airBnbApp.dto.GuestDto;
import com.aman.projects.airBnbApp.entity.*;
import com.aman.projects.airBnbApp.entity.enums.BookingStatus;
import com.aman.projects.airBnbApp.exceptions.ResourceNotFoundException;
import com.aman.projects.airBnbApp.exceptions.UnAuthorizedException;
import com.aman.projects.airBnbApp.repository.*;
import com.aman.projects.airBnbApp.strategy.PricingService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final CheckoutService checkoutService;
    private final PricingService pricingService;
    @Value("${frontend.url}")
    private String frontendUrl;
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
        inventoryRepository.initBooking(room.getId(),
                bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate(),bookingRequest.getRoomsCount());

        BigDecimal priceForOneRoom=pricingService.calculateTotalPrice(inventoryList);
        BigDecimal totalPrice=priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));

        Booking booking=Booking.builder().
                bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel).
                room(room).
                checkInDate(bookingRequest.getCheckInDate()).
                checkOutDate(bookingRequest.getCheckOutDate()).
                user(getCurrentUser()).
                roomsCount(bookingRequest.getRoomsCount()).
                amount(totalPrice).
                build();
        booking=bookingRepository.save(booking);
        return modelMapper.map(booking,BookingDto.class);
    }

    @Override
    @Transactional
    public BookingDto addGuests(Long bookingId, List<GuestDto> guests) {
        log.info("Adding Guests for booking with id: {}", bookingId);
        Booking booking=bookingRepository.findById(bookingId).orElseThrow(
                ()->new ResourceNotFoundException("Booking not found with id: "+bookingId));
        User user=getCurrentUser();
        if(!user.getId().equals(booking.getUser().getId())){
         throw new UnAuthorizedException("Booking does not belong to id: "+user.getId());
        }
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

    @Override
    @Transactional
    public String intiatePayments(Long bookingId) {
        Booking booking=bookingRepository.findById(bookingId).orElseThrow(()->
                new ResourceNotFoundException("Booking not found with id: "+bookingId));
        User user=getCurrentUser();
        if(!user.getId().equals(booking.getUser().getId())){
            throw new UnAuthorizedException("Booking does not belong to id: "+user.getId());
        }
        if(hasBookingExpired(booking)){
            throw new IllegalStateException("booking has Already Expired");
        }
        String sessionUrl=
        checkoutService.getCheckoutSession(booking,frontendUrl+"/payments/success",frontendUrl+"/payments/failure");
        booking.setBookingStatus(BookingStatus.PAYMENTS_PENDING);
        bookingRepository.save(booking);

        return sessionUrl;
    }

    @Override
    @Transactional
    public void capturePayment(Event event) {
        if("checkout.session.completed".equals(event.getType())){
            Session session=(Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if(session==null) return;
            String sessionId=session.getId();
            Booking booking=
                    bookingRepository.findByPaymentSessionId(sessionId).orElseThrow(()->
                            new ResourceNotFoundException("Booking not found for Session ID :"+sessionId));
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(),booking.getCheckInDate(),
                    booking.getCheckOutDate(),booking.getRoomsCount());
            inventoryRepository.confirmBooking(booking.getRoom().getId(),booking.getCheckInDate(),
                    booking.getCheckOutDate(),booking.getRoomsCount());
            log.info("Successfully confirmed the booking for Booking ID: {}",booking.getId());
        }else{
            log.warn("Unhandeled event type: {}",event.getType());
        }

    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking=bookingRepository.findById(bookingId).orElseThrow(()->
                new ResourceNotFoundException("Booking not found with id: "+bookingId));
        User user=getCurrentUser();
        if(!user.getId().equals(booking.getUser().getId())){
            throw new UnAuthorizedException("Booking does not belong to id: "+user.getId());
        }
        if(booking.getBookingStatus()!=BookingStatus.CONFIRMED){
            throw new IllegalStateException("Only confirmed booking can be expired");
        }
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(),booking.getCheckInDate(),
                booking.getCheckOutDate(),booking.getRoomsCount());
        inventoryRepository.cancelBooking(booking.getRoom().getId(),booking.getCheckInDate(),
                booking.getCheckOutDate(),booking.getRoomsCount());

        // Handling the refund
        try{
            Session session=Session.retrieve(booking.getPaymentSessionId());
            RefundCreateParams refundParams= RefundCreateParams.builder().
                    setPaymentIntent(session.getPaymentIntent())
                    .build();
            Refund.create(refundParams);
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }

        log.info("Successfully cancelled the booking for Booking ID: {}",booking.getId());

    }

    public  boolean hasBookingExpired(Booking booking){
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }
    public User getCurrentUser(){
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }


}
