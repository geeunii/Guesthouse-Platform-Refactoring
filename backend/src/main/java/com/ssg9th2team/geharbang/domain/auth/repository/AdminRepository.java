package com.ssg9th2team.geharbang.domain.auth.repository;

import com.ssg9th2team.geharbang.domain.auth.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    // 관리자 username으로 조회
    Optional<Admin> findByUsername(String username);

    // username 중복 체크
    boolean existsByUsername(String username);
}
