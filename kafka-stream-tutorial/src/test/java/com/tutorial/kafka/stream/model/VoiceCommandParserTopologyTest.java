package com.tutorial.kafka.stream.model;

import com.tutorial.kafka.stream.VoiceCommandParserTopology;
import com.tutorial.kafka.stream.serdes.JsonSerde;
import com.tutorial.kafka.stream.service.SpeechToTextService;
import com.tutorial.kafka.stream.service.TranslateService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoiceCommandParserTopologyTest {

    @Mock
    private SpeechToTextService speechToTextService;

    @Mock
    private TranslateService translateService;

    private VoiceCommandParserTopology voiceCommandParserTopology;


    private TopologyTestDriver topologyTestDriver;
    private TestInputTopic<String, VoiceCommand> voiceCommandInputTopic;
    private TestOutputTopic<String, ParsedVoiceCommand> recognizedCommandsOutputTopic;
    private TestOutputTopic<String, ParsedVoiceCommand> unrecognizedCommandsOutputTopic;

    @BeforeEach
    void setUp() {
        voiceCommandParserTopology = new VoiceCommandParserTopology(speechToTextService, translateService, 0.90);
        Topology topology = voiceCommandParserTopology.createTopology();

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");

        topologyTestDriver = new TopologyTestDriver(topology, props); //simulates kafka server
        JsonSerde<VoiceCommand> voiceCmdJsonSerde = new JsonSerde<>(VoiceCommand.class);
        JsonSerde<ParsedVoiceCommand> parsedVoiceCmdJsonSerde = new JsonSerde<>(ParsedVoiceCommand.class);
        voiceCommandInputTopic = topologyTestDriver.createInputTopic(VoiceCommandParserTopology.VOICE_COMMANDS_TOPIC,
                Serdes.String().serializer(), voiceCmdJsonSerde.serializer());
        recognizedCommandsOutputTopic = topologyTestDriver.createOutputTopic(VoiceCommandParserTopology.RECOGNIZED_COMMANDS_TOPIC,
                Serdes.String().deserializer(), parsedVoiceCmdJsonSerde.deserializer());
        unrecognizedCommandsOutputTopic = topologyTestDriver.createOutputTopic(VoiceCommandParserTopology.UNRECOGNIZED_COMMANDS_TOPIC,
                Serdes.String().deserializer(), parsedVoiceCmdJsonSerde.deserializer());

    }

    @Test
    @DisplayName("Given an English voice command, When processed correctly Then I receive a ParsedVoiceCommand in the recognnized-commands topic.")
    void testScenario1() {
        //Preconditions given
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        VoiceCommand voiceCommand = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(randomBytes)
                .audioCodec("FLAC")
                .language("en-US")
                .build();
        ParsedVoiceCommand parsedVoiceCommand1 = ParsedVoiceCommand.builder()
                .id(voiceCommand.getId())
                .text("call bathi")
                .language("en-US")
                .probability(0.98)
                .build();
        given(speechToTextService.speechToText(voiceCommand)).willReturn(parsedVoiceCommand1);
        //actions When
        voiceCommandInputTopic.pipeInput(voiceCommand);
        //Verifications, then

        ParsedVoiceCommand parsedVoiceCommand = recognizedCommandsOutputTopic.readRecord().value();

        assertEquals(voiceCommand.getId(), parsedVoiceCommand.getId());
        assertEquals("call bathi", parsedVoiceCommand.getText());
    }

    @Test
    @DisplayName("Given a non-English voice command, When processed correctly Then I receive a ParsedVoiceCommand in the recognized-commands topic")
    void testScenario2() {
        //Preconditions given
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        VoiceCommand voiceCommand = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(randomBytes)
                .audioCodec("FLAC")
                .language("es-AR")
                .build();
        ParsedVoiceCommand parsedVoiceCommand1 = ParsedVoiceCommand.builder()
                .id(voiceCommand.getId())
                .text("llamar a Bathi")
                .language("es-AR")
                .probability(0.98)
                .build();
        given(speechToTextService.speechToText(voiceCommand)).willReturn(parsedVoiceCommand1);
        ParsedVoiceCommand translatedVoiceCommand = ParsedVoiceCommand.builder()
                .id(voiceCommand.getId())
                .text("call bathi")
                .language("en-US")
                .probability(0.98)
                .build();
        given(translateService.translate(parsedVoiceCommand1)).willReturn(translatedVoiceCommand);
        //actions When
        voiceCommandInputTopic.pipeInput(voiceCommand);
        //Verifications, then

        ParsedVoiceCommand parsedVoiceCommand = recognizedCommandsOutputTopic.readRecord().value();

        assertEquals(voiceCommand.getId(), parsedVoiceCommand.getId());
        assertEquals("call bathi", parsedVoiceCommand.getText());
    }

    @Test
    @DisplayName("Given a non-recognizable voice command, When processed correctly Then I receive a ParsedVoiceCommand in the unrecognized-commands topic")
    void testScenarios3() {
        //Preconditions given
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        VoiceCommand voiceCommand = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(randomBytes)
                .audioCodec("FLAC")
                .language("en-US")
                .build();
        ParsedVoiceCommand parsedVoiceCommand1 = ParsedVoiceCommand.builder()
                .id(voiceCommand.getId())
                .text("call john")
                .language("en-US")
                .probability(0.70)
                .build();
        given(speechToTextService.speechToText(voiceCommand)).willReturn(parsedVoiceCommand1);
        //actions When
        voiceCommandInputTopic.pipeInput(voiceCommand);
        //Verifications, then

        ParsedVoiceCommand parsedVoiceCommand = unrecognizedCommandsOutputTopic.readRecord().value();

        assertEquals(voiceCommand.getId(), parsedVoiceCommand.getId());
        assertEquals("call john", parsedVoiceCommand.getText());

        verify(translateService, never()).translate(any(ParsedVoiceCommand.class));
        assertTrue(recognizedCommandsOutputTopic.isEmpty());
    }

    @Test
    @DisplayName("Given voice command that is too short (less than 10 bytes), When processed correctly Then I don???t receive any command in any of the output topics")
    void testScenarios4() {
        //Preconditions given
        byte[] randomBytes = new byte[9];
        new Random().nextBytes(randomBytes);
        VoiceCommand voiceCommand = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(randomBytes)
                .audioCodec("FLAC")
                .language("en-US")
                .build();
        //actions When
        voiceCommandInputTopic.pipeInput(voiceCommand);
        //Verifications, then

        verify(translateService, never()).translate(any(ParsedVoiceCommand.class));
        verify(speechToTextService, never()).speechToText(any(VoiceCommand.class));
        assertTrue(recognizedCommandsOutputTopic.isEmpty());
        assertTrue(unrecognizedCommandsOutputTopic.isEmpty());
    }
}