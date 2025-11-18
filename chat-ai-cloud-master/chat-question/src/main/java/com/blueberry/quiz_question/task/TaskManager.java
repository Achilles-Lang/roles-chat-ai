package com.blueberry.quiz_question.task;

import com.blueberry.quiz_question.redis.RedisManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TaskManager {
    @Autowired
    private RedisManager redisTemplate;

    // 任务存储前缀
    private static final String TASK_KEY_PREFIX = "task:";

    /**
     * 创建任务（初始化状态）
     */
    public String createTask(String businessType) {
        String taskId = UUID.randomUUID().toString();
        Task task = new Task();
        task.setTaskId(taskId);
        task.setBusinessType(businessType);
        task.setStatus(TaskStatus.INIT);
        task.setCreateTime(System.currentTimeMillis());
        task.setUpdateTime(System.currentTimeMillis());

        // 存储任务（24小时过期）
        redisTemplate.setObject(
                TASK_KEY_PREFIX + taskId,
                task,
                24,
                TimeUnit.HOURS
        );
        return taskId;
    }

    /**
     * 更新任务状态
     */
    public void updateTaskStatus(String taskId, TaskStatus status, String businessId, String resultUrl, String errorMsg) {
        Task task = getTaskById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在：" + taskId);
        }

        task.setStatus(status);
        task.setBusinessId(businessId);
        task.setResultUrl(resultUrl);
        task.setErrorMsg(errorMsg);
        task.setUpdateTime(System.currentTimeMillis());

        // 更新存储
        redisTemplate.setObject(
                TASK_KEY_PREFIX + taskId,
                task,
                24,
                TimeUnit.HOURS
        );
    }

    /**
     * 根据ID查询任务
     */
    public Task getTaskById(String taskId) {
        return redisTemplate.getObject(TASK_KEY_PREFIX + taskId, Task.class);
    }
}
