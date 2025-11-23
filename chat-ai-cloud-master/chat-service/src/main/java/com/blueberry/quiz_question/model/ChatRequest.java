package com.blueberry.quiz_question.model;


import com.blueberry.model.quetion.QuestionDTO;

public class ChatRequest {
    private int chatId; //未登录时随机生成，登录后使用uid
    private QuestionDTO question; //问题上下文
    private String userQuestion; //用户提的问题

    public int getChatId() {
        return chatId;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }

    public QuestionDTO getQuestion() {
        return question;
    }

    public void setQuestion(QuestionDTO question) {
        this.question = question;
    }

    public String getUserQuestion() {
        return userQuestion;
    }

    public void setUserQuestion(String userQuestion) {
        this.userQuestion = userQuestion;
    }
}
