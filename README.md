# ScreenSort: 100% Offline Autonomous Screenshot Categorizer

An offline Android application written in **Kotlin** and **Jetpack Compose Material 3** that uses **Google ML Kit OCR** to extract text from your screenshots and **autonomously discovers, invents, and names categories on-device** without any hardcoded category lists or cloud dependencies.

---

## 🌟 Key Features

- **🔒 100% Offline & Private**:
  - Uses bundled Google ML Kit OCR (`com.google.mlkit:text-recognition:16.0.1`), statically linked inside the APK.
  - `INTERNET` permission is **not** requested in `AndroidManifest.xml`. Zero bytes ever leave your phone.
- **🧠 Autonomous Dynamic Categorization**:
  - No fixed or predefined category lists (no hardcoded "Movies", "Software", etc.).
  - Extracts keywords and contextual phrases (N-grams) using on-device **TF-IDF feature vectorization**.
  - Calculates semantic cosine similarity to discover clusters of related screenshots.
  - Autonomously generates clean, human-readable category names (e.g., *"Docker & Container"*, *"Boarding Pass & Flight"*, *"Recipe & Baking"*, *"Invoice & Total"*).
- **⚡ Instant Live OCR Search**:
  - Search across all text extracted from your screenshots with real-time matching.
- **📱 Modern Jetpack Compose UI**:
  - Dynamic category chips with live count badges and distinctive procedural color palettes.
  - Grid preview with image thumbnails, category tags, and extracted text snippets.
  - Full-screen detail view with high-resolution preview, scrollable OCR text, 1-tap **"Copy Text"** to clipboard, and category renaming.
- **🔄 Auto & Manual Ingestion**:
  - **Scan Device**: Automatically queries device storage for screenshots.
  - **Import**: Multi-image picker to import specific screenshots or test photos directly.
  - **Re-cluster**: Re-analyzes your entire library anytime as new screenshot patterns emerge.

---

## 🚀 Beginner Guide: How to Run in Android Studio

As an absolute beginner, follow these simple steps:

### 1. Open in Android Studio
1. Launch **Android Studio**.
2. Click **Open** (or `File > Open`).
3. Browse and select the project folder where you cloned or extracted this repository (e.g. `screenshoter`).
4. Click **OK**. Android Studio will load the project and sync Gradle dependencies automatically.

### 2. Set Up a Device or Emulator
- **Using an Android Emulator (Virtual Device)**:
  1. In Android Studio, open **Device Manager** (icon on the right sidebar or `Tools > Device Manager`).
  2. If you don't have an emulator yet, click **Create Device**, pick a phone (e.g. *Pixel 8*), and click **Next > Finish**.
  3. Click the **Play** button to boot the virtual phone.
- **Using Your Physical Android Phone**:
  1. On your phone, go to `Settings > About Phone` and tap `Build Number` 7 times to enable Developer Options.
  2. Go to `Settings > System > Developer Options` and turn on **USB Debugging**.
  3. Plug your phone into your computer via USB cable.

### 3. Run the App
1. In the top toolbar of Android Studio, select your device from the dropdown.
2. Click the green **Run** button (or press `Shift + F10`).
3. The app will compile and launch on your phone!

---

## 📁 Project Architecture & Structure

```
screenshoter/
├── app/
│   ├── build.gradle.kts                   # Module dependencies (Compose, ML Kit, Room, Coil)
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml        # Storage permissions (zero internet)
│   │   │   ├── java/com/screensort/app/
│   │   │   │   ├── ScreenSortApp.kt       # Application class (initializes DB)
│   │   │   │   ├── MainActivity.kt        # Entry Activity & Permission launchers
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── AppDatabase.kt         # Room SQLite database singleton
│   │   │   │   │   │   ├── ScreenshotDao.kt       # Reactive SQL queries & search
│   │   │   │   │   │   └── ScreenshotEntity.kt    # Data model for screenshots
│   │   │   │   │   └── repository/
│   │   │   │   │       └── ScreenshotRepository.kt# Bridges OCR, clustering, and DB
│   │   │   │   ├── domain/
│   │   │   │   │   ├── StopWords.kt               # Stopwords & mobile UI noise filters
│   │   │   │   │   ├── TextPreprocessor.kt        # Tokenizer & N-gram extractor
│   │   │   │   │   ├── TfidfVectorizer.kt         # Mathematical TF-IDF vectorizer
│   │   │   │   │   └── DynamicTopicEngine.kt      # Autonomous clustering & topic namer
│   │   │   │   ├── ocr/
│   │   │   │   │   ├── ImageAnalysisManager.kt    # Multimodal Google ML Kit OCR & Image Labeling
│   │   │   │   │   └── MediaStoreScanner.kt       # Scans device screenshot directories
│   │   │   │   └── ui/
│   │   │   │       ├── theme/                     # Colors, Dynamic Category Palette, Theme
│   │   │   │       ├── components/
│   │   │   │       │   ├── DynamicCategoryChips.kt# Filter chips with live counts
│   │   │   │       │   ├── ScreenshotCard.kt      # Grid item card
│   │   │   │       │   └── ScreenshotDetailDialog.kt # Full view + Copy OCR text
│   │   │   │       ├── viewmodel/
│   │   │   │       │   └── MainViewModel.kt       # StateFlow UI state holder
│   │   │   │       └── screens/
│   │   │   │           └── MainScreen.kt          # Compose scaffold & layout
│   │   │   └── res/                               # Icons, themes, strings, XML rules
│   │   └── test/
│   │       └── java/com/screensort/app/
│   │           └── DynamicTopicEngineTest.kt      # Unit tests for autonomous clustering
│   └── proguard-rules.pro
├── build.gradle.kts                               # Root build script
├── settings.gradle.kts                            # Project repositories
├── gradle.properties                              # JVM & AndroidX settings
├── local.properties                               # Android SDK path
└── gradlew / gradlew.bat                          # Gradle wrapper
```

---

## 🧪 How the Dynamic Categorization Engine Works

Unlike basic apps that rely on hardcoded keyword checklists, ScreenSort uses mathematical Natural Language Processing on-device:

1. **Noise Removal**: `TextPreprocessor` cleans out timestamps, battery percentages, system icons, and common English stopwords.
2. **Phrase Extraction**: Extracts single words and two-word phrases (bigrams) like `"git commit"`, `"flight ticket"`, `"docker compose"`.
3. **TF-IDF Vectorization**: Calculates the statistical importance of each phrase relative to the screenshot collection.
4. **Similarity Clustering**: Uses vector **Cosine Similarity** to group semantically related screenshots together.
5. **Autonomous Cluster Naming**: Determines the most salient, high-weight terms in the cluster and formats them into a clean title (e.g., `"Docker & Container"`, `"Flight Ticket"`).
6. **User Control**: You can tap **Rename** on any screenshot to customize or override any category name anytime!
