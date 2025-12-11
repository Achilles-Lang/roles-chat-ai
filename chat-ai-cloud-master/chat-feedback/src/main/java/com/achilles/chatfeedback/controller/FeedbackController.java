package com.achilles.chatfeedback.controller;

import com.achilles.chatfeedback.entity.AiMessageFeedback;
import com.achilles.chatfeedback.mapper.FeedbackMapper;
import com.blueberry.model.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("feedback")
public class FeedbackController {
    @Autowired
    private FeedbackMapper feedbackMapper;

    // 提交反馈
    @PostMapping("submit")
    public Result<String> submitFeedback(@RequestBody AiMessageFeedback feedback) {
        feedback.setCreateTime(LocalDateTime.now());
        feedbackMapper.insert(feedback);
        return Result.success("反馈提交成功");
    }

    // 获取某个AI的平均分
    @GetMapping("stats")
    public Result<String> getStats(@RequestParam String aiName) {
        return Result.success(aiName + " 的好评率统计功能开发中");
    }
}
