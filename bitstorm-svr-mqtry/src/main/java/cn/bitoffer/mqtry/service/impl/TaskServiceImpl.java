package cn.bitoffer.mqtry.service.impl;

import cn.bitoffer.common.redis.RedisBase;
import cn.bitoffer.mqtry.mapper.TaskMapper;
import cn.bitoffer.mqtry.model.Task;
import cn.bitoffer.mqtry.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class TaskServiceImpl implements TaskService {
    public static Integer tmpTask = 0;
    public static final Lock lock = new ReentrantLock();

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public void save(Task task) {
        taskMapper.save(task);
    }
}
