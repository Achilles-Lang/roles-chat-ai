package com.blueberry.quiz_question.service.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blueberry.quiz_question.entity.QuestionBank;
import com.blueberry.quiz_question.mapper.QuestionBankMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuizDbService extends ServiceImpl<QuestionBankMapper, QuestionBank> {
    public QuestionBank getByBizId(String bizId) {
        return lambdaQuery().eq(QuestionBank::getBizId, bizId).one();
    }

    public List<QuestionBank> getUserQuizData(long userId) {
        return lambdaQuery().eq(QuestionBank::getUserId, userId).list();
    }
}
