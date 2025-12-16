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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
        // 存用户消息
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

        // 只有文本消息且发送者不是AI助手时，才召唤AI
        if ("TEXT".equals(userMsg.getType()) && !"AI助手".equals(senderName)) {
            System.out.println("收到用户消息，准备召唤 AI...");
            triggerAIReply(roomId, content);
        }
    }

    /**
     * 专门用来召唤 AI 的方法
     *
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

        // 准备历史记录
        List<ChatMessage> historyList = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getRoomId, roomId)
                        .orderByDesc(ChatMessage::getCreateTime) // 使用 createTime (映射 create_time)
                        .last("LIMIT 10")
        );

        StringBuilder historyBuilder = new StringBuilder();
        for (int i = historyList.size() - 1; i >= 0; i--) {
            ChatMessage msg = historyList.get(i);
            historyBuilder.append(msg.getSenderName()).append(": ").append(msg.getContent()).append("\n");
        }

        String finalHistory = "【历史记录】:\n" + historyBuilder.toString() +
                "\n【用户最新发言】:\n" + userContent +
                "\n【请接着话茬回复】";

        for (RoomAiPersona bot : bots) {

            // 主线程：立刻插入“思考中”占位符
            ChatMessage thinkingMSG = new ChatMessage();
            thinkingMSG.setRoomId(roomId);
            thinkingMSG.setSenderId(0L);
            thinkingMSG.setSenderName(bot.getAiName());
            thinkingMSG.setContent("思考中...");
            thinkingMSG.setType("THINKING");
            thinkingMSG.setCreateTime(LocalDateTime.now());

            thinkingMSG.setIsDeleted(0);

            chatMessageMapper.insert(thinkingMSG);
            Long msgId = thinkingMSG.getId();

            // 异步线程：调用 AI
            CompletableFuture.runAsync(() -> {
                try {
                    System.out.println("AI [" + bot.getAiName() + "] 正在请求接口...");

                    String replyContent = "";
                    String msgType = "AI";

                    // 检查 Key
                    String apiKey = bot.getApiKey();
                    if (apiKey == null || apiKey.trim().isEmpty()) {
                        System.out.println("提示：该 AI 未配置独立 API Key，将尝试使用系统默认 Key。");
                    }

                    // 调用接口
                    if(bot.getModelName() != null && bot.getModelName().toLowerCase().startsWith("wanx")){
                        replyContent = aiClient.generateImage(apiKey, userContent);
                        if(replyContent != null && replyContent.startsWith("https:")) {
                            msgType = "IMAGE";
                        }
                    } else {
                        replyContent = aiClient.chatDynamic(
                                apiKey,
                                bot.getModelName(),
                                bot.getSystemPrompt(),
                                finalHistory
                        );
                    }

                    if (replyContent == null || replyContent.trim().isEmpty()) {
                        System.err.println("严重警告：AI 接口返回了空内容！");
                        replyContent = "⚠️ AI 无法回复：接口返回内容为空。请检查后台日志，确认 API Key 是否有效或余额是否充足。";
                    }

                    // 更新数据库
                    ChatMessage updateMsg = new ChatMessage();
                    updateMsg.setId(msgId);
                    updateMsg.setContent(replyContent);
                    updateMsg.setType(msgType);

                    chatMessageMapper.updateById(updateMsg);
                    System.out.println("AI [" + bot.getAiName() + "] 回复更新成功");

                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("AI 执行严重错误: " + e.getMessage());
                    ChatMessage errorMSG = new ChatMessage();
                    errorMSG.setId(msgId);
                    errorMSG.setContent("程序出错: " + e.getMessage());
                    errorMSG.setType("AI");
                    chatMessageMapper.updateById(errorMSG);
                }
            });
        }
    }

    @Override
    public List<ChatMessage> getHistoryMessages(Long roomId) {
        // 查询聊天室消息
        List<ChatMessage> list = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getRoomId, roomId)
                        .orderByDesc(ChatMessage::getCreateTime)
                        .orderByDesc(ChatMessage::getId)
                        .last("LIMIT 50")
        );

        if (list == null || list.isEmpty()) {
            return list;
        }
        // 提取所有不为空的 replyToId
        List<Long> replyIds = list.stream()
                .map(ChatMessage::getReplyToId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (!replyIds.isEmpty()) {
            // 批量查询被引用的消息
            List<ChatMessage> replyMessages = chatMessageMapper.selectBatchIds(replyIds);
            Map<Long, ChatMessage> replyMap = replyMessages.stream()
                    .collect(Collectors.toMap(ChatMessage::getId, msg -> msg));
            for (ChatMessage msg : list) {
                if (msg.getReplyToId() != null && replyMap.containsKey(msg.getReplyToId())) {
                    msg.setReplyToMessage(replyMap.get(msg.getReplyToId()));
                }
            }
        }
        // 反转逻辑
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
    public void deleteRoomAi(Long aiId) { aiPersonaMapper.deleteById(aiId); }
    @Override
    public void deleteRoom(Long roomId) { chatRoomMapper.deleteById(roomId); }
    @Override
    public void updateRoomInfo(ChatRoom room) { chatRoomMapper.updateById(room); }
    @Override
    public void togglePinRoom(Long roomId) {
        ChatRoom room = chatRoomMapper.selectById(roomId);
        if(room != null){
            room.setIsPinned(!Boolean.TRUE.equals(room.getIsPinned()));
            chatRoomMapper.updateById(room);
        }
    }
    @Override
    public void deleteMessage(Long messageId) { chatMessageMapper.deleteById(messageId); }

    @Override
    public List<RoomAiPersona> getRoomAiList(Long roomId) {
        return aiPersonaMapper.selectList(
                new LambdaQueryWrapper<RoomAiPersona>()
                        .eq(RoomAiPersona::getRoomId, roomId)
                        .orderByDesc(RoomAiPersona::getIsPinned)
                        .orderByAsc(RoomAiPersona::getId)
        );
    }

    @Override
    public void updateRoomAi(RoomAiPersona aiPersona) {
        RoomAiPersona update = new RoomAiPersona();
        update.setId(aiPersona.getId());
        update.setAiName(aiPersona.getAiName());
        update.setSystemPrompt(aiPersona.getSystemPrompt());
        update.setApiKey(aiPersona.getApiKey());
        update.setModelName(aiPersona.getModelName());
        update.setAiAvatar(aiPersona.getAiAvatar());
        aiPersonaMapper.updateById(update);
    }

    @Override
    public void togglePinAi(Long aiId) {
        RoomAiPersona ai = aiPersonaMapper.selectById(aiId);
        if (ai != null) {
            boolean current = Boolean.TRUE.equals(ai.getIsPinned());
            ai.setIsPinned(!current);
            aiPersonaMapper.updateById(ai);
        }
    }
}