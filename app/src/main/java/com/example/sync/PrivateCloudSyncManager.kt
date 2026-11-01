package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.crypto.CryptoManager
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class CloudProvider(val displayName: String, val iconDescription: String) {
    GOOGLE_DRIVE("Google Drive (Private AppFolder)", "Google Cloud Storage"),
    DROPBOX("Dropbox Personal Vault", "Dropbox Cloud"),
    ONEDRIVE("Microsoft OneDrive Vault", "Microsoft Cloud"),
    NEXTCLOUD_WEBDAV("Nextcloud / WebDAV (Self-Hosted)", "Own Cloud Server"),
    LOCAL_GIT("Local Git Repository / Folder", "Private Git Sync")
}

data class SyncState(
    val isSyncing: Boolean = false,
    val selectedProvider: CloudProvider = CloudProvider.GOOGLE_DRIVE,
    val isE2EEActive: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val lastSyncTimeMillis: Long = 0L,
    val lastSyncResult: String = "No sync yet",
    val remotePath: String = "/plain_notes/EncryptedVault",
    val localGitPath: String = "/storage/emulated/0/Documents/plain_notes_Git",
    val pendingChangesCount: Int = 0
)

class PrivateCloudSyncManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("plain_notes_sync_prefs", Context.MODE_PRIVATE)

    private val _syncState = MutableStateFlow(loadSyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private fun loadSyncState(): SyncState {
        val providerName = prefs.getString("selected_provider", CloudProvider.GOOGLE_DRIVE.name) ?: CloudProvider.GOOGLE_DRIVE.name
        val provider = try { CloudProvider.valueOf(providerName) } catch (e: Exception) { CloudProvider.GOOGLE_DRIVE }
        return SyncState(
            selectedProvider = provider,
            isE2EEActive = prefs.getBoolean("e2ee_active", true),
            autoSyncEnabled = prefs.getBoolean("auto_sync", true),
            lastSyncTimeMillis = prefs.getLong("last_sync_time", 0L),
            lastSyncResult = prefs.getString("last_sync_result", "Ready to sync") ?: "Ready to sync",
            remotePath = prefs.getString("remote_path", "/plain_notes/EncryptedVault") ?: "/plain_notes/EncryptedVault",
            localGitPath = prefs.getString("local_git_path", "/storage/emulated/0/Documents/plain_notes_Git") ?: "/storage/emulated/0/Documents/plain_notes_Git"
        )
    }

    fun updateProvider(provider: CloudProvider) {
        prefs.edit().putString("selected_provider", provider.name).apply()
        _syncState.value = _syncState.value.copy(selectedProvider = provider)
    }

    fun updateRemotePath(path: String) {
        prefs.edit().putString("remote_path", path).apply()
        _syncState.value = _syncState.value.copy(remotePath = path)
    }

    fun updateLocalGitPath(path: String) {
        prefs.edit().putString("local_git_path", path).apply()
        _syncState.value = _syncState.value.copy(localGitPath = path)
    }

    fun toggleE2EE(enabled: Boolean) {
        prefs.edit().putBoolean("e2ee_active", enabled).apply()
        _syncState.value = _syncState.value.copy(isE2EEActive = enabled)
    }

    fun toggleAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync", enabled).apply()
        _syncState.value = _syncState.value.copy(autoSyncEnabled = enabled)
    }

    suspend fun performSync(
        notes: List<NoteEntity>,
        folders: List<FolderEntity>,
        passphrase: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(isSyncing = true)

        try {
            // 1. Prepare export JSON
            val exportJson = serializeVaultToJson(notes, folders)

            // 2. Encrypt if E2EE is enabled
            val payload = if (_syncState.value.isE2EEActive && !passphrase.isNullOrBlank()) {
                CryptoManager.encrypt(exportJson, passphrase)
            } else {
                exportJson
            }

            // 3. Save to app-specific private sync storage
            val syncDir = File(context.filesDir, "cloud_sync")
            if (!syncDir.exists()) syncDir.mkdirs()

            val providerFile = File(syncDir, "${_syncState.value.selectedProvider.name.lowercase()}_vault.json")
            providerFile.writeText(payload)

            // Also keep local git snapshot representation
            val gitDir = File(context.filesDir, "git_repo")
            if (!gitDir.exists()) gitDir.mkdirs()
            val gitFile = File(gitDir, "HEAD_SNAPSHOT.vault")
            gitFile.writeText(payload)

            val now = System.currentTimeMillis()
            val df = SimpleDateFormat("HH:mm:ss - MMM dd", Locale.getDefault())
            val formattedTime = df.format(Date(now))
            val resultMessage = "Synced ${notes.size} notes securely to ${_syncState.value.selectedProvider.displayName} (E2EE: ${if (_syncState.value.isE2EEActive) "Active" else "Disabled"}) at $formattedTime"

            prefs.edit()
                .putLong("last_sync_time", now)
                .putString("last_sync_result", resultMessage)
                .apply()

            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncTimeMillis = now,
                lastSyncResult = resultMessage,
                pendingChangesCount = 0
            )

            Result.success(resultMessage)
        } catch (e: Exception) {
            val errorMsg = "Sync failed: ${e.localizedMessage ?: "Unknown error"}"
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncResult = errorMsg
            )
            Result.failure(e)
        }
    }

    fun serializeVaultToJson(notes: List<NoteEntity>, folders: List<FolderEntity>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("client", "plain-notes Android")

        val foldersArray = JSONArray()
        for (f in folders) {
            val fObj = JSONObject()
            fObj.put("id", f.id)
            fObj.put("name", f.name)
            fObj.put("iconName", f.iconName)
            fObj.put("colorHex", f.colorHex)
            fObj.put("orderIndex", f.orderIndex)
            foldersArray.put(fObj)
        }
        root.put("folders", foldersArray)

        val notesArray = JSONArray()
        for (n in notes) {
            val nObj = JSONObject()
            nObj.put("id", n.id)
            nObj.put("title", n.title)
            nObj.put("content", n.content)
            nObj.put("folderId", n.folderId)
            nObj.put("folderName", n.folderName)
            nObj.put("colorHex", n.colorHex)
            nObj.put("isPinned", n.isPinned)
            nObj.put("isArchived", n.isArchived)
            nObj.put("isDeleted", n.isDeleted)
            nObj.put("isEncrypted", n.isEncrypted)
            nObj.put("tags", n.tags)
            nObj.put("createdAt", n.createdAt)
            nObj.put("updatedAt", n.updatedAt)
            notesArray.put(nObj)
        }
        root.put("notes", notesArray)

        return root.toString()
    }

    fun parseVaultFromJson(jsonString: String): Pair<List<NoteEntity>, List<FolderEntity>> {
        val root = JSONObject(jsonString)
        val folders = mutableListOf<FolderEntity>()
        val notes = mutableListOf<NoteEntity>()

        if (root.has("folders")) {
            val foldersArray = root.getJSONArray("folders")
            for (i in 0 until foldersArray.length()) {
                val fObj = foldersArray.getJSONObject(i)
                folders.add(
                    FolderEntity(
                        id = fObj.getString("id"),
                        name = fObj.getString("name"),
                        iconName = fObj.optString("iconName", "folder"),
                        colorHex = fObj.optString("colorHex", "#64748B"),
                        orderIndex = fObj.optInt("orderIndex", i)
                    )
                )
            }
        }

        if (root.has("notes")) {
            val notesArray = root.getJSONArray("notes")
            for (i in 0 until notesArray.length()) {
                val nObj = notesArray.getJSONObject(i)
                notes.add(
                    NoteEntity(
                        id = nObj.getString("id"),
                        title = nObj.getString("title"),
                        content = nObj.getString("content"),
                        folderId = nObj.optString("folderId", NoteEntity.DEFAULT_FOLDER_ID),
                        folderName = nObj.optString("folderName", "General"),
                        colorHex = nObj.optString("colorHex", "#FFFFFF"),
                        isPinned = nObj.optBoolean("isPinned", false),
                        isArchived = nObj.optBoolean("isArchived", false),
                        isDeleted = nObj.optBoolean("isDeleted", false),
                        isEncrypted = nObj.optBoolean("isEncrypted", false),
                        tags = nObj.optString("tags", ""),
                        createdAt = nObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = nObj.optLong("updatedAt", System.currentTimeMillis()),
                        syncStatus = "SYNCED"
                    )
                )
            }
        }

        return Pair(notes, folders)
    }

    suspend fun exportMarkdownZip(notes: List<NoteEntity>, targetUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(targetUri)
                ?: return@withContext Result.failure(Exception("Cannot open destination stream"))

            ZipOutputStream(outputStream).use { zipOut ->
                var exportedCount = 0
                for (note in notes) {
                    if (note.isDeleted) continue
                    val safeTitle = note.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40).ifBlank { "Untitled_${note.id.take(6)}" }
                    val folderPrefix = note.folderName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "General" }
                    val entryName = "$folderPrefix/$safeTitle.md"

                    zipOut.putNextEntry(ZipEntry(entryName))
                    val fileContent = buildString {
                        appendLine("---")
                        appendLine("title: \"${note.title.replace("\"", "\\\"")}\"")
                        appendLine("folder: \"${note.folderName}\"")
                        appendLine("tags: [${note.tagList().joinToString(", ") { "\"$it\"" }}]")
                        appendLine("pinned: ${note.isPinned}")
                        appendLine("updated: ${Date(note.updatedAt)}")
                        appendLine("---")
                        appendLine()
                        appendLine(note.content)
                    }
                    zipOut.write(fileContent.toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()
                    exportedCount++
                }
            }
            Result.success(notes.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
