package com.achilles.chat_service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.achilles.chat_service.ai.ChatAIClient;
import com.achilles.chat_service.entity.ChatMessage;
import com.achilles.chat_service.entity.ChatRoom;
import com.achilles.chat_service.entity.RoomAiPersona;
import com.achilles.chat_service.mapper.ChatMessageMapper;
import com.achilles.chat_service.mapper.ChatRoomMapper;
import com.achilles.chat_service.mapper.RoomAiPersonaMapper;
import com.achilles.chat_service.service.api.ChatRoomService;
import com.achilles.model.common.Result;
import jakarta.annotation.Resource;
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

    @Resource
    private com.achilles.chat_service.ai.KnowledgeClient knowledgeClient;
    @Override
    public ChatRoom createRoom(ChatRoom roomData, Long creatorId) {
        ChatRoom room = new ChatRoom();
        // 必填项
        room.setRoomName(roomData.getRoomName());
        room.setCreatorId(creatorId);
        room.setCreateTime(LocalDateTime.now());

        // 选填项 (新增)
        room.setDescription(roomData.getDescription());
        room.setAvatar(roomData.getAvatar());

        // 默认非置顶
        room.setIsPinned(false);

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
            triggerAIReply(roomId, userMsg);
        }
    }

    /**
     * 召唤 AI
     *
     */
    /**
     * 修改后的 AI 回复触发逻辑
     * @param roomId 房间ID
     * @param lastUserMsg 用户刚刚发送的那条消息对象 (不要只传 String)
     */
    private void triggerAIReply(Long roomId, ChatMessage lastUserMsg) {
        // 1. 获取用户内容
        String userContent = lastUserMsg.getContent();

        System.out.println("开始为房间 " + roomId + " 寻找 AI 角色...");
        List<RoomAiPersona> bots = aiPersonaMapper.selectList(
                new LambdaQueryWrapper<RoomAiPersona>().eq(RoomAiPersona::getRoomId, roomId)
        );

        if (bots.isEmpty()) {
            return;
        }

        // 2. 准备历史记录 (数据库倒序查 Limit 10 -> 内存正序排列)
        List<ChatMessage> historyList = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getRoomId, roomId)
                        .orderByDesc(ChatMessage::getCreateTime)
                        .last("LIMIT 10")
        );

        // 翻转顺序，变成：旧 -> 新
        Collections.reverse(historyList);

        StringBuilder historyBuilder = new StringBuilder();
        for (ChatMessage msg : historyList) {
            // 跳过当前的最新消息，避免重复
            if (msg.getId().equals(lastUserMsg.getId())) {
                continue;
            }
            historyBuilder.append(msg.getSenderName()).append(": ").append(msg.getContent()).append("\n");
        }

        // 3. 构建基础对话上下文 (历史 + 当前用户发言)
        String basePrompt = "【历史聊天记录】:\n" + historyBuilder.toString() +
                "\n【用户最新发言】:\n" + userContent +
                "\n【请接着话茬回复】";

        // 4. 遍历机器人进行回复
        for (RoomAiPersona bot : bots) {

            // === 主线程：立刻插入“思考中” ===
            ChatMessage thinkingMSG = new ChatMessage();
            thinkingMSG.setRoomId(roomId);
            thinkingMSG.setSenderId(0L); // 0 代表 AI
            thinkingMSG.setSenderName(bot.getAiName());
            thinkingMSG.setContent("思考中...");
            thinkingMSG.setType("THINKING");
            thinkingMSG.setCreateTime(LocalDateTime.now());
            thinkingMSG.setIsDeleted(0);
            chatMessageMapper.insert(thinkingMSG);

            Long msgId = thinkingMSG.getId();

            // === 异步线程：调用 AI ===
            CompletableFuture.runAsync(() -> {
                try {
                    String replyContent = "";
                    String msgType = "AI";

                    // A. 获取 API Key
                    String apiKey = bot.getApiKey();

                    // B. 判断是否为绘画模型
                    if (bot.getModelName() != null && bot.getModelName().toLowerCase().startsWith("wanx")) {
                        // --- 绘画逻辑 ---
                        replyContent = aiClient.generateImage(apiKey, userContent);
                        if (replyContent != null && replyContent.startsWith("http")) {
                            msgType = "IMAGE";
                        }
                    } else {
                        // --- 对话逻辑 (含 RAG) ---

                        // 1. 查知识库 (RAG)
                        String referenceContext = "";
                        try {
                            Result<List<String>> searchResult = knowledgeClient.search(userContent);
                            if (searchResult != null && searchResult.getData() != null && !searchResult.getData().isEmpty()) {
                                referenceContext = String.join("\n\n", searchResult.getData());
                                System.out.println(">>> [RAG] 检索到知识: " + referenceContext.substring(0, Math.min(referenceContext.length(), 30)) + "...");
                            }
                        } catch (Exception e) {
                            System.err.println(">>> [RAG] 检索失败: " + e.getMessage());
                        }

                        // 2. 拼接最终 Prompt (参考资料 + 基础对话)
                        StringBuilder finalPrompt = new StringBuilder();
                        if (!referenceContext.isEmpty()) {
                            finalPrompt.append("【参考资料】(请优先基于此资料回答):\n")
                                    .append(referenceContext)
                                    .append("\n\n----------------\n\n");
                        }
                        finalPrompt.append(basePrompt);

                        // 3. 调用 AI (使用 chatDynamic)
                        replyContent = aiClient.chatDynamic(
                                apiKey,
                                bot.getModelName(),
                                bot.getSystemPrompt(),
                                finalPrompt.toString() // 传入包含知识库+历史的完整内容
                        );
                    }

                    // C. 校验结果
                    if (replyContent == null || replyContent.trim().isEmpty()) {
                        replyContent = "⚠️ AI 无法回复：接口返回空。";
                    }

                    // D. 更新数据库消息
                    ChatMessage updateMsg = new ChatMessage();
                    updateMsg.setId(msgId);
                    updateMsg.setContent(replyContent);
                    updateMsg.setType(msgType);
                    chatMessageMapper.updateById(updateMsg);

                    System.out.println("AI [" + bot.getAiName() + "] 回复成功");

                } catch (Exception e) {
                    e.printStackTrace();
                    // 发生异常，把“思考中”改成错误提示
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