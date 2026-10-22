package cn.bitoffer.mqtry.controller;

import cn.bitoffer.common.model.ResponseEntity;
import cn.bitoffer.mqtry.model.Task;
import cn.bitoffer.mqtry.service.CountService;
import cn.bitoffer.mqtry.service.TaskService;
import com.alibaba.nacos.shaded.com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * web服务接口：http 接口
 **/

@RestController
@RequestMapping("/demo")
@Slf4j
public class mqtryController {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private CountService countService;

    @Autowired
    private CountService countService2;

    @Autowired
    private CountService countService3;

    @Autowired
    private TaskService taskService;

    @PostMapping(value = "/peak_clipping", consumes = "application/json; charset=utf-8")
    public ResponseEntity<String> peakClipping(@RequestBody IncrCountReq data) {
        countService.flowArrived();
        return ResponseEntity.ok();
    }
    @PostMapping(value = "/peak_clipping_with_mq", consumes = "application/json; charset=utf-8")
    public ResponseEntity<String> peakClippingWithMQ(@RequestBody IncrCountReq data) {
        String msg = "coming!!!!!!!!!";
        kafkaTemplate.send("tp-mq-peakclipping",msg);
        return ResponseEntity.ok();
    }
    @PostMapping(value = "/decoupling", consumes = "application/json; charset=utf-8")
    public ResponseEntity<String> decoupling(@RequestBody IncrCountReq data) {
        countService.incrManyTimes(10000000);
        return ResponseEntity.ok();
    }

    @PostMapping(value = "/decoupling_with_mq", consumes = "application/json; charset=utf-8")
    public ResponseEntity<String> decouplingWithMQ(@RequestBody IncrCountReq data) {
        String msg = "coming!!!!!!!!!";
        kafkaTemplate.send("tp-mq-decoupling",msg);
        return ResponseEntity.ok();
    }
    @PostMapping(value = "/dispatch", consumes = "application/json; charset=utf-8")
    public ResponseEntity<String> dispatch(@RequestBody IncrCountReq data) {
        String msg = "to the moon!";
        countService.msgAll("svr1", msg);
        countService2.msgAll("svr2", msg);
        countService3.msgAll("svr3", msg);
        return ResponseEntity.ok();
    }

    @PostMapping(value = "/dispatch_with_mq", consumes = "application/json; charset=utf-8")
    public ResponseEntity<String> dispatchWithMQ(@RequestBody IncrCountReq data) {
        String msg = "to the moon!";
        kafkaTemplate.send("tp-mq-dispatch",msg);
        return ResponseEntity.ok();
    }


    @PostMapping(value = "/create_task", consumes = "application/json; charset=utf-8")
    public ResponseEntity<String> CreateTask(@RequestBody CreateTaskReq data) {
        String msg = "coming soon!!!!!!!!!";
        kafkaTemplate.send("tp-seckill",msg);
        return ResponseEntity.ok();
    }

    public static class IncrCountReq {
        public Integer num;
    }

    public static class CreateTaskReq {
    }
}
