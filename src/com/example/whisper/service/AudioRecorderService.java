package com.example.whisper.service;

import javax.sound.sampled.*;
import java.io.*;
import java.util.function.IntConsumer;

public class AudioRecorderService {

    private TargetDataLine microphone;
    private File tempWavFile;
    private volatile boolean isRecording = false;

    public void startRecording(IntConsumer onVolumeUpdate) throws Exception {
        // 設定 Whisper 原生支援格式：16kHz, 16-bit, 單聲道 WAV
        AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new RuntimeException("找不到支援的麥克風裝置！");
        }

        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();

        tempWavFile = File.createTempFile("jframe_whisper_", ".wav");
        isRecording = true;

        // 背景執行緒：讀取麥克風 PCM 資料、計算即時音量並寫入緩衝區
        new Thread(() -> {
            ByteArrayOutputStream pcmBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];

            try {
                while (isRecording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        pcmBuffer.write(buffer, 0, bytesRead);
                        int volumePercent = calculateRmsVolume(buffer, bytesRead);
                        if (onVolumeUpdate != null) {
                            onVolumeUpdate.accept(volumePercent);
                        }
                    }
                }
                // 錄音結束，將記憶體 PCM 資料寫出為標準 WAV 檔
                byte[] audioBytes = pcmBuffer.toByteArray();
                try (ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
                     AudioInputStream ais = new AudioInputStream(bais, format, audioBytes.length / format.getFrameSize())) {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tempWavFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public File stopRecording() {
        isRecording = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        // 稍微等待背景執行緒完成 WAV 寫檔
        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        return tempWavFile;
    }

    /**
     * 計算 16-bit Little-Endian PCM 音訊的 RMS 音量百分比 (0 ~ 100)
     */
    private int calculateRmsVolume(byte[] buffer, int bytesRead) {
        long sumSquares = 0;
        int sampleCount = bytesRead / 2;
        for (int i = 0; i < bytesRead - 1; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            sumSquares += (long) sample * sample;
        }
        if (sampleCount == 0) return 0;
        double rms = Math.sqrt((double) sumSquares / sampleCount);
        // 正規化並放大顯示比例方便肉眼觀察
        int percent = (int) Math.min(100, (rms / 32768.0) * 500);
        return Math.max(0, percent);
    }
}
