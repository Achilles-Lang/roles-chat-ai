package com.blueberry.quiz_question.service.impl;

import com.blueberry.model.quetion.QuestionDTO;
import com.blueberry.model.quetion.QuizDataDTO;
import com.blueberry.model.quetion.QuizDifficulty;
import com.blueberry.quiz_question.entity.Option;
import com.blueberry.quiz_question.entity.QuestionBank;
import com.blueberry.quiz_question.entity.QuestionTO;
import com.blueberry.quiz_question.service.api.QuizImportService;
import com.blueberry.quiz_question.service.db.OptionDbService;
import com.blueberry.quiz_question.service.db.QuestionDbService;
import com.blueberry.quiz_question.service.db.QuizDbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizImportServiceImpl implements QuizImportService {
    @Autowired
    private QuizDbService quizDbService;
    @Autowired
    private QuestionDbService questionDbService;
    @Autowired
    private OptionDbService optionDbService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importQuizData(long userId, QuizDataDTO quizData) {
        QuestionBank quiz = buildQuiz(userId, quizData);
        boolean quizSaved = quizDbService.save(quiz);
        if (!quizSaved) {
            throw new RuntimeException("题库保存失败");
        }
        long quizId = quiz.getId(); //由数据库自增写回

        List<QuestionDTO> questions = quizData.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            QuestionDTO questionDTO = questions.get(i);
            QuestionTO questionTO = buildQuestionTO(quizId, questionDTO, i);
            boolean questionSaved = questionDbService.save(questionTO);
            if (!questionSaved) {
                throw new RuntimeException("题目保存失败：" + questionDTO.getTitle());
            }
            long questionId = questionTO.getId();

            List<String> answers = questionDTO.getAnswers();
            for (int j = 0; j < answers.size(); j++) {
                Option option = buildOption(questionId, answers.get(j), j);
                boolean optionSaved = optionDbService.save(option);
                if (!optionSaved) {
                    throw new RuntimeException("选项保存失败：" + answers.get(j));
                }
            }
        }
    }

    @Override
    public QuizDataDTO getQuizDataDTO(String quizBizId) {
        QuestionBank quiz = quizDbService.getByBizId(quizBizId);
        if (quiz == null) {
            throw new RuntimeException("题库不存在：" + quizBizId);
        }
        QuizDataDTO quizData = buildQuizData(quiz);

        List<QuestionTO> questions = questionDbService.listByQuizId(quiz.getId());
        List<QuestionDTO> questionDTOs = new ArrayList<>();

        for (QuestionTO question : questions) {
            var questionDTO = buildQuestionDTO(question);
            questionDTOs.add(questionDTO);
        }
        quizData.setQuestions(questionDTOs);

        return quizData;
    }

    @Override
    public List<QuizDataDTO> getUserQuizData(long userId) {
        var quizBanks = quizDbService.getUserQuizData(userId);
        return quizBanks.stream().map(this::buildQuizData).toList();
    }


    private QuestionBank buildQuiz(long userId, QuizDataDTO dto) {
        QuestionBank quiz = new QuestionBank();
        quiz.setBizId(dto.getId());
        quiz.setUserId(userId);
        quiz.setName(dto.getName());
        quiz.setDescription(dto.getDescription());
        quiz.setDifficulty(dto.getDifficulty().name());
        quiz.setQuestionCount(dto.getQuestionCount());
        return quiz;
    }

    private QuestionTO buildQuestionTO(long quizId, QuestionDTO dto, int sortOrder) {
        QuestionTO questionTO = new QuestionTO();
        questionTO.setQuestionId(quizId);
        questionTO.setTitle(dto.getTitle());
        questionTO.setRightAnswer(dto.getRightAnswer());
        questionTO.setExplanation(dto.getExplanation());
        questionTO.setDifficulty(dto.getDifficulty().name());
        questionTO.setSortOrder(sortOrder);
        return questionTO;
    }

    private Option buildOption(long questionId, String content, int sortOrder) {
        Option option = new Option();
        option.setQuestionId(questionId);
        option.setContent(content);
        option.setSortOrder(sortOrder);
        return option;
    }

    private QuestionDTO buildQuestionDTO(QuestionTO questionTO) {
        var questionDTO = new QuestionDTO();
        questionDTO.setTitle(questionTO.getTitle());
        questionDTO.setRightAnswer(questionTO.getRightAnswer());
        questionDTO.setExplanation(questionTO.getExplanation());
        questionDTO.setDifficulty(QuizDifficulty.valueOf(questionTO.getDifficulty()));

        List<Option> options = optionDbService.listByQuestionId(questionTO.getId());
        List<String> answers = options.stream()
                .map(Option::getContent)
                .collect(Collectors.toList());
        questionDTO.setAnswers(answers);
        return questionDTO;
    }

    private QuizDataDTO buildQuizData(QuestionBank quiz) {
        QuizDataDTO quizData = new QuizDataDTO();
        quizData.setId(quiz.getBizId());
        quizData.setName(quiz.getName());
        quizData.setDescription(quiz.getDescription());
        quizData.setQuestionCount(quiz.getQuestionCount());
        quizData.setDifficulty(QuizDifficulty.valueOf(quiz.getDifficulty()));
        return quizData;
    }
}
