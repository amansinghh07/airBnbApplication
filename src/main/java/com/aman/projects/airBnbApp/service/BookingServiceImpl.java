package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.dto.BookingDto;
import com.aman.projects.airBnbApp.dto.BookingReportDto;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.aman.projects.airBnbApp.utils.AppUtils.getCurrentUser;

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

    @Override
    public List<BookingDto> getAllBookingByHotelId(Long hotelId) {
        Hotel hotel=hotelRepository.findById(hotelId).orElseThrow(
                ()->new ResourceNotFoundException("Hotel not found with ID: "+hotelId));
        User user=getCurrentUser();
        if(!user.getId().equals(hotel.getOwner().getId())){
            throw new AccessDeniedException("User does not belong to this hotel");
        }
        log.info("Getting all the booking for hotel with ID: {}",hotel.getId());
        List<Booking>bookingList=bookingRepository.findByHotel(hotel);
        return bookingList.stream().map((element) -> modelMapper
                .map(element, BookingDto.class)).collect(Collectors.toList());
    }

    @Override
    public BookingReportDto getHotelReport(long hotelId, LocalDate startDate, LocalDate endDate) {
        Hotel hotel=hotelRepository.findById(hotelId).orElseThrow(
                ()->new ResourceNotFoundException("Hotel not found with ID: "+hotelId));
        User user=getCurrentUser();
        if(!user.getId().equals(hotel.getOwner().getId())){
            throw new AccessDeniedException("User does not belong to this hotel");
        }
        log.info("Getting the report for hotel with ID: {}",hotel.getId());
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<Booking>bookingList=bookingRepository.findByHotelAndCreatedAtBetween(hotel,startDateTime,endDateTime);
        long totalConfirmedBooking=bookingList.stream().filter
                ((element) -> element.getBookingStatus()==BookingStatus.CONFIRMED)
                .count();
        BigDecimal totalRevenueOfConfirmedBooking=bookingList.stream()
                .filter((ele)->ele.getBookingStatus()==BookingStatus.CONFIRMED)
                .map(Booking::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgRevenue=totalConfirmedBooking==0?BigDecimal.ZERO:
                totalRevenueOfConfirmedBooking.divide(BigDecimal.valueOf(totalConfirmedBooking)
                        ,BigDecimal.ROUND_HALF_UP);
        return new BookingReportDto(totalConfirmedBooking,totalRevenueOfConfirmedBooking,avgRevenue);
    }

    public  boolean hasBookingExpired(Booking booking){
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }


}
