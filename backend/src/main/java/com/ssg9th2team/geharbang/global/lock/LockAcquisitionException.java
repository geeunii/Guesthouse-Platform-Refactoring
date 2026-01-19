package com.ssg9th2team.geharbang.global.lock;

/**
 * 분산 락 획득 실패 시 발생하는 예외
 * HTTP 409 Conflict로 응답됨
 */
public class LockAcquisitionException extends RuntimeException {

    public LockAcquisitionException(String message) {
        super(message);
    }

    public LockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
