package cn.bitoffer.mqtry.schedued;

import cn.bitoffer.mqtry.mapper.CountMapper;
import cn.bitoffer.mqtry.service.CountService;
import cn.bitoffer.mqtry.service.impl.CountServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时任务模版
 *
 **/
@Component
public class ScheduledTask {
    @Autowired
    private CountService service;
    @Scheduled(fixedRate = 3000)
    public void scheduledTask() {
        //service.incrMemCountAndReset();
        //System.out.println("任务执行时间 isis：" + LocalDateTime.now());
    }

}
