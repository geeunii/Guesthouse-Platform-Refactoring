package com.ssg9th2team.geharbang.domain.profanity.service;

import com.ssg9th2team.geharbang.domain.profanity.repository.ForbiddenWordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfanityFilterService {

    private final ForbiddenWordRepository forbiddenWordRepository;
    private Set<String> forbiddenWordCache = new HashSet<>();

    @PostConstruct
    public void init() {
        reloadForbiddenWords();
    }

    public void reloadForbiddenWords() {
        List<String> words = forbiddenWordRepository.findAllWords();
        forbiddenWordCache = words.stream()
                .map(this::normalizeToken)
                .filter(token -> token != null && !token.isBlank())
                .collect(Collectors.toSet());
        log.info("금칙어 {}개 로드 완료", forbiddenWordCache.size());
    }

    public boolean containsProfanity(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalizedText = normalizeToken(text);
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }
        for (String word : forbiddenWordCache) {
            if (normalizedText.contains(word)) {
                return true;
            }
        }
        return false;
    }

    public void validateNoProfanity(String text, String fieldName) {
        if (containsProfanity(text)) {
            throw new IllegalArgumentException(fieldName + "에 부적절한 표현이 포함되어 있습니다.");
        }
    }

    private String normalizeToken(String text) {
        if (text == null) {
            return null;
        }
        return text.toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[^가-힣a-zA-Z0-9]", "");
    }
}
