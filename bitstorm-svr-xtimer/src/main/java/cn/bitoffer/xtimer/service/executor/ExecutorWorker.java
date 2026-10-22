package cn.bitoffer.xtimer.service.executor;

import cn.bitoffer.xtimer.exception.ErrorCode;
import cn.bitoffer.api.dto.xtimer.NotifyHTTPParam;
import cn.bitoffer.xtimer.enums.TaskStatus;
import cn.bitoffer.xtimer.enums.TimerStatus;
import cn.bitoffer.xtimer.exception.BusinessException;
import cn.bitoffer.xtimer.mapper.TaskMapper;
import cn.bitoffer.xtimer.mapper.TimerMapper;
import cn.bitoffer.xtimer.model.TaskModel;
import cn.bitoffer.xtimer.model.TimerModel;
import cn.bitoffer.xtimer.utils.TimerUtils;
import cn.bitoffer.api.dto.xtimer.TimerDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class ExecutorWorker {

    @Autowired
    TaskMapper taskMapper;

    @Autowired
    TimerMapper timerMapper;

    /**
     * 处理 定时任务的执行逻辑
     * @param timerIDUnixKey 组合格式的定时器标识符字符串, 包含timerId和unix时间戳
     *                       格式: timerId_unixTimestamp
     */
    public void work(String timerIDUnixKey){
//        分解并验证timerIDUnixKey的合法性
        List<Long> longSet = TimerUtils.SplitTimerIDUnix(timerIDUnixKey);
        if(longSet.size() != 2){
            log.error("splitTimerIDUnix 错误, timerIDUnix:"+timerIDUnixKey);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"splitTimerIDUnix 错误, timerIDUnix:"+timerIDUnixKey);
        }
        // 获取分解后的定时器ID和unix时间戳
        Long timerId = longSet.get(0);
        Long unix = longSet.get(1);

        //查询出任务，判断是否执行过了。避免重复执行
        TaskModel task = taskMapper.getTasksByTimerIdUnix(timerId,unix);
        if(task.getStatus() != TaskStatus.NotRun.getStatus()){
            log.warn("重复执行任务： timerId"+timerId+",runtimer:"+unix);
            return;
        }

        // 执行回调
        executeAndPostProcess(task,timerId,unix);
    }

    private void executeAndPostProcess(TaskModel taskModel,Long timerId, Long unix){

        // 1.查询 timerModel
        TimerModel timerModel = timerMapper.getTimerById(timerId);
        if(timerModel == null){
            log.error("执行回调错误，找不到对应的Timer。 timerId"+timerId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"执行回调错误，找不到对应的Timer。 timerId"+timerId);
        }

        // 2.判断定时器是否还处于激活状态，如果时去激活状态，就不回调了。（用户主动关闭了闹钟）
        if(timerModel.getStatus() != TimerStatus.Enable.getStatus()){
            log.warn("Timer已经处于去激活状态。 timerId"+timerId);
            return;
        }

        // 触发时间的误差时间
        int gapTime = (int) (new Date().getTime() - taskModel.getRunTimer());
        taskModel.setCostTime(gapTime);

        // 执行http回调，通知业务方
        ResponseEntity<String> resp = null;
        try{
            resp = executeTimerCallBack(timerModel);
        }catch (Exception e){
            log.error("执行回调失败，抛出异常e:"+e);
        }

        //后置处理，更新Timer执行结果
        if(resp == null){
            taskModel.setStatus(TaskStatus.Failed.getStatus());
            taskModel.setOutput("resp is null");
        } else if(resp.getStatusCode().is2xxSuccessful()){
            taskModel.setStatus(TaskStatus.Succeed.getStatus());
            taskModel.setOutput(resp.toString());
        }else{
            taskModel.setStatus(TaskStatus.Failed.getStatus());
            taskModel.setOutput(resp.toString());
        }

        taskMapper.update(taskModel);
    }

    /**
     * 执行定时器的回调请求(http回调)
     * @param timerModel
     * @return
     */
    private ResponseEntity<String> executeTimerCallBack(TimerModel timerModel){
        TimerDTO timerDTO = TimerModel.objToVo(timerModel);
        // 获取HTTP回调参数
        NotifyHTTPParam httpParam = timerDTO.getNotifyHTTPParam();
        // 创建RestTemplate对象用于发送HTTP请求
        RestTemplate restTemplate = new RestTemplate();
        // 初始化响应对象
        ResponseEntity<String> resp = null;

        // 根据HTTP请求方法执行相应的请求
        switch (httpParam.getMethod()){
            case "POST":
                resp = restTemplate.postForEntity(httpParam.getUrl(), httpParam.getBody(),String.class);
            default:
                log.error("不支持的httpMethod");
                break;
        }

        StringBuffer sb = new StringBuffer();
        // 检查HTTP响应状态码
        HttpStatus statusCode = resp.getStatusCode();
        if (!statusCode.is2xxSuccessful()){
            // 如果响应状态码不是2xx，记录错误日志
            log.error("http 回调失败："+resp);
        }
        // 返回HTTP响应
        return resp;
    }
}
