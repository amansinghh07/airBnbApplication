package com.aman.projects.airBnbApp.repository;

import com.aman.projects.airBnbApp.entity.Room;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoomRepository extends JpaRepository<Room,Long> {
    @Query("""
           SELECT r FROM Room r
                      JOIN FETCH r.hotel h
                      JOIN FETCH h.owner
                      WHERE r.id = :roomId                      
           """)
    Room findRoomWithHotelAndOwner(Long  roomId);
}
