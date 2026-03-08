package com.aman.projects.airBnbApp.advice;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiResponse<T> {
    private ApiError apiError;
    private T data;
    private LocalDateTime timeStamp;
    public ApiResponse(){
        this.timeStamp=LocalDateTime.now();
    }
    public ApiResponse(T data){
        this();
        this.data=data;
    }
    public ApiResponse(ApiError error){
        this();
        this.apiError=error;
    }
}
