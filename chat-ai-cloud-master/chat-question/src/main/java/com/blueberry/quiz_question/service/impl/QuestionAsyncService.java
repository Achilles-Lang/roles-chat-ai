package com.blueberry.quiz_question.service.impl;

import com.blueberry.quiz_question.ai.QuizAIClient;
import com.blueberry.quiz_question.model.QuestionRequest;
import com.blueberry.model.quetion.QuizDataDTO;
import com.blueberry.quiz_question.redis.RedisManager;
import com.blueberry.quiz_question.service.api.QuizImportService;
import com.blueberry.quiz_question.task.TaskManager;
import com.blueberry.quiz_question.task.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class QuestionAsyncService {
    @Autowired
    private QuizAIClient quizAIClient;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private QuizImportService quizImportService;

    /**
     * 异步生成题库（耗时操作）
     */
    @Async("taskExecutor")  // 引用线程池
    public void generateQuestionBankAsync(String taskId, QuestionRequest request) {
        try {
            // 更新任务状态为“处理中”
            taskManager.updateTaskStatus(taskId, TaskStatus.PROCESSING, null, null, null);

            // 执行题库生成逻辑
            QuizDataDTO bank = doGenerateQuestionBank(request);  // 实际生成逻辑

            // 生成成功，更新状态和结果地址
            String resultUrl = "/api/question-bank/" + bank.getId();  // 题库详情地址
            taskManager.updateTaskStatus(taskId, TaskStatus.SUCCESS, bank.getId(), resultUrl, null);

        } catch (Exception e) {
            // 生成失败，更新错误信息
            taskManager.updateTaskStatus(taskId, TaskStatus.FAILED, null, null, e.getMessage());
        }
    }

    /**
     * 题库生成逻辑
     */
    private QuizDataDTO doGenerateQuestionBank(QuestionRequest request) {
        var quizDataDTO = quizAIClient.generateQuestionBank(request);
        quizImportService.importQuizData(request.getUserId(),quizDataDTO);
        return quizDataDTO;
    }
}
