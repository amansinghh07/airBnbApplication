package com.aman.projects.airBnbApp.service;

import com.aman.projects.airBnbApp.entity.Booking;

public interface CheckoutService {
    String getCheckoutSession(Booking booking, String successUrl, String failedUrl);
}
