package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoManager
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.markdown.MarkdownRenderer
import com.example.ui.theme.NoteColorPalettes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: NoteEntity,
    folders: List<FolderEntity>,
    isVaultUnlocked: Boolean,
    onNoteClick: (NoteEntity) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleArchive: (String) -> Unit,
    onMoveToTrash: (String) -> Unit,
    onRestoreFromTrash: (String) -> Unit,
    onDeletePermanently: (String) -> Unit,
    onColorSelected: (String, String) -> Unit,
    onMoveToFolder: (String, FolderEntity) -> Unit,
    onTaskToggled: (String, String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val palette = NoteColorPalettes.firstOrNull { it.hex.equals(note.colorHex, ignoreCase = true) }
        ?: NoteColorPalettes.first()
    val cardColor = if (isDark) palette.darkColor else palette.lightColor
    val onCardTextColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    var showMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }

    // Content resolution for encrypted notes
    val displayTitle: String
    val displayContent: String
    val isDecrypted: Boolean

    if (note.isEncrypted) {
        val passphrase = CryptoManager.getActivePassphrase()
        if (passphrase != null && isVaultUnlocked) {
            displayTitle = CryptoManager.decrypt(note.title, passphrase).getOrDefault(note.title)
            displayContent = CryptoManager.decrypt(note.content, passphrase).getOrDefault(note.content)
            isDecrypted = true
        } else {
            displayTitle = "🔒 Encrypted Note"
            displayContent = "Unlock your Vault to decrypt and view this note's markdown content."
            isDecrypted = false
        }
    } else {
        displayTitle = note.title
        displayContent = note.content
        isDecrypted = true
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNoteClick(note) }
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title and Pin/Favorite button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (displayTitle.isNotBlank()) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = onCardTextColor
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (!note.isDeleted) {
                    IconButton(
                        onClick = { onTogglePin(note.id) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("pin_note_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin note" else "Pin note",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else onCardTextColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Markdown Snippet & Content
            if (displayContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                MarkdownRenderer(
                    content = displayContent,
                    textColor = onCardTextColor,
                    maxLines = 8,
                    interactiveCheckboxes = isDecrypted,
                    onCheckboxToggled = { lineText, isChecked ->
                        onTaskToggled(note.id, lineText, isChecked)
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Chips: Folder, Tags, Encrypted badge
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Folder Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = onCardTextColor.copy(alpha = 0.08f),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = onCardTextColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = note.folderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = onCardTextColor.copy(alpha = 0.8f)
                        )
                    }
                }

                // Encrypted badge
                if (note.isEncrypted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AES-256 E2EE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Tags
                for (tag in note.tagList().take(3)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = onCardTextColor.copy(alpha = 0.05f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = if (tag.startsWith("#")) tag else "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = onCardTextColor.copy(alpha = 0.65f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Footer: Timestamp & Action Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                Text(
                    text = dateFormat.format(Date(note.updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = onCardTextColor.copy(alpha = 0.5f)
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("note_options_${note.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Note actions",
                            tint = onCardTextColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                            showColorPicker = false
                            showFolderPicker = false
                        }
                    ) {
                        if (note.isDeleted) {
                            DropdownMenuItem(
                                text = { Text("Restore Note") },
                                onClick = {
                                    showMenu = false
                                    onRestoreFromTrash(note.id)
                                },
                                leadingIcon = { Icon(Icons.Default.RestoreFromTrash, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDeletePermanently(note.id)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Change Color") },
                                onClick = { showColorPicker = !showColorPicker },
                                leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null) }
                            )

                            if (showColorPicker) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (p in NoteColorPalettes.take(5)) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isDark) p.darkColor else p.lightColor)
                                                .clickable {
                                                    onColorSelected(note.id, p.hex)
                                                    showColorPicker = false
                                                    showMenu = false
                                                }
                                        )
                                    }
                                }
                            }

                            DropdownMenuItem(
                                text = { Text("Move to Folder") },
                                onClick = { showFolderPicker = !showFolderPicker },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                            )

                            if (showFolderPicker) {
                                for (folder in folders) {
                                    DropdownMenuItem(
                                        text = { Text(folder.name, fontSize = 13.sp) },
                                        onClick = {
                                            onMoveToFolder(note.id, folder)
                                            showFolderPicker = false
                                            showMenu = false
                                        }
                                    )
                                }
                            }

                            DropdownMenuItem(
                                text = { Text(if (note.isArchived) "Unarchive" else "Archive") },
                                onClick = {
                                    showMenu = false
                                    onToggleArchive(note.id)
                                },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onMoveToTrash(note.id)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
