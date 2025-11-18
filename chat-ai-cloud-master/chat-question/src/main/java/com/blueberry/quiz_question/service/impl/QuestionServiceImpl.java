package com.blueberry.quiz_question.service.impl;

import com.blueberry.quiz_question.model.QuestionRequest;
import com.blueberry.model.quetion.QuizDataDTO;
import com.blueberry.quiz_question.redis.RedisManager;
import com.blueberry.quiz_question.service.api.QuestionService;
import com.blueberry.quiz_question.service.api.QuizImportService;
import com.blueberry.quiz_question.service.db.QuestionDbService;
import com.blueberry.quiz_question.task.TaskManager;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionServiceImpl implements QuestionService {
    // 业务类型标识
    private static final String BUSINESS_TYPE = "QUESTION_BANK:";
    @Autowired
    private SensitiveWordBs sensitiveWordBs;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private QuestionAsyncService questionAsyncService;
    @Autowired
    private QuizImportService quizImportService;

    /**
     * 提交题库生成任务（入口方法）
     */
    @Override
    public String submitGenerateTask(QuestionRequest request) {

        String requestName = request.getName();
        List<String> foundWords = sensitiveWordBs.findAll(requestName);

        if (!foundWords.isEmpty()) {
            String firstFoundWord = foundWords.get(0);
            throw new IllegalArgumentException("输入信息包含敏感词[" + firstFoundWord + "]等，请修改后重试");
        }
        // 创建任务（通过TaskService，不直接操作Task实体）
        String taskId = taskManager.createTask(BUSINESS_TYPE);
        questionAsyncService.generateQuestionBankAsync(taskId, request);
        return taskId;  // 立即返回任务ID
    }

    @Override
    public QuizDataDTO getQuestionBank(String bankId) {
        return quizImportService.getQuizDataDTO(bankId);
    }

    @Override
    public List<QuizDataDTO> getUserQuestionBanks(long userId) {
        return quizImportService.getUserQuizData(userId);
    }
}
