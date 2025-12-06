package com.blueberry.quiz_question.controller;

import com.blueberry.model.common.Result;
import com.blueberry.quiz_question.entity.ChatMessage;
import com.blueberry.quiz_question.entity.ChatRoom;
import com.blueberry.quiz_question.entity.RoomAiPersona;
import com.blueberry.quiz_question.service.api.ChatRoomService;
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
    public Result<ChatRoom> createRoom(@RequestParam("name") String name,
                                       @RequestParam("userId") Long userId) {
        ChatRoom room = chatRoomService.createRoom(name, userId);
        return Result.success(room);
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
                message.getType()
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
                aiPersona.getApiKey(),    // 传这俩新参数
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
}