package com.aman.projects.airBnbApp.repository;

import com.aman.projects.airBnbApp.entity.Booking;
import com.aman.projects.airBnbApp.entity.Hotel;
import com.aman.projects.airBnbApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking,Long> {
    Optional<Booking> findByPaymentSessionId(String sessionId);

    List<Booking> findByHotel(Hotel hotel);
    List<Booking>findByHotelAndCreatedAtBetween(Hotel hotel, LocalDateTime start, LocalDateTime end);
    List<Booking>findByUser(User user);
    boolean existsByGuests_Id(Long guestId);

}
