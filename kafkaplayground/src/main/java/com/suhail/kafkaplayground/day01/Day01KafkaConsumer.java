package com.suhail.kafkaplayground.day01;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;

public class Day01KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(Day01KafkaConsumer.class);

    public static void main(String[] args) {

        // Producer -> Kafka Broker  <-> Consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG,"play-group--123");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,"false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        props.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG,"1");

        // --from-beginning

        KafkaConsumer<String, String> consumer = new KafkaConsumer(props);
        consumer.subscribe(java.util.Collections.singletonList("order-events"));

        try{
            while(true){
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                records.forEach(
                        record -> log.info("Key : {}, Value : {}", record.key(), record.value()));

                if(!records.isEmpty()){
                    consumer.commitSync();
                }

            }
        }catch (Exception e){
            log.error("Error while consuming messages", e);
        }finally {
            consumer.close();
        }



    }
}
