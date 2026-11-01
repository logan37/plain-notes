package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.ui.components.KeepTopBar
import com.example.ui.components.NoteCard
import com.example.ui.components.QuickBottomBar
import com.example.ui.theme.NoteColorPalettes
import com.example.ui.viewmodel.NavigationSection
import com.example.ui.viewmodel.NotesLayoutMode
import com.example.ui.viewmodel.NotesViewModel

@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onOpenDrawer: () -> Unit,
    onOpenSyncCenter: () -> Unit,
    onOpenNoteEditor: (noteId: String?, initialTemplate: String?, isEncrypted: Boolean) -> Unit
) {
    val displayedNotes by viewModel.displayedNotes.collectAsState()
    val folders by viewModel.allFolders.collectAsState()
    val currentSection by viewModel.currentSection.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val selectedFolderName by viewModel.selectedFolderName.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val selectedColorFilter by viewModel.selectedColorFilter.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val feedbackMessage by viewModel.userFeedbackMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val pinnedNotes = remember(displayedNotes) { displayedNotes.filter { it.isPinned } }
    val unpinnedNotes = remember(displayedNotes) { displayedNotes.filter { !it.isPinned } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                KeepTopBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    layoutMode = layoutMode,
                    onToggleLayoutMode = { viewModel.toggleLayoutMode() },
                    onOpenDrawer = onOpenDrawer,
                    isVaultUnlocked = isVaultUnlocked,
                    onToggleVaultLock = {
                        if (isVaultUnlocked) {
                            viewModel.lockVault()
                        } else {
                            viewModel.checkVaultUnlock()
                        }
                    },
                    isSyncing = syncState.isSyncing,
                    onSyncClick = onOpenSyncCenter
                )

                // Horizontal Folder Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = currentSection == NavigationSection.ALL_NOTES,
                            onClick = { viewModel.setSection(NavigationSection.ALL_NOTES) },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_chip_all")
                        )
                    }

                    item {
                        FilterChip(
                            selected = currentSection == NavigationSection.FAVORITES,
                            onClick = { viewModel.setSection(NavigationSection.FAVORITES) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = { Text("Pinned") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_chip_favorites")
                        )
                    }

                    items(folders) { folder ->
                        val isSelected = currentSection == NavigationSection.FOLDER && selectedFolderId == folder.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSection(NavigationSection.FOLDER, folder.id, folder.name) },
                            label = { Text(folder.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_chip_folder_${folder.id}")
                        )
                    }
                }

                // Color Filter Row (if active or expanded)
                if (selectedColorFilter != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Color filter:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        for (palette in NoteColorPalettes) {
                            val isSelected = palette.hex.equals(selectedColorFilter, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) palette.darkColor else palette.lightColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.setColorFilter(palette.hex) }
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.setColorFilter(null) }) {
                            Text("Clear", fontSize = 11.sp)
                        }
                    }
                }

                // Section Banner if in Trash / Archive / Vault
                when (currentSection) {
                    NavigationSection.TRASH -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Notes in Trash",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                TextButton(
                                    onClick = { viewModel.emptyTrash() },
                                    modifier = Modifier.testTag("empty_trash_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Empty Trash", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    NavigationSection.ENCRYPTED_VAULT -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isVaultUnlocked) "Vault Unlocked (AES-256-GCM active)" else "Vault Locked",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                if (!isVaultUnlocked) {
                                    TextButton(onClick = { viewModel.checkVaultUnlock() }) {
                                        Text("Unlock", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        },
        bottomBar = {
            if (currentSection != NavigationSection.TRASH && currentSection != NavigationSection.ARCHIVE) {
                QuickBottomBar(
                    onNewNoteClick = { onOpenNoteEditor(null, null, false) },
                    onNewChecklistClick = { onOpenNoteEditor(null, "- [ ] First task\n- [ ] Second task\n- [ ] ", false) },
                    onNewBulletListClick = { onOpenNoteEditor(null, "- Key point 1\n- Key point 2\n- ", false) },
                    onNewEncryptedNoteClick = {
                        if (!isVaultUnlocked) {
                            viewModel.checkVaultUnlock()
                        } else {
                            onOpenNoteEditor(null, "### 🔒 Encrypted Secret Note\n\n- Sensitive information here", true)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (displayedNotes.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (currentSection) {
                            NavigationSection.TRASH -> Icons.Default.Delete
                            NavigationSection.ARCHIVE -> Icons.Default.Archive
                            NavigationSection.ENCRYPTED_VAULT -> Icons.Default.Lock
                            NavigationSection.FAVORITES -> Icons.Default.PushPin
                            else -> Icons.Default.Lightbulb
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (currentSection) {
                            NavigationSection.TRASH -> "Trash is empty"
                            NavigationSection.ARCHIVE -> "Your archive is empty"
                            NavigationSection.ENCRYPTED_VAULT -> "No encrypted notes yet"
                            NavigationSection.FAVORITES -> "No pinned favorites yet"
                            else -> if (searchQuery.isNotEmpty()) "No notes match \"$searchQuery\"" else "Notes you add appear here"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Encrypted, offline-ready, and synced to your private cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    if (searchQuery.isEmpty() && currentSection == NavigationSection.ALL_NOTES) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onOpenNoteEditor(null, null, false) },
                            modifier = Modifier.testTag("empty_state_create_note_button")
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Note")
                        }
                    }
                }
            }
        } else {
            // Notes Grid or List
            if (layoutMode == NotesLayoutMode.STAGGERED_GRID) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("notes_staggered_grid"),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                    verticalItemSpacing = 10.dp,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pinned Section Header & Items
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = "PINNED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                            )
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                folders = folders,
                                isVaultUnlocked = isVaultUnlocked,
                                onNoteClick = { onOpenNoteEditor(it.id, null, it.isEncrypted) },
                                onTogglePin = { viewModel.togglePin(it) },
                                onToggleArchive = { viewModel.toggleArchive(it) },
                                onMoveToTrash = { viewModel.moveToTrash(it) },
                                onRestoreFromTrash = { viewModel.restoreFromTrash(it) },
                                onDeletePermanently = { viewModel.deletePermanently(it) },
                                onColorSelected = { id, hex -> viewModel.updateNoteColor(id, hex) },
                                onMoveToFolder = { id, folder -> viewModel.moveToFolder(id, folder) },
                                onTaskToggled = { id, line, checked -> viewModel.toggleTaskCheckbox(id, line, checked) }
                            )
                        }

                        if (unpinnedNotes.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = "OTHERS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }

                    // Unpinned Items
                    items(unpinnedNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            folders = folders,
                            isVaultUnlocked = isVaultUnlocked,
                            onNoteClick = { onOpenNoteEditor(it.id, null, it.isEncrypted) },
                            onTogglePin = { viewModel.togglePin(it) },
                            onToggleArchive = { viewModel.toggleArchive(it) },
                            onMoveToTrash = { viewModel.moveToTrash(it) },
                            onRestoreFromTrash = { viewModel.restoreFromTrash(it) },
                            onDeletePermanently = { viewModel.deletePermanently(it) },
                            onColorSelected = { id, hex -> viewModel.updateNoteColor(id, hex) },
                            onMoveToFolder = { id, folder -> viewModel.moveToFolder(id, folder) },
                            onTaskToggled = { id, line, checked -> viewModel.toggleTaskCheckbox(id, line, checked) }
                        )
                    }
                }
            } else {
                // List Mode
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("notes_linear_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pinnedNotes.isNotEmpty()) {
                        item {
                            Text(
                                text = "PINNED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                            )
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                folders = folders,
                                isVaultUnlocked = isVaultUnlocked,
                                onNoteClick = { onOpenNoteEditor(it.id, null, it.isEncrypted) },
                                onTogglePin = { viewModel.togglePin(it) },
                                onToggleArchive = { viewModel.toggleArchive(it) },
                                onMoveToTrash = { viewModel.moveToTrash(it) },
                                onRestoreFromTrash = { viewModel.restoreFromTrash(it) },
                                onDeletePermanently = { viewModel.deletePermanently(it) },
                                onColorSelected = { id, hex -> viewModel.updateNoteColor(id, hex) },
                                onMoveToFolder = { id, folder -> viewModel.moveToFolder(id, folder) },
                                onTaskToggled = { id, line, checked -> viewModel.toggleTaskCheckbox(id, line, checked) }
                            )
                        }

                        if (unpinnedNotes.isNotEmpty()) {
                            item {
                                Text(
                                    text = "OTHERS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }

                    items(unpinnedNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            folders = folders,
                            isVaultUnlocked = isVaultUnlocked,
                            onNoteClick = { onOpenNoteEditor(it.id, null, it.isEncrypted) },
                            onTogglePin = { viewModel.togglePin(it) },
                            onToggleArchive = { viewModel.toggleArchive(it) },
                            onMoveToTrash = { viewModel.moveToTrash(it) },
                            onRestoreFromTrash = { viewModel.restoreFromTrash(it) },
                            onDeletePermanently = { viewModel.deletePermanently(it) },
                            onColorSelected = { id, hex -> viewModel.updateNoteColor(id, hex) },
                            onMoveToFolder = { id, folder -> viewModel.moveToFolder(id, folder) },
                            onTaskToggled = { id, line, checked -> viewModel.toggleTaskCheckbox(id, line, checked) }
                        )
                    }
                }
            }
        }
    }
}
