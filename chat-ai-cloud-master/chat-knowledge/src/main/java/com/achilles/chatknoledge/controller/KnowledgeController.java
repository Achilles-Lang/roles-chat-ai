package com.achilles.chatknoledge.controller;

import com.achilles.chatknoledge.entity.KnowledgeDocument;
import com.achilles.chatknoledge.mapper.KnowledgeMapper;
import com.blueberry.model.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("doc")
public class KnowledgeController {

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    // 模拟上传文件
    @PostMapping("upload")
    public Result<String> upload(@RequestParam Long userId,
                                 @RequestParam String fileName,
                                 @RequestParam String description) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setUserId(userId);
        doc.setFileName(fileName);
        doc.setDescription(description);
        doc.setCreateTime(LocalDateTime.now());
        knowledgeMapper.insert(doc);
        return Result.success("文件信息保存成功（模拟上传）");
    }

    // 获取列表
    @GetMapping("list")
    public Result<List<KnowledgeDocument>> list(@RequestParam Long userId) {
        // 使用 MyBatis Plus 的简单查询
        // 这里需要你自己构建 QueryWrapper，或者直接 selectList(null) 演示
        return Result.success(knowledgeMapper.selectList(null));
    }
}
