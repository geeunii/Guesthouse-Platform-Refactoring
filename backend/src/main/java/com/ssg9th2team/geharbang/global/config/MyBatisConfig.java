package com.ssg9th2team.geharbang.global.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = {
        "com.ssg9th2team.geharbang.domain.**.repository.mybatis",
        "com.ssg9th2team.geharbang.domain.dashboard.host.repository",
        "com.ssg9th2team.geharbang.domain.recommendation.repository"
})
public class MyBatisConfig {
}
