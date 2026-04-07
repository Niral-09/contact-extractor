# Contact Extractor Android App — Design Spec

**Date:** 2026-04-05

---

## Context

Existing web prototype (`index.html`) extracts contact info from images using Google Gemini Vision API, deployed on GitHub Pages. Goal: convert to a production-grade native Android app with the same core OCR logic plus Google Sheets integration.

---

## Requirements

- **Platform:** Native Android, Kotlin, Jetpack Compose + Material 3
- **Fields:** Name, Mobile (10-digit), City only
- **Image input:** One image at a time (camera capture or gallery upload)
- **OCR:** Google Gemini API (`gemini-2.5-flash`), user provides API key once in Settings
- **Output 1:** Export VCF file via Android ShareSheet
- **Output 2:** Write contacts to Google Sheets — one tab per city, auto-create tabs
- **Auth:** Google Sign-In (OAuth) for Sheets; Gemini key stored in EncryptedSharedPreferences
- **Spreadsheet:** User pastes a Sheets URL or ID once in Settings (accepts both formats)
- **No backend:** All processing on-device

---

## Architecture

**Pattern:** MVVM, Single Activity, Jetpack Compose Navigation, Hilt DI

### Screen Flow

```
App Launch
  ├── Config incomplete → Settings Screen
  └── Config complete  → Home Screen
                              ↓ (image selected)
                        Processing (spinner overlay)
                              ↓
                        Results Screen (editable table)
                          ├── Save to Sheets
                          └── Export VCF
```

### Module Structure

```
app/
├── ui/
│   ├── settings/   SettingsScreen + SettingsViewModel
│   ├── home/       HomeScreen + HomeViewModel
│   └── results/    ResultsScreen + ResultsViewModel + VcfBuilder
├── data/
│   ├── model/      Contact.kt
│   ├── prefs/      SecurePrefsRepository
│   ├── ocr/        GeminiModels + GeminiApi + GeminiOcrRepository
│   └── sheets/     GoogleSheetsRepository
├── di/             AppModule (Hilt)
└── MainActivity    NavHost entry point
```

### Key Libraries

| Purpose | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose 2.8.1 |
| Camera | ActivityResultContracts.TakePicture + MediaStore |
| Gallery | ActivityResultContracts.PickVisualMedia |
| Async | Kotlin Coroutines + StateFlow |
| DI | Hilt 2.51.1 |
| HTTP | Retrofit 2.11.0 + Gson |
| Google Sheets | google-api-services-sheets v4 |
| Google Sign-In | play-services-auth 21.2.0 |
| Secure storage | EncryptedSharedPreferences (security-crypto 1.1.0-alpha06) |

---

## Data Flow

### OCR
Image URI → read bytes → base64 → POST Gemini API (gemini-2.5-flash, temp=0.1) → parse JSON → `List<Contact>`

Retry: up to 5x on HTTP 429, 60s countdown per attempt (displayed in UI)

### Sheets
Group contacts by city → for each city: check tab exists → create if not → append rows

### VCF
Per contact: `BEGIN:VCARD / VERSION:3.0 / N:{City}-{Name};;;; / FN:{City}-{Name} / TEL;TYPE=CELL:{mobile} / END:VCARD`

---

## Gemini Prompt

```
Extract all contacts from this image. Each contact must have:
- name (string, transliterate Gujarati to English if needed)
- mobile (exactly 10 digits, digits only, no country code)
- city (string)
Return ONLY a JSON array: [{"name":"...","mobile":"...","city":"..."}]
If a field is missing or unclear, use empty string. No markdown, no explanation.
```

---

## Settings

| Field | Storage |
|---|---|
| Gemini API Key | EncryptedSharedPreferences (`"gemini_api_key"`) |
| Google Account | OAuth token via Credential Manager |
| Spreadsheet ID | EncryptedSharedPreferences (`"spreadsheet_id"`) |

- App routes to Settings on first launch if Gemini key or Spreadsheet ID is missing
- Spreadsheet ID validated by fetching sheet metadata before saving
- Accepts full URL or raw ID (regex extracts ID from URL)
