package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FolderDao
import com.example.data.dao.NoteDao
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [NoteEntity::class, FolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vaultkeep_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(getInstance(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(database: VaultDatabase) {
            val folderDao = database.folderDao()
            val noteDao = database.noteDao()

            val defaultFolders = listOf(
                FolderEntity(
                    id = "general",
                    name = "General",
                    iconName = "folder",
                    colorHex = "#64748B",
                    orderIndex = 0,
                    isProtected = true
                ),
                FolderEntity(
                    id = "work",
                    name = "Work & Projects",
                    iconName = "work",
                    colorHex = "#3B82F6",
                    orderIndex = 1
                ),
                FolderEntity(
                    id = "ideas",
                    name = "Creative Ideas",
                    iconName = "lightbulb",
                    colorHex = "#EAB308",
                    orderIndex = 2
                ),
                FolderEntity(
                    id = "personal",
                    name = "Personal",
                    iconName = "person",
                    colorHex = "#10B981",
                    orderIndex = 3
                ),
                FolderEntity(
                    id = "vault",
                    name = "Encrypted Vault",
                    iconName = "lock",
                    colorHex = "#8B5CF6",
                    orderIndex = 4,
                    isProtected = true
                )
            )
            folderDao.insertFolders(defaultFolders)

            val now = System.currentTimeMillis()
            val initialNotes = listOf(
                NoteEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Welcome to plain-notes! 🔒",
                    content = """
# Welcome to plain-notes

Your privacy-first, encrypted alternative to Google Keep.

### ✨ Key Features
- **Offline-First:** All notes stored locally in high-performance SQLite/Room database
- **Markdown & Bullets:** Full support for rich text, lists, and checklists
- **Folder Organization:** Group your thoughts into folders & labels
- **Zero-Knowledge Encryption:** End-to-end AES-256-GCM encryption with PBKDF2 derivation
- **Private Cloud Sync:** Backup and sync directly to Google Drive, Dropbox, OneDrive, or local Git!

### 📝 Interactive Checklist
- [x] Download and install plain-notes
- [ ] Try creating your first bullet list
- [ ] Tap the lock icon to test AES-256 vault encryption
- [ ] Explore private cloud sync settings

*Tip: Tap any checkbox to toggle its state!*
                    """.trimIndent(),
                    folderId = "general",
                    folderName = "General",
                    colorHex = "#FFF9C4", // Keep soft yellow
                    isPinned = true,
                    isArchived = false,
                    isDeleted = false,
                    isEncrypted = false,
                    tags = "welcome, guide, keep",
                    createdAt = now - 60000,
                    updatedAt = now - 60000
                ),
                NoteEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Markdown Quick Cheatsheet 💡",
                    content = """
### Markdown Formatting Guide

VaultKeep makes notes readable and structured:

- **Bold text**: `**bold**`
- *Italic text*: `*italic*`
- `Code blocks`: `` `inline code` ``
- Quotes: `> Knowledge is power`

#### Bullet Points:
- Idea brainstorming
- Team meetings
- Reading notes

#### Task List:
- [ ] Review sprint backlog
- [x] Push code commit to local Git
- [ ] Backup encrypted vault to private cloud
                    """.trimIndent(),
                    folderId = "ideas",
                    folderName = "Creative Ideas",
                    colorHex = "#C8E6C9", // Keep soft mint
                    isPinned = true,
                    isArchived = false,
                    isDeleted = false,
                    isEncrypted = false,
                    tags = "cheatsheet, markdown",
                    createdAt = now - 30000,
                    updatedAt = now - 30000
                ),
                NoteEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Private Cloud & Git Sync 🌐",
                    content = """
### Private Cloud Architecture

Keep your notes private with zero surveillance:
- **No Third-Party Tracking**: plain-notes connects directly to your private cloud storage (Google Drive, Dropbox, OneDrive, NextCloud/WebDAV).
- **Client-Side Encryption**: Note payloads are encrypted *before* leaving your device. Even your cloud provider cannot read your files.
- **Local Git Storage**: Notes can be exported as structured markdown or encrypted JSON bundles to commit straight to your local or private Git repositories.
                    """.trimIndent(),
                    folderId = "work",
                    folderName = "Work & Projects",
                    colorHex = "#BBDEFB", // Keep soft sky blue
                    isPinned = false,
                    isArchived = false,
                    isDeleted = false,
                    isEncrypted = false,
                    tags = "sync, cloud, git, privacy",
                    createdAt = now,
                    updatedAt = now
                )
            )
            noteDao.insertNotes(initialNotes)
        }
    }
}
