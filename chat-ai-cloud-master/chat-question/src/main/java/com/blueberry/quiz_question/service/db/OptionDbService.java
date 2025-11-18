package com.blueberry.quiz_question.service.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blueberry.quiz_question.entity.Option;
import com.blueberry.quiz_question.mapper.OptionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OptionDbService extends ServiceImpl<OptionMapper, Option> {
    public List<Option> listByQuestionId(long questionId) {
        return lambdaQuery()
                .eq(Option::getQuestionId, questionId)
                .orderByAsc(Option::getSortOrder).list();
    }
}
