package com.example.whisper.service;

import com.example.whisper.model.SttResult;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WhisperApiClient {

    private final String apiUrl;
    private final HttpClient httpClient;

    public WhisperApiClient(String apiUrl) {
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    public SttResult transcribe(File wavFile, boolean vadFilter, int minSilenceMs, double threshold) throws Exception {
        String boundary = "----JavaJFrameBoundary" + UUID.randomUUID().toString().replace("-", "");
        List<byte[]> byteArrays = new ArrayList<>();

        addFormField(byteArrays, boundary, "vad_filter", String.valueOf(vadFilter));
        addFormField(byteArrays, boundary, "min_silence_duration_ms", String.valueOf(minSilenceMs));
        addFormField(byteArrays, boundary, "threshold", String.valueOf(threshold));

        String fileHeader = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + wavFile.getName() + "\"\r\n" +
                "Content-Type: audio/wav\r\n\r\n";
        byteArrays.add(fileHeader.getBytes(StandardCharsets.UTF_8));
        byteArrays.add(Files.readAllBytes(wavFile.toPath()));
        byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP 錯誤碼: " + response.statusCode() + "\n" + response.body());
        }

        String json = response.body();
        String fullText = extractJsonValue(json, "full_text");
        return new SttResult(fullText, json);
    }

    private void addFormField(List<byte[]> byteArrays, String boundary, String name, String value) {
        String field = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" +
                value + "\r\n";
        byteArrays.add(field.getBytes(StandardCharsets.UTF_8));
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return "(無辨識文字)";
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        return end != -1 ? json.substring(start, end) : "(解析失敗)";
    }
}
