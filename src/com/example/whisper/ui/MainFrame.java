package com.example.whisper.ui;

import com.example.whisper.model.SttResult;
import com.example.whisper.service.AudioRecorderService;
import com.example.whisper.service.WhisperApiClient;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MainFrame extends JFrame {

    private final AudioRecorderService recorderService = new AudioRecorderService();
    private final WhisperApiClient apiClient = new WhisperApiClient("http://127.0.0.1:8000/api/stt/transcribe");

    private JButton btnRecord;
    private JButton btnStop;
    private JCheckBox chkVadFilter;
    private JSlider sliderThreshold;
    private JLabel lblThresholdVal;
    private JProgressBar energyBar;
    private JLabel lblStatus;
    private JTextArea txtResult;

    public MainFrame() {
        setTitle("🎙️ Whisper 語音辨識桌面端 (Java JFrame + FastAPI)");
        setSize(580, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents();
    }

    private void initComponents() {
        // 1. 上方：VAD 設定與即時音量面板
        JPanel topPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("1. VAD (語音活動檢測) 設定"));

        chkVadFilter = new JCheckBox("啟用 VAD 靜音過濾 (過濾無人聲雜訊)", true);
        topPanel.add(chkVadFilter);

        JPanel sliderPanel = new JPanel(new BorderLayout(5, 0));
        lblThresholdVal = new JLabel("VAD 門檻 (Threshold): 0.50  ");
        sliderThreshold = new JSlider(10, 90, 50);
        sliderThreshold.addChangeListener(e -> {
            double val = sliderThreshold.getValue() / 100.0;
            lblThresholdVal.setText(String.format("VAD 門檻 (Threshold): %.2f  ", val));
        });
        sliderPanel.add(lblThresholdVal, BorderLayout.WEST);
        sliderPanel.add(sliderThreshold, BorderLayout.CENTER);
        topPanel.add(sliderPanel);

        energyBar = new JProgressBar(0, 100);
        energyBar.setStringPainted(true);
        energyBar.setString("麥克風即時音量能量: 0%");
        topPanel.add(energyBar);

        add(topPanel, BorderLayout.NORTH);

        // 2. 中間：控制按鈕與狀態
        JPanel centerPanel = new JPanel(new BorderLayout(5, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        btnRecord = new JButton("🔴 開始錄音");
        btnRecord.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        btnRecord.setBackground(new Color(40, 167, 69));
        btnRecord.setForeground(new Color(0, 128, 0));

        btnStop = new JButton("⏹️ 停止並送出辨識");
        btnStop.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        btnStop.setForeground(new Color(200, 0, 0));
        btnStop.setEnabled(false);

        btnPanel.add(btnRecord);
        btnPanel.add(btnStop);

        lblStatus = new JLabel("狀態：待機中 (請先啟動 Python FastAPI 8000 Port)", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));

        centerPanel.add(btnPanel, BorderLayout.NORTH);
        centerPanel.add(lblStatus, BorderLayout.CENTER);

        // 3. 下方：辨識結果區
        txtResult = new JTextArea();
        txtResult.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 15));
        txtResult.setLineWrap(true);
        txtResult.setWrapStyleWord(true);
        txtResult.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(txtResult);
        scrollPane.setBorder(BorderFactory.createTitledBorder("2. 辨識結果 (繁體中文)"));
        scrollPane.setPreferredSize(new Dimension(540, 240));

        centerPanel.add(scrollPane, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        btnRecord.addActionListener(e -> handleStartRecording());
        btnStop.addActionListener(e -> handleStopAndSend());
    }

    private void handleStartRecording() {
        try {
            recorderService.startRecording(volume -> SwingUtilities.invokeLater(() -> {
                int thresholdPercent = sliderThreshold.getValue();
                energyBar.setValue(volume);
                energyBar.setForeground(volume >= thresholdPercent ? new Color(40, 167, 69) : Color.GRAY);
                energyBar.setString(String.format("即時音量：%d%% (門檻：%d%%)", volume, thresholdPercent));
            }));

            btnRecord.setEnabled(false);
            btnStop.setEnabled(true);
            lblStatus.setText("狀態：🔴 錄音中... 請對著麥克風說話");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "無法啟動錄音：" + ex.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleStopAndSend() {
        File wavFile = recorderService.stopRecording();
        energyBar.setValue(0);
        energyBar.setString("麥克風即時音量能量: 0%");

        btnRecord.setEnabled(false);
        btnStop.setEnabled(false);
        lblStatus.setText("狀態：⏳ 正在傳送音檔至 FastAPI 進行 AI 辨識...");

        new Thread(() -> {
            try {
                boolean vadFilter = chkVadFilter.isSelected();
                double threshold = sliderThreshold.getValue() / 100.0;

                SttResult result = apiClient.transcribe(wavFile, vadFilter, 500, threshold);

                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("狀態：✅ 辨識完成！");
                    txtResult.setText("【辨識文字】\n" + result.getFullText() + "\n\n【完整 API 回傳 JSON】\n" + result.getRawJson());
                    btnRecord.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("狀態：❌ 連線失敗，請確認 Python 服務已啟動");
                    txtResult.setText("錯誤訊息：\n" + ex.getMessage());
                    btnRecord.setEnabled(true);
                });
            } finally {
                if (wavFile != null && wavFile.exists()) {
                    wavFile.delete();
                }
            }
        }).start();
    }
}
