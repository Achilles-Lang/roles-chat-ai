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
import org.springframework.web.servlet.View;

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
    @Autowired
    private View error;

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
                new LambdaQueryWrapper<ChatRoom>()
                        .orderByDesc(ChatRoom::getIsPinned)
                        .orderByDesc(ChatRoom::getCreateTime)
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
        System.out.println("开始为房间 " + roomId + " 寻找 AI 角色...");
        List<RoomAiPersona> bots = aiPersonaMapper.selectList(
                new LambdaQueryWrapper<RoomAiPersona>().eq(RoomAiPersona::getRoomId, roomId)
        );
        System.out.println("房间 " + roomId + " 中共有 " + bots.size() + " 个 AI 角色。");

        if (bots.isEmpty()) {
            System.out.println("房间 " + roomId + " 没有 AI，停止召唤。");
            return;
        }
        List<ChatMessage> historyList = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getRoomId, roomId)
                        .orderByDesc(ChatMessage::getCreateTime) // 倒序查，取最新的
                        .last("LIMIT 10")
        );

        // 拼接历史记录字符串，格式类似于：User1: 大家好
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
                // AI思考效果
                ChatMessage thinkingMSG = new ChatMessage();
                thinkingMSG.setRoomId(roomId);
                thinkingMSG.setSenderId(0L);
                thinkingMSG.setSenderName(bot.getAiName());
                thinkingMSG.setContent("思考中...");
                thinkingMSG.setType("THINKING");
                thinkingMSG.setCreateTime(LocalDateTime.now());

                chatMessageMapper.insert(thinkingMSG);
                Long msgId = thinkingMSG.getId();

                try {
                    System.out.println("AI [" + bot.getAiName() + "] (模型: " + bot.getModelName() + ") 准备回复...");
                    // 防刷屏
                    if (Math.random()>0.6){
                        return;
                    }
                    // 随机延迟
                    Thread.sleep((long) (Math.random() * 300) + 100);

                    String reply = aiClient.chatDynamic(
                            bot.getApiKey(),
                            bot.getModelName(),
                            bot.getSystemPrompt(),
                            finalHistory
                    );

                    ChatMessage updateMsg = new ChatMessage();
                    updateMsg.setId(msgId);
                    updateMsg.setContent(reply);
                    updateMsg.setType("AI");

                    chatMessageMapper.updateById(updateMsg);
                    System.out.println("AI [" + bot.getAiName() + "] 回复更新完毕 (ID: " + msgId + ")");

                    if (reply == null || reply.startsWith("(AI连接失败")) {
                        System.err.println("AI [" + bot.getAiName() + "] 回复失败: " + reply);
                        return;
                    }
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
                    ChatMessage errorMSG = new ChatMessage();
                    errorMSG.setId(msgId);
                    errorMSG.setContent("思考过程中断了："+e.getMessage());
                    errorMSG.setType("AI");
                    chatMessageMapper.updateById(errorMSG);
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

    @Override
    public List<RoomAiPersona> getRoomAiList(Long roomId) {
        return aiPersonaMapper.selectList(
                new LambdaQueryWrapper<RoomAiPersona>().eq(RoomAiPersona::getRoomId,roomId)
        );
    }

    /**
     * 踢出AI角色
     * @param aiId
     */
    @Override
    public void deleteRoomAi(Long aiId) {
        aiPersonaMapper.deleteById(aiId);
    }
    /**
     * 删除房间
     * @param roomId
     */
    @Override
    public void deleteRoom(Long roomId) {
        chatRoomMapper.deleteById(roomId);
    }
    /**
     * 更新房间的信息
     * @param room
     */
    @Override
    public void updateRoomInfo(ChatRoom room) {
        chatRoomMapper.updateById(room);
    }

    /**
     * 置顶房间
     * @param roomId
     */
    @Override
    public void togglePinRoom(Long roomId) {
        ChatRoom room = chatRoomMapper.selectById(roomId);
        if(room != null){
            // 取反
            boolean currentPinnedStatus = Boolean.TRUE.equals(room.getIsPinned());
            room.setIsPinned(!currentPinnedStatus);
            chatRoomMapper.updateById(room);
        }
    }

}