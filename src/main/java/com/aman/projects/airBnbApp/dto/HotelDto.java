package com.aman.projects.airBnbApp.dto;

import com.aman.projects.airBnbApp.entity.HotelContactInfo;
import com.aman.projects.airBnbApp.entity.Room;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HotelDto {
    private Long id;
    @NotBlank(message = "Name of the Hotel cannot be blank")
    private String name;
    private String city;
    private String[] photos;
    private String[] amenities;
    private HotelContactInfo hotelContactInfo;
    private boolean isActive;
//    @OneToMany(mappedBy = "hotel",fetch = FetchType.LAZY)
//    private List<Room> roomList;
}
