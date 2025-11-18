package com.blueberry.quiz_question.controller;

import com.blueberry.model.common.Result;
import com.blueberry.quiz_question.model.QuestionRequest;
import com.blueberry.model.quetion.QuizDataDTO;
import com.blueberry.quiz_question.service.api.QuestionService;
import com.blueberry.quiz_question.task.Task;
import com.blueberry.quiz_question.task.TaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("question")
public class QuestionController {
    @Autowired
    private QuestionService questionService;
    @Autowired
    private TaskManager taskManager;

    /**
     * 提交题库生成任务
     */
    @PostMapping("/generate")
    public Result<String> generateQuestionBank(@RequestBody QuestionRequest request) {
        String taskId = questionService.submitGenerateTask(request);
        return Result.success(taskId);  // 返回任务ID，供查询状态
    }

    /**
     * 查询任务状态
     */
    @GetMapping("/task/status")
    public Result<Task> queryTaskStatus(@RequestParam("taskId") String taskId) {
        Task task = taskManager.getTaskById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        return Result.success(task);
    }

    /**
     * 查询题库详情
     */
    @GetMapping("/{id}")
    public Result<QuizDataDTO> getQuestionBank(@PathVariable String id) {
        QuizDataDTO response = questionService.getQuestionBank(id);
        if (response == null) {
            return Result.fail("题库不存在");
        }
        return Result.success(response);
    }

    @GetMapping("/user/banks")
    public Result<List<QuizDataDTO>> getUserQuestionBanks(@RequestParam("userId") Long userId) {
        var result = questionService.getUserQuestionBanks(userId);
        if (result == null) {
            return Result.fail("生成记录获取失败");
        }
        return Result.success(result);
    }
}
