package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.ai.client.GeminiApiClient;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.review.repository.mybatis.ReviewMapper;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationAiSummaryService {

    private final AccommodationJpaRepository accommodationRepository;
    private final ReviewMapper reviewMapper;
    private final ReviewJpaRepository reviewRepository;
    private final RoomJpaRepository roomRepository;
    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;

    private static final int PRICE_THRESHOLD_BUDGET = 30000;
    private static final int PRICE_THRESHOLD_REASONABLE = 80000;

    private static final Map<String, String> TIP_MAP = Map.of(
            "파티", "파티 참석을 원하시면 미리 신청하세요! 새로운 만남이 기다리고 있습니다.",
            "조용", "조용한 휴식을 위해 소등 시간을 지켜주세요. 온전한 쉼을 즐길 수 있습니다.",
            "힐링", "조용한 휴식을 위해 소등 시간을 지켜주세요. 온전한 쉼을 즐길 수 있습니다.",
            "뷰", "인생샷 포인트는 옥상입니다! 카메라를 꼭 챙기세요.",
            "사진", "인생샷 포인트는 옥상입니다! 카메라를 꼭 챙기세요.",
            "감성", "인생샷 포인트는 옥상입니다! 카메라를 꼭 챙기세요.",
            "깨끗", "깔끔한 잠자리에서 꿀잠 예약입니다. 편안한 밤 보내세요.",
            "침구", "깔끔한 잠자리에서 꿀잠 예약입니다. 편안한 밤 보내세요.",
            "조식", "조식 맛집으로 소문난 곳입니다. 아침 식사를 꼭 챙겨 드세요!",
            "맛", "조식 맛집으로 소문난 곳입니다. 아침 식사를 꼭 챙겨 드세요!"
    );

    private static final String DEFAULT_TIP = "인기 숙소이니 마감 전 예약을 서두르세요! 체크인 전 짐 보관 가능 여부를 미리 확인하면 더 편한 여행이 될 거예요.";

    @Transactional(readOnly = true)
    public AccommodationAiSummaryResponse generateSummary(Long accommodationId) {
        // 1. 데이터 조회 (공통)
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new IllegalArgumentException("Accommodation not found: " + accommodationId));

        List<String> topTags = reviewMapper.selectTop3TagsByAccommodationId(accommodationId);
        long reviewCount = reviewRepository.countByAccommodationsIdAndIsDeletedFalse(accommodationId);
        List<Room> rooms = roomRepository.findByAccommodationsId(accommodationId);

        // 2. Gemini API 호출 시도
        try {
            return generateGeminiSummary(accommodation, topTags, reviewCount);
        } catch (Exception e) {
            log.warn("Gemini API call failed. Falling back to Rule-based logic.", e);
            // 3. 실패 시 Rule-based 로직 실행 (Fallback)
            return generateRuleBasedSummary(accommodation, topTags, reviewCount, rooms);
        }
    }

    private AccommodationAiSummaryResponse generateGeminiSummary(Accommodation accommodation, List<String> topTags, long reviewCount) throws JsonProcessingException {
        String prompt = String.format(
                "다음 숙소 정보를 바탕으로 여행객을 위한 매력적인 요약을 JSON 형식으로 작성해줘.\n" +
                        "반환할 JSON 필드: moodDescription, tip\n" +
                        "moodDescription은 HTML 태그(<strong>, <br>)를 사용하여 강조해줘.\n" +
                        "숙소 이름: %s\n" +
                        "주소: %s\n" +
                        "소개글: %s\n" +
                        "상위 태그: %s",
                accommodation.getAccommodationsName(),
                accommodation.getCity() + " " + accommodation.getDistrict(),
                accommodation.getAccommodationsDescription(),
                String.join(", ", topTags)
        );

        String jsonResponse = geminiApiClient.generateContent(prompt);
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        String moodDescription = rootNode.path("moodDescription").asText();
        String tip = rootNode.path("tip").asText();

        // 공통 로직으로 생성 가능한 필드들은 재사용
        String name = accommodation.getAccommodationsName();
        String locationTag = generateLocationTag(accommodation.getAddressDetail()); // 간단히 주소 기반 생성
        List<String> keywords = topTags.stream().map(tag -> "#" + tag.replace(" ", "")).collect(Collectors.toList());

        return new AccommodationAiSummaryResponse(name, locationTag, keywords, moodDescription, tip, reviewCount);
    }

    private AccommodationAiSummaryResponse generateRuleBasedSummary(Accommodation accommodation, List<String> topTags, long reviewCount, List<Room> rooms) {
        String name = accommodation.getAccommodationsName();
        String address = String.format("%s %s %s %s",
                accommodation.getCity(),
                accommodation.getDistrict(),
                accommodation.getTownship(),
                accommodation.getAddressDetail()).trim();
        String description = accommodation.getAccommodationsDescription() != null ? accommodation.getAccommodationsDescription() : "";

        // 가격 및 객실 정보 분석
        int minPrice = rooms.stream()
                .map(Room::getPrice)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .min()
                .orElse(0);
        boolean hasDormitory = rooms.stream()
                .anyMatch(room -> room.getRoomName() != null && room.getRoomName().contains("도미토리"));

        // 가격대별 멘트
        String priceMent;
        if (minPrice <= PRICE_THRESHOLD_BUDGET) {
            priceMent = "갓성비! 부담 없이 머물기 좋은 알뜰 숙소";
        } else if (minPrice <= PRICE_THRESHOLD_REASONABLE) {
            priceMent = "가격과 시설의 균형이 잡힌 합리적인 숙소";
        } else {
            priceMent = "특별한 날을 위한 프리미엄 감성 숙소";
        }

        // 타겟 추천 멘트
        String targetMent = hasDormitory ? "혼자 온 여행객도 어색하지 않은 분위기" : "프라이빗한 휴식을 원하는 분들에게 추천";
        
        // Introduction 조합
        String introMent = String.format("이곳은 <strong>%s</strong>로, <strong>%s</strong>입니다.", priceMent, targetMent);

        List<String> keywords = new ArrayList<>();
        String moodDescription;
        String tip = DEFAULT_TIP;

        if (!topTags.isEmpty()) {
            // 태그 데이터 기반 분석
            keywords = topTags.stream()
                    .map(tag -> "#" + tag.replace(" ", ""))
                    .collect(Collectors.toList());

            String firstTag = topTags.get(0);
            
            // 문맥 인식 분위기 설명 생성 (Intro + Tag 분석)
            moodDescription = introMent + " " + generateMoodDescription(topTags);

            // 팁 생성 (Map 활용)
            for (Map.Entry<String, String> entry : TIP_MAP.entrySet()) {
                if (firstTag.contains(entry.getKey())) {
                    tip = entry.getValue();
                    break;
                }
            }

        } else {
            // 소개글 기반 분석 (Fallback)
            if (description.contains("파티")) {
                keywords.add("#파티맛집");
                keywords.add("#새로운만남");
                moodDescription = introMent + " 활기찬 에너지와 새로운 만남이 있는 곳입니다.";
                tip = TIP_MAP.get("파티");
            } else if (description.contains("조용") || description.contains("힐링")) {
                keywords.add("#조용한저녁");
                keywords.add("#불멍타임");
                moodDescription = introMent + " 조용한 휴식과 온전한 힐링을 즐길 수 있는 곳입니다.";
                tip = TIP_MAP.get("조용");
            } else if (description.contains("감성")) {
                keywords.add("#감성숙소");
                keywords.add("#인생샷명소");
                moodDescription = introMent + " 감각적인 인테리어와 포토존이 가득한 곳입니다.";
                tip = TIP_MAP.get("감성");
            } else {
                keywords.add("#가성비갑");
                keywords.add("#편안한잠자리");
                moodDescription = introMent + " 편안하고 아늑한 잠자리를 제공하는 가성비 좋은 숙소입니다.";
            }
        }

        // 위치 태그 생성
        String locationTag = generateLocationTag(address);

        return new AccommodationAiSummaryResponse(name, locationTag, keywords, moodDescription, tip, reviewCount);
    }

    private String generateLocationTag(String address) {
        if (address.contains("애월")) return "제주 서쪽의 핫플, 애월";
        if (address.contains("성산")) return "일출이 아름다운 성산";
        if (address.contains("함덕")) return "에메랄드빛 바다, 함덕";
        if (address.contains("서귀포")) return "따뜻한 남쪽 나라, 서귀포";
        return "제주 여행의 중심";
    }

    private String generateMoodDescription(List<String> topTags) {
        if (topTags == null || topTags.isEmpty()) {
            return "아직 리뷰 데이터가 충분하지 않지만, 사장님의 정성이 담긴 공간입니다.";
        }

        String firstTag = topTags.get(0);

        if (containsAny(firstTag, "파티", "재미", "놀기")) {
            return "여행객들의 리뷰 데이터가 증명하듯, <strong>새로운 만남과 즐거운 파티 분위기</strong>가 압도적인 곳입니다. 심심할 틈이 없는 여행을 원하신다면 최고의 선택이 될 거예요.";
        } else if (containsAny(firstTag, "조용", "쉬기", "침구", "방음")) {
            return "무엇보다 <strong>편안한 휴식과 꿀잠</strong>을 중요하게 생각하는 여행객들에게 사랑받는 곳입니다. 조용한 분위기 속에서 힐링하고 싶은 분들께 강력 추천합니다.";
        } else if (containsAny(firstTag, "뷰", "사진", "인테리어", "감성")) {
            return "어디를 찍어도 인생샷이 나오는 <strong>감각적인 인테리어와 뷰</strong>가 특징입니다. 눈이 즐거운 숙소를 찾는 분들의 만족도가 매우 높습니다.";
        } else if (containsAny(firstTag, "조식", "음식", "맛")) {
            return "숙박뿐만 아니라 <strong>음식 맛까지 훌륭한 찐 맛집</strong>입니다. 든든한 아침 식사로 기분 좋은 하루를 시작할 수 있습니다.";
        } else if (containsAny(firstTag, "가성비", "가격", "저렴")) {
            return "가격 대비 만족도가 매우 높은 <strong>가성비 끝판왕</strong> 숙소입니다. 합리적인 가격에 훌륭한 시설을 누릴 수 있어 재방문율이 높습니다.";
        } else if (containsAny(firstTag, "청결", "깨끗", "관리")) {
            return "들어서자마자 느껴지는 <strong>쾌적함과 청결함</strong>이 돋보이는 곳입니다. 위생에 민감한 여행객들도 안심하고 머물 수 있습니다.";
        } else {
            String safeTagName = HtmlUtils.htmlEscape(firstTag);
            return String.format("다녀간 여행객들이 입을 모아 <strong>'%s'</strong> 점을 칭찬하는 곳입니다. 실제 데이터가 증명하는 실패 없는 선택이 될 것입니다.", safeTagName);
        }
    }

    private boolean containsAny(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
    }
}
