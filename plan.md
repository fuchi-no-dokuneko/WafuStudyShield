# StudyShield — 開源 Android 學生專注提醒器

> 狀態：產品與技術計劃草案  
> 核心原則：**學生自訂、自願啟動、離線優先、清楚可退出、不可蒐集社交內容。**

## 1. 產品願景

StudyShield 是一個開源 Android 專注工具。學生自行選擇會分心的社交媒體 app、設定讀書時段與提醒風格；在規則生效期間，當目標 app 被開啟，系統以全螢幕 Accessibility overlay 顯示一個可配置的專注提示介面。

與傳統「強制封鎖器」不同，本項目把第一目標定義為**在分心行為發生的一刻提供有感、個人化、可反思的中斷**。它不宣稱可不可繞過地禁止 app 啟動；使用者仍可關閉服務、移除 app 或調整規則。

### 主要使用情境

- 學生於上課、做功課、溫習或自習時，自行設定社交 app 的干擾規則。
- 當 TikTok、Instagram、YouTube、X、Discord 等已選 app 成為前景時，畫面立即轉成專注提醒 overlay。
- 提醒可包含角色肖像、背景圖、語音、文字泡及「今次想繼續專注嗎？」等自訂訊息。
- 裝置靜音、媒體音量為零，或音訊未能播放時，畫面必須顯示同一提示的文字版本，避免提示失效。

## 2. 範圍與非範圍

### 首版包含

- 星期與多個時間範圍規則。
- 可選擇的受提醒 app 清單。
- AccessibilityService 偵測前景視窗變更，再以 `TYPE_ACCESSIBILITY_OVERLAY` 顯示全螢幕提醒。
- 可暫停、略過一次、結束專注時段、查看當日自我紀錄。
- 全部資料、本地角色素材與活動紀錄只留在裝置內。
- 角色／Waifu mode：以本地「角色包」載入圖片、背景、MP3、文字訊息與版面設定。

### 首版不包含

- 監看訊息內容、讀取密碼、擷取螢幕文字、模擬點擊、模擬滑動或代替用戶操作其他 app。
- 隱藏服務、阻止使用者進入 Android Settings、阻止緊急通話、阻止卸載或把自願工具偽裝成系統功能。
- 雲端帳戶、廣告、追蹤 SDK、遠端角色聊天、生成式 AI 對話或跨裝置同步。
- 任何成人化、浪漫操控、羞辱、威嚇或把學生焦慮作為留存機制的角色文案。

## 3. 為何以 Accessibility overlay 作為核心

Android 的 `TYPE_ACCESSIBILITY_OVERLAY` 是由 AccessibilityService 建立的視窗類型，可覆蓋畫面並攔截互動；它能在目標 app 已經成為前景時立即顯示提醒。Android 文件同時明確指出 AccessibilityService 是專門的輔助工具，而非一般用途 UI 技巧。因此本項目採取最小權限、最少資料、沒有自動操作、可見且可停用的設計。

### 核心互動管線

```text
使用者建立規則
  → 規則引擎判斷目前時段是否生效
  → AccessibilityService 收到前景視窗改變事件
  → 只比較 package name 是否屬於目標清單
  → 建立 / 更新 TYPE_ACCESSIBILITY_OVERLAY
  → 顯示角色、背景、語音或文字泡
  → 使用者選擇返回、短暫略過或停止本次專注
  → 寫入本地、可刪除的自我紀錄
```

### 服務安全邊界

- 僅訂閱需要的視窗狀態事件；不要求取得或解析 Accessibility node tree。
- 僅使用前景 app 的 package name 作規則匹配；不讀 app 內文字、聯絡人、訊息、表單或瀏覽資料。
- 不呼叫手勢派送或任何自動點擊功能。
- 不對 Android Settings、系統 permission、鎖屏、付款、通話與緊急畫面顯示可攔截 overlay。
- 疊加層顯示清楚的 app 名稱、目前規則與退出入口，避免造成系統介面混淆。
- Accessibility service 停用、設定不完整或規則不生效時，立即移除 overlay 並停止音訊。

## 4. 核心功能規格

### 4.1 專注規則（Focus Profiles）

每個 Profile 包含：名稱、啟用狀態、星期選擇、可重複的時間範圍、目標 app、提醒強度、可選角色包與略過行為。

規則必須支援同一日有多段時間，例如上課、自習與睡前。若規則重疊，採取最嚴格的顯示策略，但不把多條音訊同時播放。

### 4.2 目標 app 選擇

- 以已安裝 app 清單供使用者主動勾選。
- 儲存 package name 與使用者可讀名稱；顯示圖示只作本機 UI。
- 不上傳 app 清單，亦不建立社交行為画像。
- 預設不自動選擇任何社交 app。

### 4.3 提醒結果

受提醒 app 被開啟時：

- 背景：可選純色、半透明遮罩或使用者選擇的 wallpaper。
- 角色：在左或右側出現，可固定一側或每次觸發隨機選擇一側；同一個提示事件內固定位置，不會反覆跳動。
- 對話：顯示「目前是 [Profile]」與角色文字；可選顯示倒數、今日提醒次數或學生自訂學習目標。
- 動作：返回桌面、短暫略過、查看本次規則；所有動作要明確標籤，並保留無角色的簡潔模式。

## 5. Waifu / Companion mode 詳細規格

此功能的產品名稱建議採用 **Companion mode**，介面可保留「Waifu mode」作使用者選項。理由是開源專案的公開定位應涵蓋所有學生，而角色只是可選的提醒呈現方式。

### 5.1 角色包格式

角色包是一個可匯入、可匯出的本地資料夾或壓縮檔。其資料包含：

- `manifest`：名稱、作者、授權、語言、內容分級與相容版本。
- 肖像圖片：透明 PNG、WebP 或其他 Android 支援圖片格式。
- wallpaper：可選的背景圖片。
- 音訊：可選 MP3；一條語音必須有對應文字稿。
- dialogue：對應場景的文字訊息，例如「啟動提醒」、「第一次略過」、「結束專注」。
- layout preset：角色、文字泡和背景的預設位置與縮放建議。

初始版本只會附帶原創、可再分發、非成人化的示例角色包。使用者匯入的角色包由使用者負責其著作權與使用權；專案文件必須明示不可把未獲授權的動畫、遊戲、聲優錄音或角色圖像納入官方發行物。

### 5.2 角色左右位置與隨機策略

- 選項：固定左側、固定右側、每次提醒隨機、按角色包預設。
- 隨機策略以本次提醒事件為單位；生成後鎖定到事件結束。
- 文字泡會自動選擇角色相反的一側，並保留安全邊距，避免被瀏海、鏡頭開孔、system bar 或手勢區遮擋。
- 畫面旋轉、分割視窗或異常螢幕尺寸時，使用 normalized coordinates 重新排版，而不是以像素硬編碼。

### 5.3 Wallpaper 與圖片設定

使用者可為每個 Profile 或角色包設定 wallpaper：

- 以 Android Photo Picker 選取圖片，避免要求整個相簿存取權。
- 在預覽畫面採用「wallpaper-style」裁切：拖曳平移、雙指縮放或縮放滑桿、還原、預覽安全區。
- 圖片的 scale、translation、overlay dim、blur 是否啟用、角色透明度均可各自配置。
- 預覽、實際 Accessibility overlay 與復原狀態使用同一份 layout model，避免設定畫面與實際效果不一致。

### 5.4 MP3 與靜音時的文字補償

- 使用者可從本機選取 MP3；Audio/Storage 存取使用系統檔案選擇器，並保存可讀取的 URI 權限，而非掃描整個裝置。
- 播放前請求適當的 audio focus；未獲得 audio focus 時不得播放。
- 當媒體音量為零、裝置靜音、audio focus 遭拒、檔案遺失、解碼失敗或使用者關閉語音時，改為顯示對應的字幕／文字泡。
- 文字泡不是音訊失敗才臨時生成的內容；每條語音在角色包中已有可編輯的對應文字，確保離線、可審閱和可翻譯。
- 音訊不循環、不自動加大裝置音量、不在提醒結束後繼續播放。

### 5.5 可編輯文字泡位置與動畫

使用者可對每個角色包設定：

- 文字泡錨點：以螢幕寬高的比例儲存 X、Y 座標。
- 對齊方式：靠左、置中、靠右；文字方向與字體大小採用 Android 系統可及性字級。
- 動畫：淡入、停留、淡出；淡出階段可選同時向下位移（fade-down），使訊息自然離場。
- 角色肖像、文字泡與背景各自有不同透明度與動畫設定。
- 「減少動態效果」模式：停用位移與非必要動畫，只保留穩定文字提醒。

## 6. 建議技術架構

採用原生 Kotlin Android，而非跨平台封裝。這是因為核心能力依賴 Android AccessibilityService、WindowManager、系統事件、媒體焦點與持久 URI 權限；它們都屬 Android 平台 API，原生實作較可測試和可維護。

```text
app/
  onboarding/           權限說明、明確同意、功能可用性檢查
  profiles/             時段與目標 app 規則編輯
  accessibility/        前景事件過濾、服務生命週期、風險畫面排除
  overlay/              WindowManager overlay、Compose overlay UI、layout renderer
  companion/            角色包匯入、素材驗證、對話選擇、位置配置
  media/                本機音訊、audio focus、字幕 fallback
  storage/              Room 資料庫、URI 權限、設定、可刪除紀錄
  analytics_local/      僅本機的提示、略過與完成次數
  test/                 unit、instrumentation、UI、device compatibility tests
```

### 建議採用的既有元件，而非重造輪子

| 需要 | 採用 | 原因 |
|---|---|---|
| 結構化本地資料 | Room | Android 官方 SQLite abstraction；適合規則、角色包索引與本地紀錄。 |
| 視覺 UI | Jetpack Compose | 將主 app 設定 UI 與 overlay renderer 共用 state model。 |
| 圖片選取 | Android Photo Picker | 只授權所選影像，符合最少媒體存取。 |
| MP3 播放 | Jetpack Media3 / ExoPlayer | Android 建議的可擴展播放器 API。 |
| 音訊協調 | AudioFocusRequest | 不與其他 app 的聲音無序競爭。 |
| 背景／前景判斷 | AccessibilityService | 核心事件來源；限制只使用 package-level 視窗變更。 |
| overlay | WindowManager + `TYPE_ACCESSIBILITY_OVERLAY` | 讓提示在目標 app 上即時顯示。 |

### 資料模型

| 實體 | 關鍵資料 | 用途 |
|---|---|---|
| `FocusProfile` | 名稱、啟用狀態、角色包、強度 | 一組學生自訂規則。 |
| `ScheduleRule` | 星期、開始時間、結束時間、時區 | 判斷現在是否應提醒。 |
| `TargetApp` | package name、顯示名稱、是否啟用 | 對應受提醒 app。 |
| `CompanionPack` | manifest、素材 URI、授權、語言 | 描述一個可匯入角色包。 |
| `DialogueCue` | 場景、文字、音訊 URI、fallback 文字 | 角色的提示內容。 |
| `OverlayLayout` | normalized position、scale、opacity、side、動畫 | 角色與文字泡在畫面的設定。 |
| `FocusEvent` | 時間、Profile、目標 package、使用者結果 | 僅本機的自我反思紀錄。 |

## 7. Onboarding、權限與 Play 發佈設計

### 權限流程

- 首頁先說明：「此服務只會知道目前開啟的是哪個 app，以在你預先選擇的 app 和時段顯示專注提醒。」
- 顯示不會讀取的資料：訊息文字、密碼、畫面內容、聯絡人、相片庫全量、位置與網路瀏覽內容。
- 使用者清楚同意後，才跳轉 Android Accessibility settings。
- 返回 app 後檢查服務狀態；未成功啟用時仍可編輯規則與預覽，但不宣稱 blocker 已在運作。
- media 選取僅在使用者點選自訂圖像或 MP3 時才出現。
## 9. UX 流程

### 首次設定

1. 選擇「建立自習 Profile」。
2. 勾選受提醒 app。
3. 設定星期與時間範圍。
4. 閱讀 AccessibilityService 披露並作出同意。
5. 在 Android Settings 啟用服務後返回 app。
6. 選擇簡潔提醒或 Companion mode。
7. 選擇／匯入角色包，設定背景、角色側邊、縮放、文字泡位置、音訊與字幕。
8. 以模擬目標 app 觸發測試 overlay；成功後啟用 Profile。

### 觸發時

1. 服務偵測目標 package 成為前景。
2. 檢查是否有 active Profile 和可用規則。
3. 選擇本次角色位置與場景對話。
4. 顯示 overlay；若可安全播放語音則播放，否則顯示文字泡。
5. 使用者作出退出、短暫略過或保留提醒的選擇。
6. overlay 移除、音訊停止、事件寫入本地資料庫。

## 10. 測試與驗收

### 功能驗收

- 多個重疊時間範圍的判斷結果一致。
- 所有已選 app 在生效時段觸發；非目標 app 不觸發。
- 橫直方向、不同密度與不同螢幕比例下角色、文字泡、按鈕不被 system UI 遮擋。
- 音量為零、靜音、無 audio focus、MP3 檔案遺失時，字幕 fallback 都可見且含完整訊息。
- 角色隨機左右位置在同一事件內維持不變。
- 關閉服務、結束規則或進入排除畫面時 overlay 立即移除。
- 匯入失敗的素材包不會令服務崩潰，也不會遺留視窗或播放音訊。

### 私隱與安全驗收

- 網路被完全封鎖時核心功能仍可運作。
- 原始碼審查可證明沒有 analytics SDK、廣告 SDK、雲端 API 或背景資料上傳。
- 不儲存目標 app 的畫面內容或 Accessibility node 資料。
- 「刪除我的資料」可清除所有本地規則、素材索引與事件紀錄；使用者匯入的原檔是否刪除必須另行清楚選擇。
- 所有官方角色包可追溯授權來源；所有第三方提交都需 manifest 授權欄位與審核。
### Repository 文件

- `README.md`：產品定位、安裝方式、Android 限制、權限說明、資料不會被讀取的範圍。
- `ARCHITECTURE.md`：模組邊界、事件流程、overlay 生命週期及本地資料模型。
- `PRIVACY.md`：離線優先、資料儲存位置、刪除流程、第三方素材風險。
- `ACCESSIBILITY_DISCLOSURE.md`：可直接對應 app 內與 Play Console 的披露文案。
- `COMPANION_PACK_SPEC.md`：角色包 schema、授權欄位、素材限制、字幕要求與內容規範。
- `CONTRIBUTING.md`：程式、翻譯、角色包、測試裝置矩陣與 issue template。

### 貢獻守則

- 不接受需要伺服器追蹤、廣告識別碼、隱藏監控或讀取其他 app 內容的功能。
- 不接受將 AccessibilityService 用於自動填表、點擊、付款、破解限制或偽裝系統畫面的 Pull Request。
- 角色包不得包含未授權素材、露骨性內容、以羞辱逼迫學習的文案或針對未成年人的操控性關係設計。

## 12. 里程碑

### Foundation

建立原生 app、資料庫、Profile／時段編輯、目標 app 選擇與 AccessibilityService 披露流程。

### Overlay Core

完成 package-level 觸發、排除畫面、WindowManager overlay、簡潔非角色提示、服務關閉的清理機制及測試工具。

### Companion Mode

完成角色包 schema、圖片／MP3 選取、Photo Picker、Media3 播放、字幕 fallback、左右隨機、wallpaper-style crop 和文字泡 layout editor。

### Trust and Distribution

完成 privacy review、可重現 build、F-Droid metadata、Play disclosure material、無網路測試與角色素材授權審查。

### Community Extensions

穩定角色包規格、提供示例包與驗證器、建立翻譯與安全內容貢獻流程；在不改變主 app 權限邊界下接受社群擴展。

## 14. 參考資料

- Android Developers — Accessibility service：<https://developer.android.com/guide/topics/ui/accessibility/service>
- Android Developers — AccessibilityWindowInfo / `TYPE_ACCESSIBILITY_OVERLAY`：<https://developer.android.com/reference/android/view/accessibility/AccessibilityWindowInfo>
- Google Play policy — Use of AccessibilityService API：<https://support.google.com/googleplay/android-developer/answer/10964491>
- Android Developers — Photo Picker：<https://developer.android.com/training/data-storage/shared/photo-picker>
- Android Developers — Media3 ExoPlayer：<https://developer.android.com/media/media3/exoplayer>
- Android Developers — Manage audio focus：<https://developer.android.com/media/optimize/audio-focus>
- Android Developers — Room：<https://developer.android.com/training/data-storage/room>
- DigiPaws / Curbox source project：<https://github.com/nethical6/digipaws>
- MindMaster source project：<https://github.com/ArmanKhanTech/MindMaster>
- Mindful source project：<https://github.com/akaMrNagar/Mindful>
- Digital Break source project：<https://github.com/lukesthl/digital-break-app>
- Google Creative Lab Digital Wellbeing Experiments Toolkit：<https://github.com/googlecreativelab/digital-wellbeing-experiments-toolkit>
