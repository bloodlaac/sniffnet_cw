package ru.yanaeva.sniffnet_cw.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yanaeva.sniffnet_cw.dto.common.ApiErrorResponse;
import ru.yanaeva.sniffnet_cw.exception.BadRequestException;
import ru.yanaeva.sniffnet_cw.exception.ConflictException;
import ru.yanaeva.sniffnet_cw.exception.IntegrationException;
import ru.yanaeva.sniffnet_cw.exception.NotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            request,
            errors
        );
    }

    @ExceptionHandler({
        BadRequestException.class,
        ConstraintViolationException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
        Exception ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            request,
            null
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
        BadCredentialsException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.UNAUTHORIZED,
            "Invalid username or password",
            request,
            null
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
        AccessDeniedException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.FORBIDDEN,
            "Access denied",
            request,
            null
        );
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
        NotFoundException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            request,
            null
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
        ConflictException ex,
        HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT,
            ex.getMessage(),
            request,
            null
        );
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegration(
        IntegrationException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.BAD_GATEWAY,
            ex.getMessage(), 
            equest,
            null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
        Exception ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected server error",
            request,
            null
        );
    }

    private ResponseEntity<ApiErrorResponse> build(
        HttpStatus status,
        String message,
        HttpServletRequest request,
        Map<String, String> validationErrors
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            validationErrors
        ));
    }
}
