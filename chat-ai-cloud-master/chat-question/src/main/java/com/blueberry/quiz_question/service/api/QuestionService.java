package com.blueberry.quiz_question.service.api;

import com.blueberry.quiz_question.model.QuestionRequest;
import com.blueberry.model.quetion.QuizDataDTO;

import java.util.List;

public interface QuestionService {
    String submitGenerateTask(QuestionRequest request);
    QuizDataDTO getQuestionBank(String bankId);
    List<QuizDataDTO> getUserQuestionBanks(long userId);
}
