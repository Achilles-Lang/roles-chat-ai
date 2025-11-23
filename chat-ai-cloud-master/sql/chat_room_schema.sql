-- AI多人聊天室数据库设计
-- 创建时间: 2024年

-- 1. AI角色表
CREATE TABLE `ai_character` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `name` varchar(100) NOT NULL COMMENT '角色名称',
    `description` text COMMENT '角色描述',
    `personality` text COMMENT '角色性格设定',
    `avatar_url` varchar(500) COMMENT '头像URL',
    `model_type` varchar(50) NOT NULL COMMENT 'AI模型类型: qwen/hunyuan/doubao',
    `model_config` json COMMENT '模型配置参数',
    `system_prompt` text COMMENT '系统提示词',
    `is_active` tinyint(1) DEFAULT 1 COMMENT '是否启用',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI角色表';

-- 2. 聊天室表
CREATE TABLE `chat_room` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '聊天室ID',
    `name` varchar(200) NOT NULL COMMENT '聊天室名称',
    `description` text COMMENT '聊天室描述',
    `avatar_url` varchar(500) COMMENT '聊天室头像URL',
    `room_type` varchar(20) DEFAULT 'public' COMMENT '聊天室类型: public/private',
    `owner_id` bigint(20) COMMENT '创建者ID',
    `max_members` int(11) DEFAULT 50 COMMENT '最大成员数',
    `is_active` tinyint(1) DEFAULT 1 COMMENT '是否启用',
    `settings` json COMMENT '聊天室设置',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_room_type` (`room_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天室表';

-- 3. 聊天室成员表
CREATE TABLE `chat_room_member` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '成员ID',
    `room_id` bigint(20) NOT NULL COMMENT '聊天室ID',
    `user_id` bigint(20) COMMENT '用户ID (可为NULL表示匿名用户)',
    `member_type` varchar(20) NOT NULL COMMENT '成员类型: human/ai',
    `character_id` bigint(20) COMMENT 'AI角色ID (当member_type=ai时)',
    `nickname` varchar(100) COMMENT '成员昵称',
    `avatar_url` varchar(500) COMMENT '成员头像',
    `join_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `last_active_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    `is_active` tinyint(1) DEFAULT 1 COMMENT '是否在聊天室中',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_user` (`room_id`, `user_id`, `member_type`, `character_id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_character_id` (`character_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天室成员表';

-- 4. 消息表
CREATE TABLE `chat_message` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `room_id` bigint(20) NOT NULL COMMENT '聊天室ID',
    `sender_type` varchar(20) NOT NULL COMMENT '发送者类型: human/ai/system',
    `sender_id` bigint(20) COMMENT '发送者ID (用户ID或AI角色ID)',
    `sender_name` varchar(100) NOT NULL COMMENT '发送者名称',
    `sender_avatar` varchar(500) COMMENT '发送者头像',
    `message_type` varchar(20) DEFAULT 'text' COMMENT '消息类型: text/image/file',
    `content` longtext NOT NULL COMMENT '消息内容',
    `extra_data` json COMMENT '额外数据',
    `reply_to_id` bigint(20) COMMENT '回复的消息ID',
    `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_room_id_time` (`room_id`, `created_time`),
    KEY `idx_sender` (`sender_type`, `sender_id`),
    KEY `idx_reply_to` (`reply_to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 5. 用户表 (扩展现有用户表)
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `session_id` varchar(100) COMMENT '会话ID (用于匿名用户)';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `last_active_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间';

-- 6. 预置AI角色数据
INSERT INTO `ai_character` (`name`, `description`, `personality`, `model_type`, `system_prompt`) VALUES
('小智', '友善的AI助手', '热情、乐于助人、富有同情心', 'qwen', '你是一个友善、热心的AI助手，总是帮助用户解答问题。'),
('博士', '知识渊博的学者', '冷静、理性、知识丰富', 'qwen', '你是一个博学的学者，说话严谨，逻辑清晰。'),
('小可爱', '活泼的AI朋友', '活泼、可爱、充满活力', 'qwen', '你是一个活泼可爱的小伙伴，说话轻松幽默。'),
('顾问', '专业的商业顾问', '专业、理性、经验丰富', 'qwen', '你是一个专业的商业顾问，提供专业的商业建议。');