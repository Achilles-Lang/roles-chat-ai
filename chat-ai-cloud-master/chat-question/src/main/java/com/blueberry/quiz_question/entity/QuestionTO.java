package com.blueberry.quiz_question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question")
public class QuestionTO {
    @TableId(type = IdType.AUTO)
    private long id;
    @TableField("quiz_id")
    private long questionId;
    private String title;
    @TableField("right_answer")
    private String rightAnswer;
    private String explanation;
    private String difficulty;
    @TableField("sort_order")
    private int sortOrder;
}
