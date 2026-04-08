package com.commerce.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TossPaymentException.class)
    public ResponseEntity<Map<String, Object>> handleTossPaymentException(TossPaymentException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotFoundException(PaymentNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "결제 정보를 찾을 수 없습니다.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = (error instanceof FieldError) ? ((FieldError) error).getField() : "unknown";
            String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid value";
            errors.put(fieldName, message);
        });
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 400);
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
