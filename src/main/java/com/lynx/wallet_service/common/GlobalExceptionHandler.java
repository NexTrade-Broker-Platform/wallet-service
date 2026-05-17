package com.lynx.wallet_service.common;

import com.lynx.wallet_service.wallet.exception.InsufficientFundsException;
import com.lynx.wallet_service.wallet.exception.InsufficientReservedBalanceException;
import com.lynx.wallet_service.wallet.exception.WalletNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWalletNotFound(WalletNotFoundException ex) {
        return buildError("NOT_FOUND", ex.getMessage(), new HashMap<>(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFunds(InsufficientFundsException ex) {
        return buildError("INSUFFICIENT_FUNDS", ex.getMessage(), new HashMap<>(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InsufficientReservedBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientReservedBalance(InsufficientReservedBalanceException ex) {
        return buildError("INSUFFICIENT_RESERVED_BALANCE", ex.getMessage(), new HashMap<>(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        Map<String, String> details = new HashMap<>();
        details.put(ex.getParameterName(), ex.getParameterName() + " is required");
        return buildError("VALIDATION_ERROR", "The request payload failed validation.", details, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return buildError("VALIDATION_ERROR", "The request payload failed validation.", new HashMap<>(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }
        return buildError("VALIDATION_ERROR", "The request payload failed validation.", details, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getConstraintViolations().forEach(cv -> {
            String field = cv.getPropertyPath().toString();
            details.put(field.contains(".") ? field.substring(field.lastIndexOf('.') + 1) : field, cv.getMessage());
        });
        return buildError("VALIDATION_ERROR", "The request payload failed validation.", details, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildError("INTERNAL_SERVER_ERROR", "Something went wrong on the server.", new HashMap<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> buildError(String code, String message,
                                                           Map<String, String> details, HttpStatus status) {
        ErrorResponse error = new ErrorResponse(code, message, details);
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        return new ResponseEntity<>(body, status);
    }
}