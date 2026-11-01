package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoManager
import com.example.data.VaultDatabase
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.repository.NoteRepository
import com.example.sync.CloudProvider
import com.example.sync.PrivateCloudSyncManager
import com.example.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavigationSection {
    ALL_NOTES,
    FAVORITES,
    FOLDER,
    ENCRYPTED_VAULT,
    ARCHIVE,
    TRASH,
    SYNC_CENTER
}

enum class NotesLayoutMode {
    STAGGERED_GRID,
    LIST
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = VaultDatabase.getInstance(application)
    private val repository = NoteRepository(database.noteDao(), database.folderDao())
    val syncManager = PrivateCloudSyncManager(application)

    private val _currentSection = MutableStateFlow(NavigationSection.ALL_NOTES)
    val currentSection: StateFlow<NavigationSection> = _currentSection.asStateFlow()

    private val _selectedFolderId = MutableStateFlow(NoteEntity.DEFAULT_FOLDER_ID)
    val selectedFolderId: StateFlow<String> = _selectedFolderId.asStateFlow()

    private val _selectedFolderName = MutableStateFlow("General")
    val selectedFolderName: StateFlow<String> = _selectedFolderName.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _layoutMode = MutableStateFlow(NotesLayoutMode.STAGGERED_GRID)
    val layoutMode: StateFlow<NotesLayoutMode> = _layoutMode.asStateFlow()

    private val _selectedColorFilter = MutableStateFlow<String?>(null)
    val selectedColorFilter: StateFlow<String?> = _selectedColorFilter.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(CryptoManager.isVaultUnlocked())
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _showUnlockDialog = MutableStateFlow(false)
    val showUnlockDialog: StateFlow<Boolean> = _showUnlockDialog.asStateFlow()

    private val _showSetupVaultDialog = MutableStateFlow(false)
    val showSetupVaultDialog: StateFlow<Boolean> = _showSetupVaultDialog.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    val allFolders: StateFlow<List<FolderEntity>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncState: StateFlow<SyncState> = syncManager.syncState

    private val sectionNotes = combine(
        repository.allActiveNotes,
        repository.archivedNotes,
        repository.trashNotes,
        repository.favoritesNotes,
        _currentSection
    ) { all, archived, trash, favorites, section ->
        when (section) {
            NavigationSection.ALL_NOTES -> all
            NavigationSection.FAVORITES -> favorites
            NavigationSection.ARCHIVE -> archived
            NavigationSection.TRASH -> trash
            else -> all
        }
    }

    private val filterPair = combine(_searchQuery, _selectedColorFilter) { q, c -> q to c }

    val displayedNotes: StateFlow<List<NoteEntity>> = combine(
        sectionNotes,
        repository.encryptedNotes,
        _currentSection,
        _selectedFolderId,
        filterPair
    ) { baseNotes, encrypted, section, folderId, (query, colorFilter) ->
        val list = when (section) {
            NavigationSection.FOLDER -> baseNotes.filter { it.folderId == folderId }
            NavigationSection.ENCRYPTED_VAULT -> encrypted
            NavigationSection.SYNC_CENTER -> emptyList()
            else -> baseNotes
        }

        list.filter { note ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                note.title.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true) ||
                        note.tags.contains(query, ignoreCase = true) ||
                        note.folderName.contains(query, ignoreCase = true)
            }
            val matchesColor = colorFilter == null || note.colorHex.equals(colorFilter, ignoreCase = true)
            matchesQuery && matchesColor
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSection(section: NavigationSection, folderId: String = NoteEntity.DEFAULT_FOLDER_ID, folderName: String = "General") {
        _currentSection.value = section
        _selectedFolderId.value = folderId
        _selectedFolderName.value = folderName
        if (section == NavigationSection.ENCRYPTED_VAULT) {
            checkVaultUnlock()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleLayoutMode() {
        _layoutMode.value = if (_layoutMode.value == NotesLayoutMode.STAGGERED_GRID) {
            NotesLayoutMode.LIST
        } else {
            NotesLayoutMode.STAGGERED_GRID
        }
    }

    fun setColorFilter(colorHex: String?) {
        _selectedColorFilter.value = if (_selectedColorFilter.value == colorHex) null else colorHex
    }

    fun checkVaultUnlock() {
        val configured = CryptoManager.isVaultConfigured(getApplication())
        if (!configured) {
            _showSetupVaultDialog.value = true
        } else if (!CryptoManager.isVaultUnlocked()) {
            _showUnlockDialog.value = true
        } else {
            _isVaultUnlocked.value = true
        }
    }

    fun unlockVault(passphrase: String): Boolean {
        val success = CryptoManager.unlockVault(getApplication(), passphrase)
        if (success) {
            _isVaultUnlocked.value = true
            _showUnlockDialog.value = false
            showMessage("Vault unlocked with AES-256-GCM")
        } else {
            showMessage("Incorrect master passphrase")
        }
        return success
    }

    fun setupVault(passphrase: String, hint: String): Boolean {
        val success = CryptoManager.setupVault(getApplication(), passphrase, hint)
        if (success) {
            _isVaultUnlocked.value = true
            _showSetupVaultDialog.value = false
            showMessage("Vault configured and unlocked with AES-256-GCM")
        } else {
            showMessage("Passphrase must be at least 4 characters")
        }
        return success
    }

    fun lockVault() {
        CryptoManager.lockVault()
        _isVaultUnlocked.value = false
        showMessage("Encrypted vault locked")
    }

    fun dismissVaultDialogs() {
        _showUnlockDialog.value = false
        _showSetupVaultDialog.value = false
    }

    fun togglePin(noteId: String) {
        viewModelScope.launch {
            repository.togglePin(noteId)
        }
    }

    fun toggleArchive(noteId: String) {
        viewModelScope.launch {
            repository.toggleArchive(noteId)
            showMessage("Note moved")
        }
    }

    fun moveToTrash(noteId: String) {
        viewModelScope.launch {
            repository.moveToTrash(noteId)
            showMessage("Note moved to Trash")
        }
    }

    fun restoreFromTrash(noteId: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(noteId)
            showMessage("Note restored")
        }
    }

    fun deletePermanently(noteId: String) {
        viewModelScope.launch {
            repository.deletePermanently(noteId)
            showMessage("Note deleted permanently")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            showMessage("Trash emptied")
        }
    }

    fun updateNoteColor(noteId: String, colorHex: String) {
        viewModelScope.launch {
            repository.updateColor(noteId, colorHex)
        }
    }

    fun moveToFolder(noteId: String, folder: FolderEntity) {
        viewModelScope.launch {
            repository.moveToFolder(noteId, folder.id, folder.name)
            showMessage("Moved to ${folder.name}")
        }
    }

    fun toggleTaskCheckbox(noteId: String, lineText: String, isChecked: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCheckbox(noteId, lineText, isChecked)
        }
    }

    fun saveNote(
        id: String?,
        title: String,
        content: String,
        folderId: String,
        folderName: String,
        colorHex: String,
        isPinned: Boolean,
        isEncrypted: Boolean,
        tags: String
    ) {
        viewModelScope.launch {
            val passphrase = CryptoManager.getActivePassphrase()
            if (isEncrypted && passphrase == null) {
                showMessage("Unlock vault first to encrypt notes")
                return@launch
            }

            if (id == null) {
                repository.createNote(
                    title = title,
                    content = content,
                    folderId = folderId,
                    folderName = folderName,
                    colorHex = colorHex,
                    isPinned = isPinned,
                    isEncrypted = isEncrypted,
                    tags = tags,
                    passphrase = passphrase
                )
                showMessage("Note saved")
            } else {
                val existing = repository.getNoteById(id)
                var finalTitle = title
                var finalContent = content

                if (isEncrypted && passphrase != null) {
                    finalTitle = CryptoManager.encrypt(title, passphrase)
                    finalContent = CryptoManager.encrypt(content, passphrase)
                }

                val updated = (existing ?: NoteEntity(id = id, title = title, content = content)).copy(
                    title = finalTitle,
                    content = finalContent,
                    folderId = folderId,
                    folderName = folderName,
                    colorHex = colorHex,
                    isPinned = isPinned,
                    isEncrypted = isEncrypted,
                    tags = tags,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = "PENDING"
                )
                repository.saveNote(updated)
                showMessage("Note updated")
            }

            // Trigger auto-sync in background if enabled
            if (syncState.value.autoSyncEnabled) {
                triggerSync()
            }
        }
    }

    fun addFolder(name: String, colorHex: String = "#3B82F6") {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val folder = repository.addFolder(name, colorHex = colorHex)
                showMessage("Folder '${folder.name}' created")
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            if (_selectedFolderId.value == folderId) {
                setSection(NavigationSection.ALL_NOTES)
            }
            showMessage("Folder deleted")
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            val notes = repository.getAllNotesForExport()
            val folders = repository.getAllFoldersForExport()
            val passphrase = CryptoManager.getActivePassphrase()

            val result = syncManager.performSync(notes, folders, passphrase)
            result.onSuccess { msg ->
                showMessage(msg)
            }.onFailure { err ->
                showMessage("Sync error: ${err.message}")
            }
        }
    }

    fun updateCloudProvider(provider: CloudProvider) {
        syncManager.updateProvider(provider)
        showMessage("Provider set to ${provider.displayName}")
    }

    fun toggleSyncE2EE(enabled: Boolean) {
        syncManager.toggleE2EE(enabled)
    }

    fun toggleAutoSync(enabled: Boolean) {
        syncManager.toggleAutoSync(enabled)
    }

    fun exportMarkdownZip(targetUri: Uri) {
        viewModelScope.launch {
            val notes = repository.getAllNotesForExport()
            val result = syncManager.exportMarkdownZip(notes, targetUri)
            result.onSuccess { count ->
                showMessage("Exported $count markdown notes successfully")
            }.onFailure { err ->
                showMessage("Export failed: ${err.message}")
            }
        }
    }

    fun importVaultJson(jsonString: String) {
        viewModelScope.launch {
            try {
                // If payload is encrypted VK1, attempt decrypt
                val decrypted = if (jsonString.startsWith("VK1:")) {
                    val passphrase = CryptoManager.getActivePassphrase()
                    if (passphrase == null) {
                        showMessage("Unlock vault first to decrypt import payload")
                        return@launch
                    }
                    val decResult = CryptoManager.decrypt(jsonString, passphrase)
                    decResult.getOrThrow()
                } else {
                    jsonString
                }

                val (importedNotes, importedFolders) = syncManager.parseVaultFromJson(decrypted)
                repository.importFolders(importedFolders)
                repository.importNotes(importedNotes)
                showMessage("Imported ${importedNotes.size} notes and ${importedFolders.size} folders")
            } catch (e: Exception) {
                showMessage("Import failed: ${e.message}")
            }
        }
    }

    fun showMessage(msg: String) {
        _userFeedbackMessage.value = msg
    }

    fun clearMessage() {
        _userFeedbackMessage.value = null
    }

    suspend fun getNoteForEditing(id: String): NoteEntity? {
        val note = repository.getNoteById(id) ?: return null
        if (note.isEncrypted) {
            val passphrase = CryptoManager.getActivePassphrase()
            if (passphrase != null) {
                val decTitle = CryptoManager.decrypt(note.title, passphrase).getOrDefault(note.title)
                val decContent = CryptoManager.decrypt(note.content, passphrase).getOrDefault(note.content)
                return note.copy(title = decTitle, content = decContent)
            }
        }
        return note
    }
}
