package cn.bitoffer.mqtry.kafka.consumer;

import cn.bitoffer.mqtry.service.CountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TopicConsumer {
    @Autowired
    private CountService couontSerivce;
    @KafkaListener(topics = "tp-mq-decoupling", groupId = "TEST_GROUP",concurrency = "1", containerFactory = "kafkaManualAckListenerContainerFactory")
    public void topic_test(ConsumerRecord<?, ?> record, Acknowledgment ack, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        Optional message = Optional.ofNullable(record.value());
        if (message.isPresent()) {
            Object msg = message.get();
            System.out.println("收到Kafka消息! Topic:" + topic + ",Message:" + msg);
            try {
                couontSerivce.incrManyTimes(10000000);
                ack.acknowledge();
                log.info("Kafka消费成功! Topic:" + topic + ",Message:" + msg);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Kafka消费失败！Topic:" + topic + ",Message:" + msg, e);
            }
        }
    }

    @KafkaListener(topics = "tp-mq-peakclipping", groupId = "TEST_GROUP",concurrency = "1", containerFactory = "kafkaManualAckListenerContainerFactory")
    public void peakClipping(ConsumerRecord<?, ?> record, Acknowledgment ack, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        Optional message = Optional.ofNullable(record.value());
        if (message.isPresent()) {
            Object msg = message.get();
            System.out.println("收到Kafka消息! Topic:" + topic + ",Message:" + msg);
            try {
                couontSerivce.flowArrived();
                TimeUnit.SECONDS.sleep(1);
                ack.acknowledge();
                log.info("Kafka消费成功! Topic:" + topic + ",Message:" + msg);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Kafka消费失败！Topic:" + topic + ",Message:" + msg, e);
            }
        }
    }

    @KafkaListener(topics = "tp-mq-dispatch", groupId = "TEST_GROUP1",concurrency = "1", containerFactory = "kafkaManualAckListenerContainerFactory")
    public void dispatchForSvr1(ConsumerRecord<?, ?> record, Acknowledgment ack, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        Optional message = Optional.ofNullable(record.value());
        if (message.isPresent()) {
            Object msg = message.get();
            System.out.println("svr1 收到Kafka消息! Topic:" + topic + ",Message:" + msg);
            try {
                couontSerivce.incrManyTimes(10000);
                ack.acknowledge();
                log.info("Kafka消费成功! Topic:" + topic + ",Message:" + msg);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Kafka消费失败！Topic:" + topic + ",Message:" + msg, e);
            }
        }
    }

    @KafkaListener(topics = "tp-mq-dispatch", groupId = "TEST_GROUP2",concurrency = "1", containerFactory = "kafkaManualAckListenerContainerFactory")
    public void dispatchForSvr2(ConsumerRecord<?, ?> record, Acknowledgment ack, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        Optional message = Optional.ofNullable(record.value());
        if (message.isPresent()) {
            Object msg = message.get();
            System.out.println("svr2 收到Kafka消息! Topic:" + topic + ",Message:" + msg);
            try {
                couontSerivce.incrManyTimes(10000);
                ack.acknowledge();
                log.info("Kafka消费成功! Topic:" + topic + ",Message:" + msg);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Kafka消费失败！Topic:" + topic + ",Message:" + msg, e);
            }
        }
    }

    @KafkaListener(topics = "tp-mq-dispatch", groupId = "TEST_GROUP3",concurrency = "1", containerFactory = "kafkaManualAckListenerContainerFactory")
    public void dispatchForSvr3(ConsumerRecord<?, ?> record, Acknowledgment ack, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        Optional message = Optional.ofNullable(record.value());
        if (message.isPresent()) {
            Object msg = message.get();
            System.out.println("svr3 收到Kafka消息! Topic:" + topic + ",Message:" + msg);
            try {
                couontSerivce.incrManyTimes(10000);
                ack.acknowledge();
                log.info("Kafka消费成功! Topic:" + topic + ",Message:" + msg);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Kafka消费失败！Topic:" + topic + ",Message:" + msg, e);
            }
        }
    }

}




