package com.aman.projects.airBnbApp.advice;

import com.aman.projects.airBnbApp.exceptions.ResourceNotFoundException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>>handleResourceNotFound(ResourceNotFoundException exception){
        ApiError apiError=ApiError.builder().
                httpStatus(HttpStatus.NOT_FOUND)
                        .message(exception.getMessage())
                                .build();
        return buildErrorResponseEntity(apiError);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>>handleInternalServerError(Exception exception){
        ApiError apiError=ApiError.builder().
                httpStatus(HttpStatus.INTERNAL_SERVER_ERROR).
                message(exception.getMessage()).
                build();
        return buildErrorResponseEntity(apiError);
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError>handleAuthenticException(AuthenticationException ex){
        ApiError apiError=ApiError.builder().message(ex.getLocalizedMessage())
                .httpStatus( HttpStatus.UNAUTHORIZED)
                .build();
        return new ResponseEntity<>(apiError,HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError>handleJwtException(JwtException ex){
        ApiError apiError=ApiError.builder().message(ex.getLocalizedMessage())
                .httpStatus( HttpStatus.UNAUTHORIZED)
                .build();
        return new ResponseEntity<>(apiError,HttpStatus.UNAUTHORIZED);
    }
        @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError>handleAccessDeniedException(AccessDeniedException ex){
        ApiError apiError=ApiError.builder().message(ex.getLocalizedMessage())
                .httpStatus(HttpStatus.FORBIDDEN)
                .build();
        return new ResponseEntity<>(apiError,HttpStatus.FORBIDDEN);
    }
    private ResponseEntity<ApiResponse<?>> buildErrorResponseEntity(ApiError apiError){
        ApiResponse<?> response = new ApiResponse<>();
        response.setApiError(apiError);

        return new ResponseEntity<>(response, apiError.getHttpStatus());
    }

}
