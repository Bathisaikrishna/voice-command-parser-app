package com.tutorial.kafka.stream.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedVoiceCommand {
    private String id;
    private String text;
    private String audioCodec;
    private String language;
    private double probability;
}
