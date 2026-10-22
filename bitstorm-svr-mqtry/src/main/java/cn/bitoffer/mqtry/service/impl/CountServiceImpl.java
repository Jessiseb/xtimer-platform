package cn.bitoffer.mqtry.service.impl;

import cn.bitoffer.common.redis.RedisBase;
import cn.bitoffer.mqtry.mapper.CountMapper;
import cn.bitoffer.mqtry.model.Count;
import cn.bitoffer.mqtry.service.CountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CountServiceImpl implements CountService {
    public static Integer tmpCount = 0;
    public static final Lock lock = new ReentrantLock();

    @Autowired
    private CountMapper countMapper;

    @Autowired
    private RedisBase redisBase;



    @Override
    public void save(Count example) {
        countMapper.save(example);
    }

    @Override
    public void msgAll(String svrName, String msg) {
        System.out.println("svr " + svrName + " recevied msg : " + msg);
    }

    @Override
    public void incrCount(Integer num) {
        countMapper.incrCount(num);
    }

    @Override
    public void flowArrived() {
        Date d = new Date();
        System.out.println(d.toString() + " flow arrived!!!");
    }

    @Override
    public void incrManyTimes(Integer num) {
        for (int i = 0; i < num; i++) {
            incrCountAtomic(num);
        }
    }

    private  void clearMemCount() {
        tmpCount = 0;
    }

    public void incrCountAtomic(Integer num) {
        lock.lock();
        tmpCount++;
        lock.unlock();
    }

}
