package com.blueberry.quiz_question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blueberry.quiz_question.ai.ChatAIClient;
import com.blueberry.quiz_question.entity.ChatMessage;
import com.blueberry.quiz_question.entity.ChatRoom;
import com.blueberry.quiz_question.mapper.ChatMessageMapper;
import com.blueberry.quiz_question.mapper.ChatRoomMapper;
import com.blueberry.quiz_question.service.api.ChatRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ChatRoomServiceImpl implements ChatRoomService {

    @Autowired
    private ChatRoomMapper chatRoomMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ChatAIClient aiClient; // 注入 AI 客户端

    @Override
    public ChatRoom createRoom(String name, Long creatorId) {
        ChatRoom room = new ChatRoom();
        room.setRoomName(name);
        room.setCreatorId(creatorId);
        room.setCreateTime(LocalDateTime.now());
        chatRoomMapper.insert(room);
        return room;
    }

    @Override
    public List<ChatRoom> getRoomList() {
        return chatRoomMapper.selectList(
                new LambdaQueryWrapper<ChatRoom>().orderByDesc(ChatRoom::getCreateTime)
        );
    }

    @Override
    public void sendMessage(Long roomId, Long senderId, String senderName, String content) {
        // 1. 打印日志：确认收到了消息
        System.out.println("收到消息 -> 房间: " + roomId + ", 发送者: " + senderName + ", 内容: " + content);

        // 2. 先保存用户发的消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setRoomId(roomId);
        userMsg.setSenderId(senderId);
        userMsg.setSenderName(senderName);
        userMsg.setContent(content);
        userMsg.setType("TEXT");
        userMsg.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(userMsg);

        // 3. 异步触发 AI 回复 (防止自己触发自己)
        if (!"AI助手".equals(senderName)) {
            System.out.println("正在准备召唤 AI...");
            triggerAIReply(roomId, content);
        } else {
            System.out.println("这是 AI 自己发的消息，不触发回复。");
        }
    }

    /**
     * 专门用来召唤 AI 的异步方法
     */
    private void triggerAIReply(Long roomId, String userContent) {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("AI 线程开始运行...");

                // 调用 AI 获取回复
                String aiResponse = aiClient.chat(userContent);

                // 打印 AI 的原始回复
                System.out.println("AI 原始回复: " + aiResponse);

                if (aiResponse == null || aiResponse.isEmpty()) {
                    System.out.println("AI 回复为空，不保存！");
                    return;
                }

                // 把 AI 的回复存进数据库
                ChatMessage aiMsg = new ChatMessage();
                aiMsg.setRoomId(roomId);
                aiMsg.setSenderId(0L); // 0 代表系统/AI
                aiMsg.setSenderName("AI助手"); // 名字叫 AI助手
                aiMsg.setContent(aiResponse);
                aiMsg.setType("AI");
                aiMsg.setCreateTime(LocalDateTime.now());

                chatMessageMapper.insert(aiMsg);

                System.out.println("AI 回复已成功存入数据库！");

            } catch (Exception e) {
                System.err.println("AI 线程发生异常！！");
                e.printStackTrace();
            }
        });
    }

    @Override
    public List<ChatMessage> getHistoryMessages(Long roomId) {
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getRoomId, roomId)
                        .orderByAsc(ChatMessage::getCreateTime)
                        .last("LIMIT 50")
        );
    }
}