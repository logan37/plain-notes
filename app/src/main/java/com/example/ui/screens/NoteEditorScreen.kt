package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.markdown.MarkdownRenderer
import com.example.ui.theme.NoteColorPalettes

@Composable
fun NoteEditorScreen(
    initialNote: NoteEntity?,
    folders: List<FolderEntity>,
    isVaultUnlocked: Boolean,
    onRequireUnlock: () -> Unit,
    onSaveNote: (
        id: String?,
        title: String,
        content: String,
        folderId: String,
        folderName: String,
        colorHex: String,
        isPinned: Boolean,
        isEncrypted: Boolean,
        tags: String
    ) -> Unit,
    onDeleteNote: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()

    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var contentValue by remember {
        mutableStateOf(TextFieldValue(initialNote?.content ?: ""))
    }
    var selectedFolder by remember {
        mutableStateOf(
            folders.firstOrNull { it.id == initialNote?.folderId }
                ?: folders.firstOrNull()
                ?: FolderEntity("general", "General")
        )
    }
    var colorHex by remember { mutableStateOf(initialNote?.colorHex ?: "#FFFFFF") }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var isEncrypted by remember { mutableStateOf(initialNote?.isEncrypted ?: false) }
    var tagsText by remember { mutableStateOf(initialNote?.tags ?: "") }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Edit, 1: Markdown Preview
    var showColorPalette by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val currentPalette = NoteColorPalettes.firstOrNull { it.hex.equals(colorHex, ignoreCase = true) }
        ?: NoteColorPalettes.first()
    val screenBackground = if (isDark) currentPalette.darkColor else currentPalette.lightColor
    val onScreenTextColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)

    // Save helper
    fun saveAndExit() {
        if (title.isNotBlank() || contentValue.text.isNotBlank()) {
            onSaveNote(
                initialNote?.id,
                title.trim(),
                contentValue.text,
                selectedFolder.id,
                selectedFolder.name,
                colorHex,
                isPinned,
                isEncrypted,
                tagsText.trim()
            )
        }
        onBack()
    }

    BackHandler {
        saveAndExit()
    }

    // Markdown text insertion helper
    fun insertMarkdown(prefix: String, suffix: String = "") {
        val text = contentValue.text
        val selection = contentValue.selection
        val selectedText = text.substring(selection.min, selection.max)
        val newText = text.replaceRange(selection.min, selection.max, "$prefix$selectedText$suffix")
        val newCursor = selection.min + prefix.length + selectedText.length
        contentValue = TextFieldValue(newText, TextRange(newCursor))
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        color = screenBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { saveAndExit() },
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Save and go back",
                        tint = onScreenTextColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Encryption Toggle
                    IconButton(
                        onClick = {
                            if (!isVaultUnlocked) {
                                onRequireUnlock()
                            } else {
                                isEncrypted = !isEncrypted
                            }
                        },
                        modifier = Modifier.testTag("editor_encrypt_toggle")
                    ) {
                        Icon(
                            imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (isEncrypted) "Encrypted Note" else "Plain Note",
                            tint = if (isEncrypted) MaterialTheme.colorScheme.tertiary else onScreenTextColor.copy(alpha = 0.6f)
                        )
                    }

                    // Pin Toggle
                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier.testTag("editor_pin_toggle")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin" else "Pin",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else onScreenTextColor.copy(alpha = 0.6f)
                        )
                    }

                    // Color Picker
                    IconButton(
                        onClick = { showColorPalette = !showColorPalette },
                        modifier = Modifier.testTag("editor_color_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = "Change color",
                            tint = onScreenTextColor.copy(alpha = 0.7f)
                        )
                    }

                    // Done / Save Button
                    IconButton(
                        onClick = { saveAndExit() },
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = onScreenTextColor.copy(alpha = 0.7f)
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Note") },
                                onClick = {
                                    showMoreMenu = false
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TITLE, title)
                                        putExtra(Intent.EXTRA_TEXT, "$title\n\n${contentValue.text}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share note"))
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Copy Markdown") },
                                onClick = {
                                    showMoreMenu = false
                                    clipboardManager.setText(AnnotatedString(contentValue.text))
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                            )

                            if (initialNote != null) {
                                DropdownMenuItem(
                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMoreMenu = false
                                        onDeleteNote(initialNote.id)
                                        onBack()
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

            // Color Palette Selector Row
            if (showColorPalette) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (p in NoteColorPalettes) {
                        val isSelected = p.hex.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isDark) p.darkColor else p.lightColor)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    colorHex = p.hex
                                    showColorPalette = false
                                }
                        )
                    }
                }
            }

            // Folder & Tags Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder Selector
                Box {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = onScreenTextColor.copy(alpha = 0.08f),
                        modifier = Modifier.clickable { showFolderMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedFolder.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = onScreenTextColor
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showFolderMenu,
                        onDismissRequest = { showFolderMenu = false }
                    ) {
                        for (folder in folders) {
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = {
                                    selectedFolder = folder
                                    showFolderMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Tags Input
                BasicTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = onScreenTextColor),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (tagsText.isEmpty()) {
                            Text(
                                text = "Add tags (e.g. #work, #todo)",
                                style = MaterialTheme.typography.bodySmall,
                                color = onScreenTextColor.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                )

                if (isEncrypted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = "E2EE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Tab Row: Edit vs Markdown Preview
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {
                    HorizontalDivider(color = onScreenTextColor.copy(alpha = 0.1f))
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Edit Markdown", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Preview", fontWeight = FontWeight.SemiBold) }
                )
            }

            // Markdown Toolbar (visible in Edit mode)
            if (selectedTab == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MarkdownToolbarButton(icon = Icons.Default.FormatListBulleted, tooltip = "Bullet") {
                        insertMarkdown("\n- ")
                    }
                    MarkdownToolbarButton(icon = Icons.Default.CheckBox, tooltip = "Checklist") {
                        insertMarkdown("\n- [ ] ")
                    }
                    MarkdownToolbarButton(icon = Icons.Default.FormatBold, tooltip = "Bold") {
                        insertMarkdown("**", "**")
                    }
                    MarkdownToolbarButton(icon = Icons.Default.FormatItalic, tooltip = "Italic") {
                        insertMarkdown("*", "*")
                    }
                    MarkdownToolbarButton(icon = Icons.Default.Title, tooltip = "Header") {
                        insertMarkdown("\n### ")
                    }
                    MarkdownToolbarButton(icon = Icons.Default.FormatQuote, tooltip = "Quote") {
                        insertMarkdown("\n> ")
                    }
                    MarkdownToolbarButton(icon = Icons.Default.HorizontalRule, tooltip = "Divider") {
                        insertMarkdown("\n---\n")
                    }
                }
                HorizontalDivider(color = onScreenTextColor.copy(alpha = 0.1f))
            }

            // Main Editor Body
            if (selectedTab == 0) {
                // Edit Mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Note Title
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = onScreenTextColor
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_title_input"),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    text = "Title",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = onScreenTextColor.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Note Content
                    BasicTextField(
                        value = contentValue,
                        onValueChange = { contentValue = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = onScreenTextColor,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .testTag("note_content_input"),
                        decorationBox = { innerTextField ->
                            if (contentValue.text.isEmpty()) {
                                Text(
                                    text = "Type your note here...\nSupports Markdown: **bold**, *italic*, - bullets, - [ ] checklists, > quotes",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = onScreenTextColor.copy(alpha = 0.45f),
                                        lineHeight = 24.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            } else {
                // Preview Mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = onScreenTextColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (contentValue.text.isNotBlank()) {
                        MarkdownRenderer(
                            content = contentValue.text,
                            textColor = onScreenTextColor,
                            interactiveCheckboxes = true,
                            onCheckboxToggled = { lineText, isChecked ->
                                val lines = contentValue.text.lines().toMutableList()
                                val index = lines.indexOfFirst {
                                    it.trim().removePrefix("- [ ] ").removePrefix("- [x] ").trim() == lineText.trim()
                                }
                                if (index != -1) {
                                    val prefix = if (isChecked) "- [x] " else "- [ ] "
                                    lines[index] = prefix + lineText.trim()
                                    contentValue = TextFieldValue(lines.joinToString("\n"))
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "No content to preview",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onScreenTextColor.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Word count & stats bar at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val words = contentValue.text.split(Regex("\\s+")).count { it.isNotBlank() }
                val chars = contentValue.text.length
                Text(
                    text = "$words words • $chars characters",
                    style = MaterialTheme.typography.labelSmall,
                    color = onScreenTextColor.copy(alpha = 0.5f)
                )

                if (isEncrypted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Encrypted with AES-256-GCM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownToolbarButton(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
