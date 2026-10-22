package cn.bitoffer.xtimer.service.scheduler;

import cn.bitoffer.xtimer.common.conf.SchedulerAppConf;
import cn.bitoffer.common.redis.ReentrantDistributeLock;
import cn.bitoffer.xtimer.service.trigger.TriggerWorker;
import cn.bitoffer.xtimer.utils.TimerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class SchedulerTask {

    @Autowired
    ReentrantDistributeLock reentrantDistributeLock;

    @Autowired
    SchedulerAppConf schedulerAppConf;

    @Autowired
    TriggerWorker triggerWorker;

    /**
     * 异步处理时间片
     * @param date 当前时间
     * @param bucketId 桶号
     */
    @Async("schedulerPool")
    public void asyncHandleSlice(Date date,int bucketId) {
        log.info("start executeAsync");

        //生成一个唯一的锁标识符
        String lockToken = TimerUtils.GetTokenStr();

        // 只加锁不解锁，只有超时解锁；超时时间控制频率；
        // 锁住横纵向切分后的：单个桶（分钟+bucketIndex）
        // 尝试获取分布式锁，防止同一时间片的多个任务并发执行
        // 参数1: lockKey - 通过 TimerUtils.GetTimeBucketLockKey 生成的时间桶锁键，格式为 "time_bucket_lock_{yyyy-MM-dd HH:mm}_{bucketId}"
        // 参数2: lockToken - 本次加锁的唯一标识符，通过 TimerUtils.GetTokenStr() 生成，用于确保锁的持有者可以续期或释放锁
        // 参数3: expireSeconds - 锁的过期时间（单位：秒），从 schedulerAppConf.getTryLockSeconds() 获取，用于控制锁的超时时间，避免死锁
        boolean ok = reentrantDistributeLock.lock(
                TimerUtils.GetTimeBucketLockKey(date, bucketId),
                lockToken,
                schedulerAppConf.getTryLockSeconds()); // 抢到锁之后设置锁的过期时间为配置的 70 秒

        if(!ok){
            log.info("asyncHandleSlice 获取分布式锁失败");
            return;
        }

        log.info("get scheduler lock success, key: %s", TimerUtils.GetTimeBucketLockKey(date, bucketId));

        // 注释: 调用trigger进行处理--->也是异步处理
        //触发worker工作, 处理待定时间片的消息
        //TimerUtils.GetSliceMsgKey(date,bucketId):
        triggerWorker.work(TimerUtils.GetSliceMsgKey(date,bucketId));

        // 延长分布式锁的时间,避免重复执行分片
        reentrantDistributeLock.expireLock(
                TimerUtils.GetTimeBucketLockKey(date,bucketId),
                lockToken,
                schedulerAppConf.getSuccessExpireSeconds());

        log.info("end executeAsync");
    }
}
