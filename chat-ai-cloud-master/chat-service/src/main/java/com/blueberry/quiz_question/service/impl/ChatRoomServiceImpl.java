package com.blueberry.quiz_question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blueberry.quiz_question.ai.ChatAIClient;
import com.blueberry.quiz_question.entity.ChatMessage;
import com.blueberry.quiz_question.entity.ChatRoom;
import com.blueberry.quiz_question.entity.RoomAiPersona;
import com.blueberry.quiz_question.mapper.ChatMessageMapper;
import com.blueberry.quiz_question.mapper.ChatRoomMapper;
import com.blueberry.quiz_question.mapper.RoomAiPersonaMapper;
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
    private ChatAIClient aiClient;

    @Autowired
    private RoomAiPersonaMapper aiPersonaMapper;

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
        // 1. 查出这个房间里住了哪些 AI
        List<RoomAiPersona> bots = aiPersonaMapper.selectList(
                new LambdaQueryWrapper<RoomAiPersona>().eq(RoomAiPersona::getRoomId, roomId)
        );

        if (bots.isEmpty()) {
            System.out.println("当前房间没有 AI 入驻。");
            return; // 如果没配置AI，就不回话
        }

        // 2. 让所有 AI 开始干活（这里演示“全部回复”，实际可以做随机回复）
        for (RoomAiPersona bot : bots) {
            CompletableFuture.runAsync(() -> {
                try {
                    // 为了让聊天看起来自然，每个 AI 随机延迟 1-3 秒
                    Thread.sleep((long) (Math.random() * 3000) + 1000);

                    // 调用 AI，传入特定的人设
                    String reply = aiClient.chatWithPersona(bot.getSystemPrompt(), userContent);

                    // 保存消息
                    ChatMessage aiMsg = new ChatMessage();
                    aiMsg.setRoomId(roomId);
                    aiMsg.setSenderId(0L); // 还是 0，代表系统
                    aiMsg.setSenderName(bot.getAiName()); // **关键：用具体角色的名字**
                    aiMsg.setContent(reply);
                    aiMsg.setType("AI");
                    aiMsg.setCreateTime(LocalDateTime.now());

                    chatMessageMapper.insert(aiMsg);
                    System.out.println(bot.getAiName() + " 已回复");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
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