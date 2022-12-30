package com.tutorial.kafka.stream.service;

import com.tutorial.kafka.stream.model.ParsedVoiceCommand;

public class MockTranslateClient implements TranslateService {

    public ParsedVoiceCommand translate(ParsedVoiceCommand original) {
        return ParsedVoiceCommand.builder()
                .id(original.getId())
                .text("call juan")
                .probability(original.getProbability())
                .language(original.getLanguage())
                .build();
    }
}
