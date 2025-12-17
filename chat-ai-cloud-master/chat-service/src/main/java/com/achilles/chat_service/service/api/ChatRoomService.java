package com.achilles.chat_service.service.api;

import com.achilles.chat_service.entity.ChatMessage;
import com.achilles.chat_service.entity.ChatRoom;
import com.achilles.chat_service.entity.RoomAiPersona;

import java.util.List;

public interface ChatRoomService {
    ChatRoom createRoom(ChatRoom roomData, Long creatorId);

    // 获取所有房间列表
    List<ChatRoom> getRoomList();

    // 发送一条消息
    void sendMessage(Long roomId, Long senderId, String senderName, String content,String type,Long replyId);

    // 获取某个房间的历史消息
    List<ChatMessage> getHistoryMessages(Long roomId);

    void addAiToRoom(Long roomId, String aiName,String prompt,String apiKey,String modelName);
    // 获取房间内的所有AI角色
    List<RoomAiPersona> getRoomAiList(Long roomId);
    // 踢出房间内AI角色
    void deleteRoomAi(Long aiId);
    // 删除房间
    void deleteRoom(Long roomId);
    // 更新房间的信息
    void updateRoomInfo(ChatRoom room);
    // 置顶/取消置顶房间
    void togglePinRoom(Long roomId);
    // 删除消息
    void deleteMessage(Long messageId);
    // 更新AI信息
    void updateRoomAi(RoomAiPersona aiPersona);
    // 置顶/取消置顶AI
    void togglePinAi(Long aiId);
}