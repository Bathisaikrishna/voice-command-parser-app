package com.tutorial.kafka.stream.service;

import com.tutorial.kafka.stream.model.ParsedVoiceCommand;

public interface TranslateService {
    ParsedVoiceCommand translate(ParsedVoiceCommand parsedVoiceCommand);
}
