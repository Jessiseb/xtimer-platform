package cn.bitoffer.xtimer.redis;

import cn.bitoffer.xtimer.exception.ErrorCode;
import cn.bitoffer.xtimer.common.conf.SchedulerAppConf;
import cn.bitoffer.xtimer.exception.BusinessException;
import cn.bitoffer.xtimer.model.TaskModel;
import cn.bitoffer.xtimer.utils.TimerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class TaskCache {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    SchedulerAppConf schedulerAppConf;

    public String GetTableName(TaskModel taskModel){
        int maxBucket = schedulerAppConf.getBucketsNum();

        StringBuilder sb = new StringBuilder();

        //创建日期格式化对象, 用于格式化任务运行时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        //将任务运行时间格式化为字符串
        String timeStr = sdf.format(new Date(taskModel.getRunTimer()));

        //计算任务计时器ID对最大桶数的模, 用于分配任务到不同的桶
        long index = taskModel.getTimerId()%maxBucket;

        //拼接名称并返回 (运行时间＋桶号)
        return sb.append(timeStr).append("_").append(index).toString();
    }

    /**
     * 将任务模型列表缓存到Redis中
     * @param taskList 任务模型列表
     * @return 是否成功
     */
    public boolean cacheSaveTasks(List<TaskModel> taskList){

        try {
            //定义一个SessionCallback匿名内部类, 用于执行Redis事务操作
            SessionCallback sessionCallback = new SessionCallback() {
                @Override
                public Object execute(RedisOperations redisOperations) throws DataAccessException {
                    //开启redis事务
                    redisOperations.multi();
                    //遍历任务列表
                    for (TaskModel task : taskList) {
                        //获取到任务的执行时间的  --- >时间戳
                        long unix = task.getRunTimer();
                        //获取到任务信息对应的表名--->时间戳＋桶号
                        String tableName = GetTableName(task);
                        //将任务的定时器ID与运行时间戳合并, 作为有序集合中的成员,分数运行时间戳
                        redisTemplate.opsForZSet().add(
                                tableName, //key(时间＋桶号)
                                TimerUtils.UnionTimerIDUnix(task.getTimerId(), unix), //value(timerId+时间戳)
                                unix); //score(时间戳)
                        //key是集合的名字, value-score就是集合中的key-value
                    }
                    //执行事务
                    return redisOperations.exec(); //2023-11-06 21:54_1
                }
            };
            //执行Redis事务操作
            redisTemplate.execute(sessionCallback);
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 从Redis ZSET缓存中获取指定时间范围内的定时任务数据
     * @param key Redis存储的ZSET键名
     * @param start 查询时间范围起始时间戳
     * @param end   查询时间范围结束时间戳
     * @return
     */
    public List<TaskModel> getTasksFromCache(String key,long start, long end){
        List<TaskModel> tasks = new ArrayList<>();

        // 从ZSET 获取1秒范围的任务；
        Set<Object> timerIDUnixs = redisTemplate.opsForZSet().rangeByScore(key,start,end-1);
        if(CollectionUtils.isEmpty(timerIDUnixs)){
            return tasks;
        }

        //遍历每个定时任务数据
        for (Object timerIDUnixObj:timerIDUnixs) {
            TaskModel task = new TaskModel();
            String timerIDUnix = (String) timerIDUnixObj;


            List<Long> longSet = TimerUtils.SplitTimerIDUnix(timerIDUnix);
            if(longSet.size() != 2){
                log.error("splitTimerIDUnix 错误, timerIDUnix:"+timerIDUnix);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"splitTimerIDUnix 错误, timerIDUnix:"+timerIDUnix);
            }
            task.setTimerId(longSet.get(0));
            task.setRunTimer(longSet.get(1));
            tasks.add(task);
        }

        return tasks;
    }

}
