package com.ssg9th2team.geharbang.domain.holiday.exception;

import com.ssg9th2team.geharbang.domain.holiday.controller.HolidayController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = HolidayController.class)
public class HolidayExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<HolidayErrorResponse> handleInvalidRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new HolidayErrorResponse("INVALID_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(HolidayConfigException.class)
    public ResponseEntity<HolidayErrorResponse> handleConfig(HolidayConfigException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new HolidayErrorResponse("HOLIDAY_CONFIG", ex.getMessage()));
    }

    @ExceptionHandler(HolidayUpstreamException.class)
    public ResponseEntity<HolidayErrorResponse> handleUpstream(HolidayUpstreamException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatus());
        HttpStatus resolved = status != null ? status : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(resolved)
                .body(new HolidayErrorResponse("HOLIDAY_UPSTREAM", ex.getMessage()));
    }

    static class HolidayErrorResponse {
        private final String code;
        private final String message;

        HolidayErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
