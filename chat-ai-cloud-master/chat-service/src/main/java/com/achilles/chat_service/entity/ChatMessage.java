package com.achilles.chat_service.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息实体类
 */
@TableName(value = "chat_message",autoResultMap = true)
public class ChatMessage {
    @TableField(exist = false)
    private List<Map<String, Object>> reactions;
    @TableId(type = IdType.AUTO)
    // 主键
    private Long id;
    // 房间ID
    private Long roomId;
    // 发送人ID
    private Long senderId;
    // 发送者昵称（如果是AI，这里就是"AI助手"）
    private String senderName;
    // 消息内容
    private String content;
    // 消息类型 (TEXT, SYSTEM, AI)
    private String type;
    // 发送时间
    private LocalDateTime createTime;
    // 回复哪条消息
    private Long replyToId;
    @TableField(exist = false)
    private ChatMessage replyToMessage;
    // 是否删除
    @TableLogic
    private Integer isDeleted;
    @TableField(value = "extra_data", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraData;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public List<Map<String, Object>> getReactions() {
        return reactions;
    }

    public void setReactions(List<Map<String, Object>> reactions) {
        this.reactions = reactions;
    }

    public Long getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(Long replyToId) {
        this.replyToId = replyToId;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public ChatMessage getReplyToMessage() {
        return replyToMessage;
    }

    public void setReplyToMessage(ChatMessage replyToMessage) {
        this.replyToMessage = replyToMessage;
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }
}