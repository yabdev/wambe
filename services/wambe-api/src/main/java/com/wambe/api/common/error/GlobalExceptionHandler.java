package com.wambe.api.common.error;

import com.wambe.api.common.web.RequestIdFilter;
import com.wambe.api.generated.model.ErrorEnvelope;
import com.wambe.api.generated.model.ErrorEnvelopeError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorEnvelope> handleApi(ApiException exception, HttpServletRequest request) {
        return response(
                exception.status(),
                exception.code(),
                exception.getMessage(),
                exception.fieldErrors(),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorEnvelope> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, List<String>> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        error -> error.getField(),
                        Collectors.mapping(
                                error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                                Collectors.toList())));
        return response(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDATION_FAILED",
                "One or more fields are invalid",
                fieldErrors,
                request);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class
    })
    ResponseEntity<ErrorEnvelope> handleMalformed(Exception exception, HttpServletRequest request) {
        return response(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDATION_FAILED",
                "The request could not be validated",
                Map.of(),
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorEnvelope> handleConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "RESOURCE_CONFLICT",
                "The request conflicts with existing state",
                Map.of(),
                request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorEnvelope> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled API failure", exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "The request could not be completed",
                Map.of(),
                request);
    }

    private ResponseEntity<ErrorEnvelope> response(
            HttpStatus status,
            String code,
            String message,
            Map<String, List<String>> fieldErrors,
            HttpServletRequest request) {
        String requestId = String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE));
        var error = new ErrorEnvelopeError(code, message, requestId);
        if (!fieldErrors.isEmpty()) {
            error.fieldErrors(fieldErrors);
        }
        return ResponseEntity.status(status).body(new ErrorEnvelope(error));
    }
}
