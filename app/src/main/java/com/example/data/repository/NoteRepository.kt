package com.example.data.repository

import com.example.crypto.CryptoManager
import com.example.data.dao.FolderDao
import com.example.data.dao.NoteDao
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NoteRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao
) {
    val allActiveNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    val pinnedActiveNotes: Flow<List<NoteEntity>> = noteDao.getPinnedActiveNotes()
    val unpinnedActiveNotes: Flow<List<NoteEntity>> = noteDao.getUnpinnedActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val trashNotes: Flow<List<NoteEntity>> = noteDao.getTrashNotes()
    val favoritesNotes: Flow<List<NoteEntity>> = noteDao.getFavoritesNotes()
    val encryptedNotes: Flow<List<NoteEntity>> = noteDao.getEncryptedNotes()
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()

    fun getNotesByFolder(folderId: String): Flow<List<NoteEntity>> = noteDao.getNotesByFolder(folderId)

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    suspend fun getNoteById(id: String): NoteEntity? = noteDao.getNoteById(id)

    suspend fun saveNote(note: NoteEntity) {
        val now = System.currentTimeMillis()
        val toSave = note.copy(
            updatedAt = now,
            syncStatus = "PENDING"
        )
        noteDao.insertNote(toSave)
    }

    suspend fun createNote(
        title: String,
        content: String,
        folderId: String = NoteEntity.DEFAULT_FOLDER_ID,
        folderName: String = "General",
        colorHex: String = "#FFFFFF",
        isPinned: Boolean = false,
        isEncrypted: Boolean = false,
        tags: String = "",
        passphrase: String? = null
    ): NoteEntity {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        var storedTitle = title
        var storedContent = content

        if (isEncrypted && !passphrase.isNullOrBlank()) {
            storedTitle = CryptoManager.encrypt(title, passphrase)
            storedContent = CryptoManager.encrypt(content, passphrase)
        }

        val note = NoteEntity(
            id = id,
            title = storedTitle,
            content = storedContent,
            folderId = folderId,
            folderName = folderName,
            colorHex = colorHex,
            isPinned = isPinned,
            isArchived = false,
            isDeleted = false,
            isEncrypted = isEncrypted,
            tags = tags,
            createdAt = now,
            updatedAt = now,
            syncStatus = "PENDING"
        )
        noteDao.insertNote(note)
        return note
    }

    suspend fun togglePin(noteId: String) {
        val note = noteDao.getNoteById(noteId) ?: return
        noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis(), syncStatus = "PENDING"))
    }

    suspend fun toggleArchive(noteId: String) {
        val note = noteDao.getNoteById(noteId) ?: return
        noteDao.updateNote(note.copy(isArchived = !note.isArchived, isPinned = false, updatedAt = System.currentTimeMillis(), syncStatus = "PENDING"))
    }

    suspend fun moveToTrash(noteId: String) {
        val note = noteDao.getNoteById(noteId) ?: return
        noteDao.updateNote(note.copy(isDeleted = true, isPinned = false, updatedAt = System.currentTimeMillis(), syncStatus = "PENDING"))
    }

    suspend fun restoreFromTrash(noteId: String) {
        val note = noteDao.getNoteById(noteId) ?: return
        noteDao.updateNote(note.copy(isDeleted = false, updatedAt = System.currentTimeMillis(), syncStatus = "PENDING"))
    }

    suspend fun deletePermanently(noteId: String) {
        noteDao.deleteNoteById(noteId)
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    suspend fun updateColor(noteId: String, colorHex: String) {
        val note = noteDao.getNoteById(noteId) ?: return
        noteDao.updateNote(note.copy(colorHex = colorHex, updatedAt = System.currentTimeMillis(), syncStatus = "PENDING"))
    }

    suspend fun moveToFolder(noteId: String, folderId: String, folderName: String) {
        val note = noteDao.getNoteById(noteId) ?: return
        noteDao.updateNote(note.copy(folderId = folderId, folderName = folderName, updatedAt = System.currentTimeMillis(), syncStatus = "PENDING"))
    }

    suspend fun toggleTaskCheckbox(noteId: String, lineText: String, isChecked: Boolean) {
        val note = noteDao.getNoteById(noteId) ?: return
        if (note.isEncrypted) return // encrypted notes are not modified outside active decrypted editor

        val lines = note.content.lines().toMutableList()
        val index = lines.indexOfFirst { it.trim().removePrefix("- [ ] ").removePrefix("- [x] ").trim() == lineText.trim() }
        if (index != -1) {
            val prefix = if (isChecked) "- [x] " else "- [ ] "
            lines[index] = prefix + lineText.trim()
            val newContent = lines.joinToString("\n")
            noteDao.updateNote(note.copy(content = newContent, updatedAt = System.currentTimeMillis(), syncStatus = "PENDING"))
        }
    }

    suspend fun addFolder(name: String, iconName: String = "folder", colorHex: String = "#3B82F6"): FolderEntity {
        val id = UUID.randomUUID().toString()
        val count = folderDao.getAllFoldersList().size
        val folder = FolderEntity(
            id = id,
            name = name.trim(),
            iconName = iconName,
            colorHex = colorHex,
            orderIndex = count,
            isProtected = false
        )
        folderDao.insertFolder(folder)
        return folder
    }

    suspend fun deleteFolder(folderId: String) {
        folderDao.deleteFolderById(folderId)
    }

    suspend fun getAllNotesForExport(): List<NoteEntity> = noteDao.getAllNotes()

    suspend fun getAllFoldersForExport(): List<FolderEntity> = folderDao.getAllFoldersList()

    suspend fun importNotes(notes: List<NoteEntity>) {
        noteDao.insertNotes(notes)
    }

    suspend fun importFolders(folders: List<FolderEntity>) {
        folderDao.insertFolders(folders)
    }
}
