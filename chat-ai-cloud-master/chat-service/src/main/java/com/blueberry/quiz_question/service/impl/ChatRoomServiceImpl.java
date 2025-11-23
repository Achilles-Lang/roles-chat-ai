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
        List<RoomAiPersona> bots = aiPersonaMapper.selectList(
                new LambdaQueryWrapper<RoomAiPersona>().eq(RoomAiPersona::getRoomId, roomId)
        );

        if (bots.isEmpty()) {
            return;
        }
        List<ChatMessage> historyList = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getRoomId, roomId)
                        .orderByDesc(ChatMessage::getCreateTime) // 倒序查，取最新的
                        .last("LIMIT 10")
        );

        // 拼接历史记录字符串
        // 格式类似于：
        // User1: 大家好
        StringBuilder historyBuilder = new StringBuilder();
        for (int i = historyList.size() - 1; i >= 0; i--) {
            ChatMessage msg = historyList.get(i);
            historyBuilder.append(msg.getSenderName()).append(": ").append(msg.getContent()).append("\n");
        }

        String finalHistory = "【这是群聊的历史记录】：\n" + historyBuilder.toString() +
                "\n【用户的最新发言】：\n" + userContent +
                "\n【请你根据历史记录接话】";

        for (RoomAiPersona bot : bots) {
            CompletableFuture.runAsync(() -> {
                // 防刷屏
                try {
                    if (Math.random()>0.6){
                        return;
                    }
                    // 随机延迟
                    Thread.sleep((long) (Math.random() * 3000) + 1000);

                    String reply = aiClient.chatDynamic(
                            bot.getApiKey(),
                            bot.getModelName(),
                            bot.getSystemPrompt(),
                            finalHistory
                    );

                    // 保存消息
                    ChatMessage aiMsg = new ChatMessage();
                    aiMsg.setRoomId(roomId);
                    aiMsg.setSenderId(0L);
                    aiMsg.setSenderName(bot.getAiName());
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

    @Override
    public void addAiToRoom(Long roomId, String aiName, String prompt, String apiKey, String modelName) {
        RoomAiPersona persona = new RoomAiPersona();
        persona.setRoomId(roomId);
        persona.setAiName(aiName);
        persona.setSystemPrompt(prompt);

        // 新增的两个字段
        persona.setApiKey(apiKey);
        persona.setModelName(modelName);

        persona.setAiAvatar("");
        aiPersonaMapper.insert(persona);
    }

}