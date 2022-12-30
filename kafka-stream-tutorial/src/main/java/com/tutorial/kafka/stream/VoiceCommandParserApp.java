package com.tutorial.kafka.stream;

import com.tutorial.kafka.stream.config.StreamsConfiguration;
import com.tutorial.kafka.stream.service.MockSttClient;
import com.tutorial.kafka.stream.service.MockTranslateClient;
import org.apache.kafka.streams.KafkaStreams;

public class VoiceCommandParserApp {

    public static void main(String[] args) {
        VoiceCommandParserTopology voiceCommandParserTopology = new VoiceCommandParserTopology(new MockSttClient(),
                new MockTranslateClient(), 0.90);

        StreamsConfiguration streamsConfiguration = new StreamsConfiguration();

        KafkaStreams kafkaStreams = new KafkaStreams(voiceCommandParserTopology.createTopology(), streamsConfiguration.createConfig());
        kafkaStreams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close)); //close stream gracefully when app got shut down
    }
}
