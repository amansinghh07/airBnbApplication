package com.aman.projects.airBnbApp.strategy;

import com.aman.projects.airBnbApp.entity.Inventory;

import java.math.BigDecimal;

public interface PricingStrategy {
    public BigDecimal calculatePrice(Inventory inventory);
}
