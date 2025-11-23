package com.blueberry.quiz_question.controller;

import com.blueberry.model.common.Result;
import com.blueberry.quiz_question.entity.ChatMessage;
import com.blueberry.quiz_question.entity.ChatRoom;
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
     * 水龙头1：创建一个新房间
     * 地址：POST /room/create
     */
    @PostMapping("/create")
    public Result<ChatRoom> createRoom(@RequestParam("name") String name,
                                       @RequestParam("userId") Long userId) {
        ChatRoom room = chatRoomService.createRoom(name, userId);
        return Result.success(room);
    }

    /**
     * 水龙头2：查看所有房间
     * 地址：GET /room/list
     */
    @GetMapping("/list")
    public Result<List<ChatRoom>> getRoomList() {
        return Result.success(chatRoomService.getRoomList());
    }

    /**
     * 水龙头3：发送一条消息
     * 地址：POST /room/send
     */
    @PostMapping("/send")
    public Result<Void> sendMessage(@RequestBody ChatMessage message) {
        // 这里的 message 是前端传过来的包裹
        chatRoomService.sendMessage(
                message.getRoomId(),
                message.getSenderId(),
                message.getSenderName(),
                message.getContent()
        );
        return Result.success();
    }

    /**
     * 水龙头4：获取某个房间的历史消息
     * 地址：GET /room/messages?roomId=1
     */
    @GetMapping("/messages")
    public Result<List<ChatMessage>> getHistoryMessages(@RequestParam("roomId") Long roomId) {
        return Result.success(chatRoomService.getHistoryMessages(roomId));
    }
}