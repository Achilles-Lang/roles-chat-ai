package com.blueberry.quiz_user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user")
public class UserTO {
    @TableId(type = IdType.AUTO)
    private long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
}
