package com.blueberry.quiz_question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("quiz")
public class QuestionBank {
    @TableId(type = IdType.AUTO)
    private long id;
    @TableField("biz_id")
    private String bizId;
    @TableField("user_id")
    private long userId;
    private String name;
    private String description;
    @TableField("question_count")
    private int questionCount;
    private String difficulty;
}
