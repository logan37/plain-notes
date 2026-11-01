# Release Notes: plain-notes v1.0.0

## Initial Public Release (v1.0.0) - October 2026

Welcome to the initial release of **plain-notes**, a privacy-first, zero-knowledge Google Keep alternative for Android!

### 🌟 Key Highlights

- **Keep-Style Interface**:
  - Masonry staggered grid and single-column list views.
  - Signature pastel color themes (Lemon, Peach, Mint, Sage, Sky, Lavender, Coral, Slate, and Dark theme equivalents).
  - Quick bottom compose bar with direct actions for checklists, bullets, and encrypted notes.
  - Dedicated `PINNED` and `OTHERS` sections with archive and recycle bin management.

- **Markdown & Interactive Bullets**:
  - Live markdown rendering for headings, bold, italic, quotes, inline code, and horizontal rules.
  - Interactive checklists (`- [ ]` / `- [x]`): Tap checkboxes directly from the note cards or in the editor without modifying raw markdown text.
  - Bullets and hierarchical lists with automatic prefix continuations.

- **Zero-Knowledge Encryption (AES-256-GCM)**:
  - Client-side authenticated encryption using 256-bit AES-GCM.
  - Key derivation powered by PBKDF2 with HMAC-SHA-256 (10,000 rounds) and random 16-byte salt / 12-byte IV.
  - Master passphrase vault lock with session timeout.

- **Private Cloud & Local Git Sync**:
  - Direct integration configurations for Google Drive, Dropbox, Microsoft OneDrive, and Nextcloud/WebDAV.
  - Client-side pre-encryption ensures zero plaintext data is shared with cloud hosts.
  - Markdown `.zip` export with YAML frontmatter, ready to commit directly to your local or private Git repositories, Obsidian, or Logseq.
  - Encrypted JSON vault backup export and import.

### 📦 Artifacts
- `plain-notes-v1.0.0.apk` (Universal release binary)
