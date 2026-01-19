package com.ssg9th2team.geharbang.global.util;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationCodeService {

    private final Map<String, String> emailCodeStorage = new ConcurrentHashMap<>();
    private final Map<String, Long> codeExpiration = new ConcurrentHashMap<>();

    private static final long EXPIRATION_TIME = 3 * 60 * 1000; // 3분 유효시간 걸어두기

    public String generateAndSaveCode(String email) {
        String code = String.valueOf((int)(Math.random() * 900000) + 100000); // 6-digit code
        emailCodeStorage.put(email, code);
        codeExpiration.put(email, System.currentTimeMillis() + EXPIRATION_TIME);
        return code;
    }

    public boolean verifyCode(String email, String code) {
        String storedCode = emailCodeStorage.get(email);
        Long expirationTime = codeExpiration.get(email);

        if (storedCode == null || expirationTime == null) {
            return false;
        }

        if (System.currentTimeMillis() > expirationTime) {
            removeCode(email);
            return false;
        }

        if (storedCode.equals(code)) {
            removeCode(email);
            return true;
        }
        return false;
    }

    // 비밀번호 찾기용: 검증만 하고 삭제하지 않음
    public boolean verifyCodeOnly(String email, String code) {
        String storedCode = emailCodeStorage.get(email);
        Long expirationTime = codeExpiration.get(email);

        if (storedCode == null || expirationTime == null) {
            return false;
        }

        if (System.currentTimeMillis() > expirationTime) {
            return false; // 만료되었지만 삭제하지 않음
        }

        return storedCode.equals(code); // 검증만 하고 삭제하지 않음
    }

    public void removeCode(String email) {
        emailCodeStorage.remove(email);
        codeExpiration.remove(email);
    }
}
