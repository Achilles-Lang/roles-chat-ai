package com.blueberry.quiz_question.service.api;

import com.blueberry.quiz_question.entity.ChatMessage;
import com.blueberry.quiz_question.entity.ChatRoom;
import com.blueberry.quiz_question.entity.RoomAiPersona;

import java.util.List;

public interface ChatRoomService {
    // 创建一个新房间
    ChatRoom createRoom(String name, Long creatorId);

    // 获取所有房间列表
    List<ChatRoom> getRoomList();

    // 发送一条消息
    void sendMessage(Long roomId, Long senderId, String senderName, String content);

    // 获取某个房间的历史消息
    List<ChatMessage> getHistoryMessages(Long roomId);

    void addAiToRoom(Long roomId, String aiName,String prompt,String apiKey,String modelName);
    // 获取房间内的所有AI角色
    List<RoomAiPersona> getRoomAiList(Long roomId);
}