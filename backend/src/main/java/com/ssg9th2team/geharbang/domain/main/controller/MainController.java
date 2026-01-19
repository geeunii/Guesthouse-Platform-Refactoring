package com.ssg9th2team.geharbang.domain.main.controller;

import com.ssg9th2team.geharbang.domain.accommodation.service.AccommodationService;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.main.dto.AvailableRoomResponse;
import com.ssg9th2team.geharbang.domain.main.dto.AccommodationDetailDto;
import com.ssg9th2team.geharbang.domain.main.dto.MainAccommodationListResponse;
import com.ssg9th2team.geharbang.domain.main.service.MainService;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;
    private final AccommodationService accommodationService;
    private final UserRepository userRepository; // Inject UserRepository

    private final RoomJpaRepository roomJpaRepository;

    @GetMapping("/list")
    public MainAccommodationListResponse list(
            Authentication authentication, // Inject Authentication object
            @RequestParam(name = "themeIds", required = false) List<Long> themeIds,
            @RequestParam(name = "keyword", required = false) String keyword) {

        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String userEmail = authentication.getName();
            userId = userRepository.findByEmail(userEmail)
                    .map(com.ssg9th2team.geharbang.domain.auth.entity.User::getId)
                    .orElse(null); // If user not found, userId remains null
        }
        return mainService.getMainAccommodationList(userId, themeIds, keyword);
    }

    @GetMapping("/list/bulk")
    public Map<Long, MainAccommodationListResponse> listBulk(
            Authentication authentication,
            @RequestParam(name = "themeIds") List<Long> themeIds,
            @RequestParam(name = "keyword", required = false) String keyword) {
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String userEmail = authentication.getName();
            userId = userRepository.findByEmail(userEmail)
                    .map(com.ssg9th2team.geharbang.domain.auth.entity.User::getId)
                    .orElse(null);
        }

        if (themeIds == null || themeIds.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // 최적화된 벌크 조회 사용 (기존: 테마별 순차 조회 -> 신규: 한 번에 조회 후 그룹핑)
        return mainService.getMainAccommodationListBulk(userId, themeIds, keyword);
    }

    @GetMapping("/detail/{accommodationsId}")
    public AccommodationDetailDto accommodationDetail(@PathVariable Long accommodationsId) {
        return AccommodationDetailDto.from(accommodationService.getAccommodation(accommodationsId));
    }

    @GetMapping("/detail/{accommodationsId}/availability")
    public AvailableRoomResponse availableRooms(
            @PathVariable Long accommodationsId,
            @RequestParam(name = "checkin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
            @RequestParam(name = "checkout", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout,
            @RequestParam(name = "guestCount", required = false) Integer guestCount) {
        if (checkin == null || checkout == null) {
            return AvailableRoomResponse.of(List.of());
        }
        if (!checkout.isAfter(checkin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "체크아웃 날짜는 체크인 날짜 이후여야 합니다.");
        }
        LocalDateTime checkinAt = checkin.atTime(15, 0);
        LocalDateTime checkoutAt = checkout.atTime(11, 0);
        List<Long> roomIds = roomJpaRepository.findAvailableRoomIds(accommodationsId, checkinAt, checkoutAt,
                guestCount);
        return AvailableRoomResponse.of(roomIds);
    }
}
