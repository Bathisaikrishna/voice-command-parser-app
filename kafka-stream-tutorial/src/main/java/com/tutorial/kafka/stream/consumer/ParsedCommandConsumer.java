package com.tutorial.kafka.stream.consumer;

import com.tutorial.kafka.stream.VoiceCommandParserTopology;
import com.tutorial.kafka.stream.model.ParsedVoiceCommand;
import com.tutorial.kafka.stream.serdes.JsonSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Serdes;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ParsedCommandConsumer {

    public static void main(String[] args) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "parsed-command-consumer-1");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, ParsedVoiceCommand> commandConsumer = new KafkaConsumer<>(props, Serdes.String().deserializer(),
                new JsonSerde<>(ParsedVoiceCommand.class).deserializer())) {
            commandConsumer.subscribe(Arrays.asList(VoiceCommandParserTopology.RECOGNIZED_COMMANDS_TOPIC, VoiceCommandParserTopology.UNRECOGNIZED_COMMANDS_TOPIC));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> close(commandConsumer)));

            while (true)  {
                commandConsumer.poll(Duration.ofSeconds(1))
                        .forEach(record -> System.out.printf("Topic: %s Result: %s%n", record.topic(),
                                record.value().toString()));
                commandConsumer.commitAsync();
            }
        }
    }

    private static void close(KafkaConsumer<String, ParsedVoiceCommand> commandConsumer) {
        commandConsumer.wakeup();
    }
}
