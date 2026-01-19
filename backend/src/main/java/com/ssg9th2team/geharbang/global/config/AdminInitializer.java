package com.ssg9th2team.geharbang.global.config;

import com.ssg9th2team.geharbang.domain.auth.entity.Admin;
import com.ssg9th2team.geharbang.domain.auth.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component // 스프링이 이 클래스를 발견하고 자동으로 실행 준비를 하게 함
@RequiredArgsConstructor // 클래스 안의 'final'이 붙은 애들을 스프링이 자동으로 채워준다.
@Slf4j // 로그
public class AdminInitializer implements CommandLineRunner {
    // CommandLineRunner: 서버가 켜지자마자 이 안에 있는 run() 메서드를 실행

    private final AdminRepository adminRepository; // DB에 관리자 정보를 저장할 저장소
    private final PasswordEncoder passwordEncoder; // 비밀번호를 암호화해주는 도구

    @Override
    public void run(String... args) {
        // 중복 체크와 저장을 위해 사용할 이메일 변수
        String adminEmail = "admin@admin.com"; //유효성 검사

        // 1. DB에 "admin@admin.com"이라는 이름을 가진 관리자가 이미 있는지 확인해봄

        if (!adminRepository.existsByUsername(adminEmail)) {

            // 2. 없다면, "admin"이라는 비밀번호를 복잡하게 암호화
            String encodedPassword = passwordEncoder.encode("admin");

            // 3. 관리자 정보 (이메일은 admin@admin.com, 비밀번호는 암호화된 값)
            Admin admin = Admin.builder()
                    .username(adminEmail)
                    .password(encodedPassword)
                    .build();

            // 4. DB에 최종적으로 저장
            adminRepository.save(admin);

            // 로그 생성
            log.info("기본 관리자 계정({})이 생성되었습니다.", adminEmail);
        }
    }
}