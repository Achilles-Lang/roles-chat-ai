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
import java.util.ArrayList;
import java.util.Collections;
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
                new LambdaQueryWrapper<ChatRoom>()
                        .orderByDesc(ChatRoom::getIsPinned)
                        .orderByDesc(ChatRoom::getCreateTime)
        );
    }

    @Override
    public void sendMessage(Long roomId, Long senderId, String senderName, String content, String type, Long replyToId) {
        System.out.println("收到消息 -> 类型: " + type + ", 内容长度: " + (content != null ? content.length() : 0));

        // 1. 先保存用户发的消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setRoomId(roomId);
        userMsg.setSenderId(senderId);
        userMsg.setSenderName(senderName);
        userMsg.setContent(content);
        userMsg.setReplyToId(replyToId);
        userMsg.setIsDeleted(0);
        userMsg.setType(type != null ? type : "TEXT");
        userMsg.setCreateTime(LocalDateTime.now());

        chatMessageMapper.insert(userMsg);

        // 2. 只有文本消息且发送者不是AI助手时，才触发AI回复
        if ("TEXT".equals(userMsg.getType()) && !"AI助手".equals(senderName)) {
            System.out.println("正在准备召唤 AI...");
            triggerAIReply(roomId, content);
        }
    }

    /**
     * 专门用来召唤 AI 的方法
     * 修复核心：先在主线程插入“思考中”，再异步去更新内容
     */
    private void triggerAIReply(Long roomId, String userContent) {

        System.out.println("开始为房间 " + roomId + " 寻找 AI 角色...");
        List<RoomAiPersona> bots = aiPersonaMapper.selectList(
                new LambdaQueryWrapper<RoomAiPersona>().eq(RoomAiPersona::getRoomId, roomId)
        );

        if (bots.isEmpty()) {
            System.out.println("房间 " + roomId + " 没有 AI，停止召唤。");
            return;
        }

        // 准备历史记录 (这一步还在主线程)
        List<ChatMessage> historyList = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getRoomId, roomId)
                        .orderByDesc(ChatMessage::getCreateTime)
                        .last("LIMIT 10")
        );

        // 拼接历史记录字符串
        StringBuilder historyBuilder = new StringBuilder();
        // 因为查出来是倒序(最新在最前)，所以要反向遍历拼接，变成正常的时间顺序
        for (int i = historyList.size() - 1; i >= 0; i--) {
            ChatMessage msg = historyList.get(i);
            historyBuilder.append(msg.getSenderName()).append(": ").append(msg.getContent()).append("\n");
        }

        String finalHistory = "【这是群聊的历史记录】：\n" + historyBuilder.toString() +
                "\n【用户的最新发言】：\n" + userContent +
                "\n【请你根据历史记录接话】";

        // 遍历所有机器人
        for (RoomAiPersona bot : bots) {

            // =================================================================
            // 【重点修复】在主线程立刻插入“思考中”消息
            // 这样前端发送完消息立刻请求列表时，数据库里已经有这条消息了！
            // =================================================================
            ChatMessage thinkingMSG = new ChatMessage();
            thinkingMSG.setRoomId(roomId);
            thinkingMSG.setSenderId(0L); // 0 表示 AI
            thinkingMSG.setSenderName(bot.getAiName());
            thinkingMSG.setContent("思考中...");
            thinkingMSG.setType("THINKING");
            thinkingMSG.setCreateTime(LocalDateTime.now());

            chatMessageMapper.insert(thinkingMSG);
            Long msgId = thinkingMSG.getId(); // 拿到插入后的 ID，传给异步线程

            // 开启异步线程去调用大模型
            CompletableFuture.runAsync(() -> {
                try {
                    System.out.println("AI [" + bot.getAiName() + "] 正在思考 (ID: " + msgId + ")...");

                    // 模拟思考延迟，让动画多展示一会
                    Thread.sleep((long) (Math.random() * 500) + 200);

                    String replyContent = "";
                    String msgType = "AI";

                    // 调用 AI 接口
                    if(bot.getModelName() != null && bot.getModelName().toLowerCase().startsWith("wanx")){
                        replyContent = aiClient.generateImage(bot.getApiKey(), userContent);
                        if(replyContent.startsWith("https:")) {
                            msgType = "IMAGE";
                        }
                    } else {
                        replyContent = aiClient.chatDynamic(
                                bot.getApiKey(),
                                bot.getModelName(),
                                bot.getSystemPrompt(),
                                finalHistory
                        );
                    }

                    // =================================================================
                    // 【重点修复】只执行 updateById，绝对不要再 insert 了！
                    // =================================================================
                    ChatMessage updateMsg = new ChatMessage();
                    updateMsg.setId(msgId); // 锁定刚才那条“思考中”的消息
                    updateMsg.setContent(replyContent);
                    updateMsg.setType(msgType);
                    // updateMsg.setCreateTime(LocalDateTime.now()); // 可选：更新为回复时间

                    chatMessageMapper.updateById(updateMsg);
                    System.out.println("AI [" + bot.getAiName() + "] 回复更新完毕");

                } catch (Exception e) {
                    e.printStackTrace();
                    // 发生异常也要更新消息，告诉用户出错了
                    ChatMessage errorMSG = new ChatMessage();
                    errorMSG.setId(msgId);
                    errorMSG.setContent("思考中断：" + e.getMessage());
                    errorMSG.setType("AI");
                    chatMessageMapper.updateById(errorMSG);
                }
            });
        }
    }

    @Override
    public List<ChatMessage> getHistoryMessages(Long roomId) {
        // 1. 先按时间倒序（DESC）查询，确保拿到的是“最新”的 50 条
        List<ChatMessage> list = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getRoomId, roomId)
                        .orderByDesc(ChatMessage::getCreateTime)
                        .last("LIMIT 50")
        );

        // 2. 如果列表为空，直接返回
        if (list == null || list.isEmpty()) {
            return list;
        }

        // 3. 在内存中反转列表，恢复成“旧->新”的时间顺序
        List<ChatMessage> result = new ArrayList<>(list);
        Collections.reverse(result);

        return result;
    }

    @Override
    public void addAiToRoom(Long roomId, String aiName, String prompt, String apiKey, String modelName) {
        RoomAiPersona persona = new RoomAiPersona();
        persona.setRoomId(roomId);
        persona.setAiName(aiName);
        persona.setSystemPrompt(prompt);
        persona.setApiKey(apiKey);
        persona.setModelName(modelName);
        persona.setAiAvatar("");
        aiPersonaMapper.insert(persona);
    }

    @Override
    public List<RoomAiPersona> getRoomAiList(Long roomId) {
        return aiPersonaMapper.selectList(
                new LambdaQueryWrapper<RoomAiPersona>().eq(RoomAiPersona::getRoomId, roomId)
        );
    }

    @Override
    public void deleteRoomAi(Long aiId) {
        aiPersonaMapper.deleteById(aiId);
    }

    @Override
    public void deleteRoom(Long roomId) {
        chatRoomMapper.deleteById(roomId);
    }

    @Override
    public void updateRoomInfo(ChatRoom room) {
        chatRoomMapper.updateById(room);
    }

    @Override
    public void togglePinRoom(Long roomId) {
        ChatRoom room = chatRoomMapper.selectById(roomId);
        if(room != null){
            boolean currentPinnedStatus = Boolean.TRUE.equals(room.getIsPinned());
            room.setIsPinned(!currentPinnedStatus);
            chatRoomMapper.updateById(room);
        }
    }

    @Override
    public void deleteMessage(Long messageId) {
        chatMessageMapper.deleteById(messageId);
    }
}