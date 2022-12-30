package com.tutorial.kafka.stream;

import com.tutorial.kafka.stream.model.ParsedVoiceCommand;
import com.tutorial.kafka.stream.model.VoiceCommand;
import com.tutorial.kafka.stream.serdes.JsonSerde;
import com.tutorial.kafka.stream.service.SpeechToTextService;
import com.tutorial.kafka.stream.service.TranslateService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.util.Map;

public class VoiceCommandParserTopology {

    public static String VOICE_COMMANDS_TOPIC = "voice-commands";
    public static String RECOGNIZED_COMMANDS_TOPIC = "recognized-commands";
    public static String UNRECOGNIZED_COMMANDS_TOPIC = "unrecognized-commands";

    private final SpeechToTextService speechToTextService;
    private final TranslateService translateService;
    private final double certaintyThreshold;

    public VoiceCommandParserTopology(SpeechToTextService speechToTextService,
                                      TranslateService translateService,
                                      double certaintyThreshold) {
        this.speechToTextService = speechToTextService;
        this.translateService = translateService;
        this.certaintyThreshold = certaintyThreshold;
    }

    public Topology createTopology() {
        StreamsBuilder streamsBuilder = new StreamsBuilder();

        JsonSerde<VoiceCommand> voiceCommandJsonSerde = new JsonSerde<>(VoiceCommand.class);
        JsonSerde<ParsedVoiceCommand> parsedVoiceCommandJsonSerde = new JsonSerde<>(ParsedVoiceCommand.class);

        Map<String, KStream<String, ParsedVoiceCommand>> branches = streamsBuilder.stream(VOICE_COMMANDS_TOPIC, Consumed.with(Serdes.String(), voiceCommandJsonSerde))
                .filter((key, value) -> value.getAudio().length >= 10, Named.as("filtered"))
                .mapValues((key, voiceCommand) -> speechToTextService.speechToText(voiceCommand))
                .split(Named.as("branches-"))
                .branch((key, parsedVoiceCommand) -> parsedVoiceCommand.getProbability() > certaintyThreshold, Branched.as("recognized"))
                .defaultBranch(Branched.as("unrecognized"));

        branches.get("branches-unrecognized")
                .to(UNRECOGNIZED_COMMANDS_TOPIC, Produced.with(Serdes.String(), parsedVoiceCommandJsonSerde));

        Map<String, KStream<String, ParsedVoiceCommand>> kStreamMap = branches.get("branches-recognized")
                .split(Named.as("language-"))
                .branch((key, parsedVoiceCommand) -> parsedVoiceCommand.getLanguage().startsWith("en"), Branched.as("english"))
                .defaultBranch(Branched.as("non-english"));

        kStreamMap.get("language-non-english")
                .mapValues((key, value) -> translateService.translate(value))
                .merge(kStreamMap.get("language-english"))
                .to(RECOGNIZED_COMMANDS_TOPIC, Produced.with(Serdes.String(), parsedVoiceCommandJsonSerde));
        return streamsBuilder.build();
    }
}
