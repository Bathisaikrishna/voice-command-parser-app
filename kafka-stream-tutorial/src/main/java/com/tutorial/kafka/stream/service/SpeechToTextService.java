package com.tutorial.kafka.stream.service;

import com.tutorial.kafka.stream.model.ParsedVoiceCommand;
import com.tutorial.kafka.stream.model.VoiceCommand;

public interface SpeechToTextService {
    ParsedVoiceCommand speechToText(VoiceCommand voiceCommand);
}
