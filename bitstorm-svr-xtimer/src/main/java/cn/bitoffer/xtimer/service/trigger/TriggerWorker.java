package cn.bitoffer.xtimer.service.trigger;

import cn.bitoffer.xtimer.common.conf.TriggerAppConf;
import cn.bitoffer.xtimer.exception.BusinessException;
import cn.bitoffer.xtimer.mapper.TaskMapper;
import cn.bitoffer.xtimer.redis.TaskCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

@Component
@Slf4j
public class TriggerWorker {

    @Autowired
    TriggerAppConf triggerAppConf;

    @Autowired
    TriggerPoolTask triggerPoolTask;

    @Autowired
    TaskCache taskCache;

    @Autowired
    TaskMapper taskMapper;

    /**
     * 执行一个定时任务, 该任务会在指定的时间范围内执行zrange操作------>分片
     * @param minuteBucketKey 用于标识一分钟时间桶的建,用于执行zrange操作
     */
    public void work(String minuteBucketKey){
        // 进行为时一分钟的zrange
        Date startTime = getStartMinute(minuteBucketKey);
        Date endTime = new Date(startTime.getTime() + 60000);

        //初始化计数器,用于等待任务执行完成
        CountDownLatch latch = new CountDownLatch(1);
        //创建定时器对象
        Timer timer = new Timer("Timer");
        //创建定时任务对象, 该任务会执行zrange操作
        TriggerTimerTask task = new TriggerTimerTask(
                triggerAppConf,triggerPoolTask,taskCache,taskMapper,latch,startTime,endTime,minuteBucketKey);

        //定时任务以固定的速率执行zrange操作
        //此处会调用TriggerTimerTask里的一份run方法, run方法又会调用一个方法来
        //每隔1s来进行高频扫描
        timer.scheduleAtFixedRate(task, 0L, triggerAppConf.getZrangeGapSeconds()*1000L);
        try {
            //等待任务执行完成
            latch.await();
        } catch (InterruptedException e) {
            log.error("执行TriggerTimerTask异常中断，task:"+task);
        }finally{
            //取消定时器
            timer.cancel();
        }
    }

    /**
     * 根据分钟桶的健获取开始时间
     * @param minuteBucketKey yyyy-MM-dd-HH:mm_yyyy-MM-dd-HH:mm-->表示时间范围
     * @return
     */
    private Date getStartMinute(String minuteBucketKey){
       //分割分钟桶建, 以获取开始时间和结束时间字符串
        String[] timeBucket = minuteBucketKey.split("_");
       //yyyy-MM-dd-HH:mm_yyyy-MM-dd-HH:mm ->不符合这个格式就错
        if(timeBucket.length != 2){
            log.error("TriggerWorker getStartMinute 错误");
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date startMinute = null;
        try {
            //解析开始时间字符串
            startMinute = sdf.parse(timeBucket[0]);
        } catch (ParseException e) {
            log.error("TriggerWorker getStartMinute 错误");
        }
        return startMinute;
    }
}
