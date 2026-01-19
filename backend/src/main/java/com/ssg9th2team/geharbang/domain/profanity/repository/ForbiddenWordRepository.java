package com.ssg9th2team.geharbang.domain.profanity.repository;

import com.ssg9th2team.geharbang.domain.profanity.entity.ForbiddenWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForbiddenWordRepository extends JpaRepository<ForbiddenWord, Long> {

    @Query("SELECT f.word FROM ForbiddenWord f")
    List<String> findAllWords();
}
