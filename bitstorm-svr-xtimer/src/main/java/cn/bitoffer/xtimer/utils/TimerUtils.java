package cn.bitoffer.xtimer.utils;

import org.quartz.CronExpression;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TimerUtils {
    public static String GetCreateLockKey(String app) {
        return "create_timer_lock_" + app;
    }

    public static String GetEnableLockKey(String app) {
        return "enable_timer_lock_" + app;
    }

    public static String GetMigratorLockKey(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        String dateStr = sdf.format(date);
        return "migrator_lock_" + dateStr;
    }

    public static String GetTimeBucketLockKey(Date time, int bucketId) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String timeStr = sdf.format(time);
        return sb.append("time_bucket_lock_").append(timeStr).append("_").append(bucketId).toString();
    }

    /**
     * 生成时间片消息建
     * @param time
     * @param bucketId
     * @return
     */
    public static String GetSliceMsgKey(Date time, int bucketId) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String timeStr = sdf.format(time);
        return sb.append(timeStr).append("_").append(bucketId).toString();
    }

    public static String GetTokenStr() {
        long timestamp = System.currentTimeMillis();
        String thread = Thread.currentThread().getName();
        return thread + timestamp;
    }

    public static Date GetForwardTwoMigrateStepEnd(Date start, int diffMinutes) {
        Date end = new Date(start.getTime() + 2L * diffMinutes * 60000);
        return end;
    }

    /**
     * 在指定的时间范围内获取 Cron 表达式的下一个有效时间列表
     * 此方法用于计算在给定的开始时间和结束时间之间，Cron 表达式的下一个有效触发时间
     * 如果结束时间早于开始时间，则返回空列表
     * 主要用于调度和定时任务场景，以确定任务在未来特定时间段内的触发时间
     *
     * @param cronExpression Cron 表达式，定义了调度规则
     * @param now            开始时间，表示 Cron 表达式开始生效的时间
     * @param end            结束时间，表示 Cron 表达式停止生效的时间
     * @return 包含在指定时间内所有下一个有效触发时间的时间戳列表如果结束时间早于开始时间，返回空列表
     */
    public static List<Long> GetCronNextsBetween(CronExpression cronExpression, Date now, Date end) {
        List<Long> times = new ArrayList<>();
        if (end.before(now)) {
            return times;
        }
        // 从开始时间开始, 直到达到或超过结束时间
        for (Date start = now; start.before(end); ) {
            // 获取当前时间点之后的下一个有效Cron表达式
            Date next = cronExpression.getNextValidTimeAfter(start);
            // 将下一个有效时间的时间戳添加到列表中
            times.add(next.getTime());
            // 更新当前时间为下一个有效时间，继续循环
            start = next;
        }
        // 返回包含所有下一个有效触发时间的时间戳列表
        return times;
    }

    public static String UnionTimerIDUnix(long timerId, long unix) {
        return new StringBuilder().append(timerId).append("_").append(unix).toString();
    }

    /**
     * 将Unix时间格式的定时器ID字符串转换为两个长时间之的列表
     * 定时器ID字符串 --> "长时间值_长时间值"
     * @param timerIDUnix timerIDUnix Unix时间格式的定时器ID字符串
     * @return
     */
    public static List<Long> SplitTimerIDUnix(String timerIDUnix) {
        List<Long> longSet = new ArrayList<>();
        String[] strList = timerIDUnix.split("_");
        if (strList.length != 2) {
            return longSet;
        }
        // 将分割后的第一个字符串转换为长时间值并添加到列表中
        longSet.add(Long.parseLong(strList[0]));
        // 将分割后的第二个字符串转换为长时间值并添加到列表中
        longSet.add(Long.parseLong(strList[1]));
        return longSet;
    }
}
