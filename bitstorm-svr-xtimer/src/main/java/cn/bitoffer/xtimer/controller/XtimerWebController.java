package cn.bitoffer.xtimer.controller;

import cn.bitoffer.api.dto.xtimer.TimerDTO;
import cn.bitoffer.common.model.ResponseEntity;
import cn.bitoffer.xtimer.service.XTimerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@RestController
@RequestMapping("/xtimer")
@Slf4j
public class XtimerWebController {

    @Resource
    private XTimerService xTimerService;

    /**
     * 创建定时器接口
     * 通过POST请求接收TimerDTO对象，其中包含创建定时器所需的信息
     * 成功创建后，返回新创建定时器的ID
     *
     * @param timerDTO 定时器数据传输对象，包含创建定时器的必要数据
     * @return 返回新创建定时器的ID，使用HTTP状态码200表示请求成功
     */
    @PostMapping(value = "/createTimer")
    public ResponseEntity<Long> createTimer(@RequestBody TimerDTO timerDTO){
        //创建定时器并获得id
        Long timerId = xTimerService.CreateTimer(timerDTO);
        return ResponseEntity.ok(timerId);
    }

    /**
     * 启用定时器接口
     * 该接口允许通过指定应用和定时器ID来启用一个定时器
     *
     * @param app 应用标识符，用于区分不同的应用
     * @param timerId 定时器的唯一标识符
     * @param headers 请求头，包含额外的请求信息
     * @return 返回一个响应实体，表示操作结果
     */
    @GetMapping(value = "/enableTimer")
    public ResponseEntity<String> enableTimer(@RequestParam(value = "app") String app,
                            @RequestParam(value = "timerId") Long timerId,
                            @RequestHeader MultiValueMap<String, String> headers){
        xTimerService.EnableTimer(app,timerId);
        return ResponseEntity.ok("ok");
    }

    /**
     * 查询所有定时任务
     */
    @GetMapping(value = "/getAppTimers")
    public ResponseEntity<List<TimerDTO>> getAllTimers(@RequestParam(value = "app") String app){
        List<TimerDTO> timerDTOList = xTimerService.GetAppTimers(app);
        return ResponseEntity.ok(timerDTOList);
    }
}
