package com.blueberry.quiz_question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question_option")
public class Option {
    @TableId(type = IdType.AUTO)
    private long id;
    @TableField("question_id")
    private long questionId;
    private String content;
    @TableField("sort_order")
    private int sortOrder;
}
