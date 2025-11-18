package com.blueberry.quiz_question.service.api;

import com.blueberry.model.quetion.QuizDataDTO;

import java.util.List;

public interface QuizImportService {
    void importQuizData(long userId, QuizDataDTO quizData);
    QuizDataDTO getQuizDataDTO(String quizBizId);
    List<QuizDataDTO> getUserQuizData(long userId);
}
