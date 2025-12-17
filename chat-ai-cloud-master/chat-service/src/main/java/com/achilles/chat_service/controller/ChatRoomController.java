package com.achilles.chat_service.controller;

import com.achilles.model.common.Result;
import com.achilles.chat_service.entity.ChatMessage;
import com.achilles.chat_service.entity.ChatRoom;
import com.achilles.chat_service.entity.RoomAiPersona;
import com.achilles.chat_service.service.api.ChatRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("room")
public class ChatRoomController {

    @Autowired
    private ChatRoomService chatRoomService;

    /**
     * 创建一个新房间
     * POST /room/create
     */
    @PostMapping("/create")
    public Result<ChatRoom> createRoom(@RequestBody ChatRoom room) {
        // 1. 获取当前登录用户 ID
        // 你的项目中应该有获取当前用户的方法，比如 Sa-Token 的 StpUtil.getLoginIdAsLong()
        // 或者从请求头获取，例如:
        Long userId = StpUtil.getLoginIdAsLong();
        // 2. 调用修改后的 Service (传入整个 room 对象和 userId)
        ChatRoom newRoom = chatRoomService.createRoom(room, userId);

        return Result.success(newRoom);
    }

    /**
     * 查看所有房间
     * GET /room/list
     */
    @GetMapping("/list")
    public Result<List<ChatRoom>> getRoomList() {
        return Result.success(chatRoomService.getRoomList());
    }

    /**
     * 发送一条消息
     * POST /room/send
     */
    @PostMapping("/send")
    public Result<Void> sendMessage(@RequestBody ChatMessage message) {
        // 这里的 message 是前端传过来的包裹
        chatRoomService.sendMessage(
                message.getRoomId(),
                message.getSenderId(),
                message.getSenderName(),
                message.getContent(),
                message.getType(),
                message.getReplyToId()
        );
        return Result.success();
    }

    /**
     * 获取某个房间的历史消息
     * 地址：GET /room/messages?roomId=1
     */
    @GetMapping("/messages")
    public Result<List<ChatMessage>> getHistoryMessages(@RequestParam("roomId") Long roomId) {
        return Result.success(chatRoomService.getHistoryMessages(roomId));
    }

    @PostMapping("/addAi")
    public Result<Void> addAiToRoom(@RequestBody RoomAiPersona aiPersona) {
        chatRoomService.addAiToRoom(
                aiPersona.getRoomId(),
                aiPersona.getAiName(),
                aiPersona.getSystemPrompt(),
                aiPersona.getApiKey(),
                aiPersona.getModelName()
        );
        return Result.success();
    }
    /**
     * 获取房间内的 AI 列表
     * 地址：GET /room/ai/list?roomId=1
     */
    @GetMapping("/ai/list")
    public Result<List<RoomAiPersona>> getRoomAiList(@RequestParam("roomId") Long roomId) {
        return Result.success(chatRoomService.getRoomAiList(roomId));
    }
    /**
     * 删除房间内的 AI
     * 地址：DELETE /room/ai/delete?aiId=1
     */
    @DeleteMapping("/ai/delete")
    public Result<Void> deleteRoomAi(@RequestParam("aiId") Long aiId) {
        chatRoomService.deleteRoomAi(aiId);
        return Result.success();
    }
    /**
     * 水龙头8：删除房间
     * 地址：DELETE /room/delete?roomId=1
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteRoom(@RequestParam("roomId") Long roomId) {
        chatRoomService.deleteRoom(roomId);
        return Result.success();
    }
    /**
     * 水龙头10：置顶/取消置顶
     */
    @PutMapping("/pin")
    public Result<Void> pinRoom(@RequestParam("roomId") Long roomId) {
        chatRoomService.togglePinRoom(roomId);
        return Result.success();
    }
    /**
     * 水龙头11：全能更新房间的信息
     */
    @PostMapping("/update")
    public Result<Void> updateRoom(@RequestBody ChatRoom room) {
        chatRoomService.updateRoomInfo(room);
        return Result.success();
    }

    @DeleteMapping("/message/delete")
    public Result<Void> deleteMessage(@RequestParam("msgId") Long msgId) {
        chatRoomService.deleteMessage(msgId);
        return Result.success();

    }
    /**
     * 更新 AI 信息 (设置)
     */
    @PostMapping("/ai/update")
    public Result<Void> updateRoomAi(@RequestBody RoomAiPersona aiPersona) {
        chatRoomService.updateRoomAi(aiPersona);
        return Result.success();
    }

    /**
     * 置顶/取消置顶 AI
     */
    @PutMapping("/ai/pin")
    public Result<Void> pinAi(@RequestParam("aiId") Long aiId) {
        chatRoomService.togglePinAi(aiId);
        return Result.success();
    }
}