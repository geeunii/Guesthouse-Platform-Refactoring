package com.ssg9th2team.geharbang.domain.admin.repository.mybatis;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationMetrics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminAccommodationMapper {
    List<AdminAccommodationMetrics> selectAccommodationMetrics(@Param("ids") List<Long> ids);

    @Select("""
        SELECT am.amenity_name
        FROM accommodation_amenity aa
        JOIN amenity am ON aa.amenity_id = am.amenity_id
        WHERE aa.accommodations_id = #{accommodationId}
    """)
    List<String> selectAmenitiesByAccommodationId(@Param("accommodationId") Long accommodationId);
}
