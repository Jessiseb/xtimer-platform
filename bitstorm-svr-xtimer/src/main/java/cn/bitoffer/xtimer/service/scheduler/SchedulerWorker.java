package cn.bitoffer.xtimer.service.scheduler;

import cn.bitoffer.xtimer.common.conf.SchedulerAppConf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;

@Component
@Slf4j
public class SchedulerWorker {

    @Autowired
    SchedulerTask schedulerTask;

    @Autowired
    SchedulerAppConf schedulerAppConf;

    /**
     * 用于定期执行特定操作,,每过1000毫秒该方法会重新执行(1秒执行一次)
     */
    @Scheduled(fixedRate = 1000)
    public void scheduledTask() {
        log.info("任务执行时间：" + LocalDateTime.now());
        //调用handleSlices方法处理所有分片时间
        handleSlices();
    }
    
    private void handleSlices(){
        //遍历所有桶编号, 处理每个对应的分片
        for (int i = 0; i < schedulerAppConf.getBucketsNum(); i++) {
            handleSlice(i);
        }
    }

    /*
    * Q：为什么要重复执行一下前一分钟的批次？
    * A：主要是为了一个兜底重试机制，如果某一个分片执行失败了，那这种方式可以在下一分钟进行一次重试。
    * A：如果分片执行成功了，会延长分布式锁的时间，这样"重复执行前一分钟"批次这个逻辑也因为获取不到分布式
    * 锁而避免重复执行。如果分片本身执行失败了，则不会延长分布式锁时间，"重复执行前一分钟"则可以获取到分布式锁
    * 重试执行。
    * */
    private void handleSlice(int bucketId){
        //当前时间
        Date now = new Date();
        //当前时间的前一分钟-->重试机制
        Date nowPreMin = new Date(now.getTime() - 60000);
        try {
            //异步处理上一分钟批次的任务
            schedulerTask.asyncHandleSlice(nowPreMin,bucketId);
        }catch (Exception e){
            log.error("[handle slice] submit nowPreMin task failed, err:",e);
        }

        try {
            //异步处理当前批次的任务
            schedulerTask.asyncHandleSlice(now,bucketId);
        }catch (Exception e){
            log.error("[handle slice] submit now task failed, err:",e);
        }

    }
}
