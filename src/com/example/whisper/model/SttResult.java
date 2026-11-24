package com.example.whisper.model;

public class SttResult {
    private final String fullText;
    private final String rawJson;

    public SttResult(String fullText, String rawJson) {
        this.fullText = fullText;
        this.rawJson = rawJson;
    }

    public String getFullText() {
        return fullText;
    }

    public String getRawJson() {
        return rawJson;
    }
}
