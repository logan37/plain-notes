# plain-notes 🔒📝

> **A privacy-first, zero-knowledge Google Keep alternative with Markdown formatting, folder organization, interactive bullets, and private cloud sync.**

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.2-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Encryption](https://img.shields.io/badge/Security-AES--256--GCM-8B5CF6.svg?style=flat)](https://en.wikipedia.org/wiki/Galois/Counter_Mode)
[![Offline-First](https://img.shields.io/badge/Architecture-100%25%20Offline%20First-10B981.svg?style=flat)](https://developer.android.com/training/data-storage/room)
[![Release](https://img.shields.io/github/v/release/prakankipati/plain-notes?include_prereleases&color=F59E0B)](https://github.com/prakankipati/plain-notes/releases)

---

## 🌟 Overview

**plain-notes** delivers the rapid, intuitive, card-based note taking experience of Google Keep, while solving its biggest drawbacks: **lack of end-to-end encryption, surveillance, and vendor lock-in**.

All notes are stored in a local on-device SQLite/Room database by default. Sensitive notes or entire folders can be secured with client-side **AES-256-GCM** authenticated encryption. Synchronize directly with your preferred private cloud storage (Google Drive, Dropbox, Microsoft OneDrive, Nextcloud / WebDAV) or sync to a **local Git repository** without third-party servers ever accessing your plaintext data.

---

## ✨ Features

### 🗂️ Google Keep Alternative Experience
- **Staggered Grid & List Layouts**: One-tap toggle between responsive masonry cards and single-column list view.
- **Keep Pastel Palette**: Color-code notes with soft pastel tones (Lemon `#FFF9C4`, Peach `#FFE0B2`, Mint `#C8E6C9`, Sage `#B2DFDB`, Sky `#BBDEFB`, Lavender `#E1BEE7`, Coral `#FFCDD2`, Slate `#ECEFF1`, and dark mode variants).
- **Quick Bottom Compose Bar**: Fast-entry bar with *"Take a note..."*, one-tap shortcuts for interactive checklists, bullet lists, and encrypted notes, plus an animated Floating Action Button (`+`).
- **Pinned & Favorites**: Dedicated `PINNED` section keeps vital tasks and notes at the top.
- **Archive & Recycle Bin**: Safe note archiving and Trash with instant restore or permanent deletion.

### 📝 Markdown & Interactive Bullets
- **Rich Markdown Formatting**: Headings (`#`, `##`, `###`), bold (`**bold**`), italic (`*italic*`), inline code (`` `code` ``), blockquotes (`> quote`), and horizontal dividers (`---`).
- **Interactive Checklists**: `- [ ]` and `- [x]` task items can be checked or unchecked directly on the note cards or in the live markdown preview without opening the raw text editor.
- **Hierarchical Bullet Lists**: Automatic indentation and bullet continuations (`- `, `* `).
- **Dedicated Formatting Toolbar**: Quick-action bar for one-tap Markdown insertions.
- **Real-Time Word & Character Statistics**: Live metrics displayed at the bottom of the editor.

### 🔒 Zero-Knowledge End-to-End Encryption (E2EE)
- **AES-256-GCM Authenticated Encryption**: Military-grade cipher ensuring confidentiality and cryptographic integrity verification.
- **PBKDF2 SHA-256 Key Derivation**: 10,000 rounds with cryptographically secure random salts and 12-byte initialization vectors (IVs).
- **Client-Side Only**: Keys and plaintexts remain in-memory only during unlocked sessions; only ciphertext is written to storage or cloud targets.
- **Encrypted Vault Mode**: Set a master passphrase with optional hint and configurable session auto-lock.

### 🌐 Private Cloud & Local Git Sync
- **Multi-Cloud Support**:
  - **Google Drive** (via Private AppData folder)
  - **Dropbox** Personal Vault
  - **Microsoft OneDrive**
  - **Nextcloud / WebDAV** (Self-hosted personal cloud servers)
  - **Local Git Repository**
- **Client-Side Pre-Encryption**: Backups and sync payloads are encrypted with your master key *before* leaving your device. Cloud providers never hold decryption keys.
- **Open Data Portability**:
  - **Export Markdown `.zip`**: Exports notes as clean `.md` files with YAML frontmatter metadata, immediately compatible with local Git commits, **Obsidian**, or **Logseq**.
  - **Encrypted Vault Snapshots**: Complete JSON backup export and import for seamless migration.

### ⚡ 100% Offline-First
- Instant local performance backed by **Jetpack Room (SQLite)**.
- Full functionality without an internet connection.
- Background sync queue that automatically synchronizes when network connectivity is available.

---

## 🛡️ Security Architecture

| Component | Specification |
|---|---|
| **Cipher Algorithm** | AES-256 in Galois/Counter Mode (GCM) with 128-bit authentication tag |
| **Key Derivation** | PBKDF2 with HMAC-SHA256 (10,000 iterations, 256-bit key) |
| **Salt & IV** | 16-byte random salt + 12-byte random IV via `java.security.SecureRandom` |
| **Cloud Storage** | Zero-knowledge ciphertext blobs (`plain_notes_vault.json`) |
| **Local Storage** | Encrypted payloads stored in SQLite / Room with hardware Keystore support |

---

## 🏗️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/) (100%)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
- **Local Persistence**: [Jetpack Room](https://developer.android.com/training/data-storage/room) & SQLite
- **Architecture**: MVVM + Clean Architecture + StateFlow
- **Cryptography**: Standard Java Cryptography Architecture (`javax.crypto.Cipher`, `PBEKeySpec`, `SecretKeyFactory`)
- **Testing**: [Robolectric](https://robolectric.org/) + JUnit4 (Local JVM execution)

---

## 🚀 Building & Running

### Prerequisites
- JDK 17 or higher
- Android SDK (API Level 36 / Android 15+)
- Gradle 8.x+

### Quick Build Commands

```bash
# Clone the repository
git clone https://github.com/prakankipati/plain-notes.git
cd plain-notes

# Run unit and Robolectric tests
gradle :app:testDebugUnitTest

# Assemble Debug APK (outputs to app/build/outputs/apk/debug/)
gradle :app:assembleDebug

# Assemble Release APK
gradle :app:assembleRelease
```

---

## 📦 GitHub Releases & CI/CD

This repository includes an automated GitHub Actions release pipeline (`.github/workflows/release.yml`).

### How Releases Work:
1. **Automated on Tag**: Pushing any tag matching `v*` (e.g. `git tag v1.0.0 && git push origin v1.0.0`) automatically:
   - Sets up the build environment
   - Runs all verification tests
   - Compiles the release APK (`plain-notes-vX.X.X.apk`)
   - Generates release notes
   - Publishes a new GitHub Release with the attached APK!
2. **Manual Dispatch**: You can also trigger a release build manually from the GitHub **Actions** tab by selecting the "Build & Publish Release" workflow.

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
You may obtain a copy of the License at `http://www.apache.org/licenses/LICENSE-2.0`.
