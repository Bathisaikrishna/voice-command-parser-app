package com.tutorial.kafka.stream.service;

import com.tutorial.kafka.stream.model.ParsedVoiceCommand;
import com.tutorial.kafka.stream.model.VoiceCommand;

public class MockSttClient implements SpeechToTextService {

    public ParsedVoiceCommand speechToText(VoiceCommand value) {
         switch (value.getId()) {
            case "26679943-f55e-4731-986e-c5c5395715de" :
                return ParsedVoiceCommand.builder()
                    .id(value.getId())
                    .text("call john")
                    .probability(0.957)
                    .language(value.getLanguage())
                    .build();
             case "9821f112-ec35-4679-91e7-c558de479bc5":
                 return ParsedVoiceCommand.builder()
                    .id(value.getId())
                    .text("llamar a juan")
                    .probability(0.937)
                    .language(value.getLanguage())
                    .build();
             default:
                 return ParsedVoiceCommand.builder()
                    .id(value.getId())
                    .text("call john")
                    .probability(0.37)
                    .language(value.getLanguage())
                    .build();
        }
    }
}