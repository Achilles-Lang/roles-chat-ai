package com.achilles.chatknowledge.controller;

import com.achilles.chatknowledge.service.KnowledgeService;
import com.achilles.model.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    // 上传文件接口
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "description", required = false) String description) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        knowledgeService.processUpload(file, description);
        return Result.success("文件已存入知识库");
    }

    // 内部调用接口：搜索知识
    @GetMapping("/search")
    public Result<List<String>> search(@RequestParam("query") String query) {
        List<String> results = knowledgeService.search(query);
        return Result.success(results);
    }
}
