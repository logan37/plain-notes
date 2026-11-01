package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.NoteEntity
import com.example.ui.components.FolderDrawer
import com.example.ui.components.SetupVaultDialog
import com.example.ui.components.UnlockVaultDialog
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.NotesListScreen
import com.example.ui.screens.SyncSettingsScreen
import com.example.ui.theme.VaultKeepTheme
import com.example.ui.viewmodel.NavigationSection
import com.example.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

enum class AppScreen {
    LIST,
    EDITOR,
    SYNC
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaultKeepTheme {
                VaultKeepApp()
            }
        }
    }
}

@Composable
fun VaultKeepApp(viewModel: NotesViewModel = viewModel()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(AppScreen.LIST) }
    var activeNoteForEditing by remember { mutableStateOf<NoteEntity?>(null) }
    var initialEditorTemplate by remember { mutableStateOf<String?>(null) }
    var initialEditorEncrypted by remember { mutableStateOf(false) }

    val folders by viewModel.allFolders.collectAsState()
    val currentSection by viewModel.currentSection.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val showUnlockDialog by viewModel.showUnlockDialog.collectAsState()
    val showSetupVaultDialog by viewModel.showSetupVaultDialog.collectAsState()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                FolderDrawer(
                    currentSection = currentSection,
                    selectedFolderId = selectedFolderId,
                    folders = folders,
                    isVaultUnlocked = isVaultUnlocked,
                    onSelectSection = { section, folderId, folderName ->
                        scope.launch { drawerState.close() }
                        if (section == NavigationSection.SYNC_CENTER) {
                            currentScreen = AppScreen.SYNC
                        } else {
                            currentScreen = AppScreen.LIST
                            viewModel.setSection(section, folderId, folderName)
                        }
                    },
                    onAddFolder = { name -> viewModel.addFolder(name) },
                    onDeleteFolder = { id -> viewModel.deleteFolder(id) }
                )
            }
        }
    ) {
        when (currentScreen) {
            AppScreen.LIST -> {
                NotesListScreen(
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenSyncCenter = { currentScreen = AppScreen.SYNC },
                    onOpenNoteEditor = { noteId, template, isEncrypted ->
                        initialEditorTemplate = template
                        initialEditorEncrypted = isEncrypted
                        if (noteId != null) {
                            scope.launch {
                                activeNoteForEditing = viewModel.getNoteForEditing(noteId)
                                currentScreen = AppScreen.EDITOR
                            }
                        } else {
                            activeNoteForEditing = null
                            currentScreen = AppScreen.EDITOR
                        }
                    }
                )
            }

            AppScreen.EDITOR -> {
                val effectiveInitialNote = remember(activeNoteForEditing, initialEditorTemplate, initialEditorEncrypted) {
                    activeNoteForEditing ?: if (initialEditorTemplate != null || initialEditorEncrypted) {
                        NoteEntity(
                            id = "",
                            title = "",
                            content = initialEditorTemplate ?: "",
                            folderId = selectedFolderId,
                            folderName = folders.firstOrNull { it.id == selectedFolderId }?.name ?: "General",
                            isEncrypted = initialEditorEncrypted
                        )
                    } else null
                }

                NoteEditorScreen(
                    initialNote = effectiveInitialNote,
                    folders = folders,
                    isVaultUnlocked = isVaultUnlocked,
                    onRequireUnlock = { viewModel.checkVaultUnlock() },
                    onSaveNote = { id, title, content, folderId, folderName, colorHex, isPinned, isEncrypted, tags ->
                        viewModel.saveNote(
                            id = id,
                            title = title,
                            content = content,
                            folderId = folderId,
                            folderName = folderName,
                            colorHex = colorHex,
                            isPinned = isPinned,
                            isEncrypted = isEncrypted,
                            tags = tags
                        )
                    },
                    onDeleteNote = { noteId ->
                        viewModel.moveToTrash(noteId)
                    },
                    onBack = {
                        currentScreen = AppScreen.LIST
                        activeNoteForEditing = null
                        initialEditorTemplate = null
                    }
                )
            }

            AppScreen.SYNC -> {
                BackHandler {
                    currentScreen = AppScreen.LIST
                }
                SyncSettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = AppScreen.LIST }
                )
            }
        }
    }

    if (showUnlockDialog) {
        UnlockVaultDialog(
            onUnlock = { passphrase ->
                viewModel.unlockVault(passphrase)
            },
            onDismiss = {
                viewModel.dismissVaultDialogs()
            }
        )
    }

    if (showSetupVaultDialog) {
        SetupVaultDialog(
            onSetup = { passphrase, hint ->
                viewModel.setupVault(passphrase, hint)
            },
            onDismiss = {
                viewModel.dismissVaultDialogs()
            }
        )
    }
}
