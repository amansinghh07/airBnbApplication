package com.aman.projects.airBnbApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingReportDto {
    private Long bookingCount;
    private BigDecimal totalRevenue;
    private BigDecimal averageRevenue;
}
