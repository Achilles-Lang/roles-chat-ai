package com.blueberry.quiz_user.exception;

import com.blueberry.model.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class)
    public Result<Void> handleUsernameDuplicate(UserException e) {
        return Result.fail(409, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOtherException(Exception e) {
        e.printStackTrace();
        return Result.fail(500, "服务器内部错误");
    }
}
