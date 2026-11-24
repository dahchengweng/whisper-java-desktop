# ☕ Whisper Desktop Client (Java JFrame + FastAPI)

本專案為 Whisper 語音辨識系統的 **Java JFrame 桌面客戶端**，採用純 JDK 標準函式庫實作（無需額外下載第三方 Jar 套件），透過 HTTP `multipart/form-data` 連線至本地 Python FastAPI 推論微服務。

## 📂 `src` 目錄結構說明

```text
src/
├── README.md                          # 本說明文件
└── com/example/whisper/
    ├── MainApp.java                   # 程式進入點
    ├── model/
    │   └── SttResult.java             # 辨識結果資料模型
    ├── service/
    │   ├── AudioRecorderService.java  # 麥克風錄音 (16kHz WAV) 與即時 RMS 音量計算
    │   └── WhisperApiClient.java      # HTTP 客戶端 (負責發送音檔至 FastAPI)
    └── ui/
        └── MainFrame.java             # Swing JFrame 視窗介面與事件控制
```

## 🚀 編譯與執行方式

### 前置條件
1. 已安裝 **JDK 11 或以上版本**（建議 JDK 17 / 21）。
2. Python FastAPI 後端已在 `http://127.0.0.1:8000` 啟動。

### 方式一：透過 VS Code 直接執行（推薦）
1. 在 VS Code 中開啟 `whisper-java-desktop` 資料夾（需安裝 Microsoft 官方 `Extension Pack for Java`）。
2. 打開 `src/com/example/whisper/MainApp.java`。
3. 點擊 `public static void main` 上方的 **`Run`** 按鈕即可啟動視窗。

### 方式二：透過終端機指令編譯與執行
在專案根目錄（`whisper-java-desktop/`）開啟 PowerShell 執行：

```powershell
# 1. 建立編譯輸出資料夾 bin
New-Item -ItemType Directory -Force -Path bin

# 2. 編譯 src 底下所有 Java 檔案 (指定 UTF-8 編碼避免中文註解報錯)
javac -encoding UTF-8 -d bin (Get-ChildItem -Recurse -Filter *.java -Path src | ForEach-Object { \$_.FullName })

# 3. 啟動 JFrame 桌面應用程式
java -cp bin com.example.whisper.MainApp
```
