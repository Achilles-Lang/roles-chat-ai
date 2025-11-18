package com.blueberry.model.quetion;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuizDataDTO {
    private String id;
    private String name;
    private String description;
    private int questionCount;
    private QuizDifficulty difficulty;
    private int estimatedTime;
    private int views;
    private int practiceCount;
    private String visibility;
    private List<QuestionDTO> questions;
    private LocalDateTime createdAt;
}
