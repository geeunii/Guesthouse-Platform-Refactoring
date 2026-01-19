package com.ssg9th2team.geharbang.domain.coupon.repository.mybatis;

import com.ssg9th2team.geharbang.domain.coupon.dto.UserCouponResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCouponMapper {

    // 상태별 쿠폰 조회 (ISSUED / USED / EXPIRED)
    List<UserCouponResponseDto> selectMyCouponsByStatus(@Param("userId") Long userId,
                                                        @Param("status") String status);
}
