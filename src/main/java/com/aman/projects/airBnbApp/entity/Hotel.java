package com.aman.projects.airBnbApp.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String city;
    @Column(columnDefinition = "TEXT[]")
    private String[] photos;
    @Column(columnDefinition = "TEXT[]")
    private String[] amenities;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @Embedded
    private HotelContactInfo hotelContactInfo;
    @Column(nullable = false)
    @JsonProperty("active")
    private boolean isActive;
/*    @OneToMany(mappedBy = "hotel",fetch = FetchType.LAZY)
    private List<Room> roomList;*/
    @ManyToOne
    private User owner;
    @OneToMany(mappedBy = "hotel")
    private List<Room>rooms;

}
