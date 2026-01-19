package com.ssg9th2team.geharbang.domain.holiday.exception;

public class HolidayUpstreamException extends RuntimeException {

    private final int status;

    public HolidayUpstreamException(int status, String message) {
        super(message);
        this.status = status;
    }

    public HolidayUpstreamException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
