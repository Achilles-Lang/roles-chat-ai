package com.blueberry.quiz_question.service.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blueberry.quiz_question.entity.QuestionTO;
import com.blueberry.quiz_question.mapper.QuestionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionDbService extends ServiceImpl<QuestionMapper, QuestionTO> {
    public List<QuestionTO> listByQuizId(long quizId) {
        return lambdaQuery()
                .eq(QuestionTO::getQuestionId, quizId)
                .orderByAsc(QuestionTO::getSortOrder).list();
    }
}
