package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NoteEntity
import com.example.data.repository.ExportFormat
import com.example.domain.model.NoteSourceType
import com.example.ui.MainViewModel
import com.example.ui.components.AudioPlayerBar
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.PrimaryScarlet
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val selectedNote by viewModel.selectedNote.collectAsState()

    var isEditingBody by remember { mutableStateOf(false) }
    var editedBodyText by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newTitleInput by remember { mutableStateOf("") }
    var showExportSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Load note when screen opens
    LaunchedEffect(noteId) {
        viewModel.selectNote(noteId)
    }

    LaunchedEffect(selectedNote) {
        selectedNote?.let {
            if (!isEditingBody) {
                editedBodyText = it.body
            }
        }
    }

    val note = selectedNote
    if (note == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading note...", color = TextMuted)
        }
        return
    }

    val isAudioNote = note.getSourceTypeEnum() != NoteSourceType.TEXT

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isAudioNote) "Voice Note" else "Note",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("note_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Pin / Unpin
                    IconButton(
                        onClick = { viewModel.togglePin(note) },
                        modifier = Modifier.testTag("detail_pin_button")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin" else "Pin",
                            tint = if (note.isPinned) PrimaryScarlet else TextSecondary
                        )
                    }

                    // Archive / Unarchive
                    IconButton(
                        onClick = {
                            viewModel.toggleArchive(note)
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("detail_archive_button")
                    ) {
                        Icon(
                            imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (note.isArchived) "Unarchive" else "Archive",
                            tint = TextSecondary
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.testTag("detail_more_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Note", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = TextSecondary) },
                                onClick = {
                                    showMoreMenu = false
                                    shareNote(context, note.title, note.body)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Copy Text", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary) },
                                onClick = {
                                    showMoreMenu = false
                                    copyToClipboard(context, note.body)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Export Note", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = TextSecondary) },
                                onClick = {
                                    showMoreMenu = false
                                    showExportSheet = true
                                }
                            )

                            if (isAudioNote && !note.audioFilePath.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text("Re-transcribe Audio", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.retranscribeNote(note)
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Delete Note", color = PrimaryScarlet) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = PrimaryScarlet) },
                                onClick = {
                                    showMoreMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Title Card with rename affordance
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = note.title,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 24.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatFullDate(note.createdAt),
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            if (note.titleWasAutoGenerated) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Auto-titled",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            newTitleInput = note.title
                            showRenameDialog = true
                        },
                        modifier = Modifier.testTag("rename_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename Title",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Embedded Audio Player (for voice / imported audio notes)
            if (isAudioNote && !note.audioFilePath.isNullOrBlank()) {
                val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()
                val currentPositionMs by viewModel.audioPlayer.currentPositionMs.collectAsState()
                val playerDurationMs by viewModel.audioPlayer.durationMs.collectAsState()
                val playbackSpeed by viewModel.audioPlayer.playbackSpeed.collectAsState()

                AudioPlayerBar(
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = if (playerDurationMs > 0L) playerDurationMs else note.durationMs,
                    playbackSpeed = playbackSpeed,
                    onTogglePlay = { viewModel.audioPlayer.togglePlayPause() },
                    onSeek = { viewModel.audioPlayer.seekTo(it) },
                    onSpeedChange = { viewModel.audioPlayer.setSpeed(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Note Body Content Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NOTE CONTENT",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        if (!isEditingBody) {
                            IconButton(
                                onClick = {
                                    editedBodyText = note.body
                                    isEditingBody = true
                                },
                                modifier = Modifier.size(32.dp).testTag("edit_body_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Text",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Row {
                                IconButton(
                                    onClick = { isEditingBody = false },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Edit",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        viewModel.updateNoteBody(note.id, editedBodyText)
                                        isEditingBody = false
                                    },
                                    modifier = Modifier.size(32.dp).testTag("save_body_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save Edit",
                                        tint = PrimaryScarlet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isEditingBody) {
                        OutlinedTextField(
                            value = editedBodyText,
                            onValueChange = { editedBodyText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .testTag("edit_body_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryScarlet,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated
                            )
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = note.body,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 24.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Word count & info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${note.wordCount} words",
                            color = TextMuted,
                            fontSize = 12.sp
                        )

                        if (note.suggestedKeywords.isNotBlank()) {
                            Text(
                                text = "Keywords: ${note.suggestedKeywords.replace(",", " • ")}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Technical Metadata (Subtle)
            if (isAudioNote && note.provider != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Engine: ${note.provider} (${note.model ?: ""})",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            if (note.detectedLanguages != null) {
                                Text(
                                    text = "Language: ${note.detectedLanguages}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (note.processingTimeMs != null && note.processingTimeMs > 0) {
                            Text(
                                text = "${note.processingTimeMs}ms",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Rename Title", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTitleInput,
                    onValueChange = { newTitleInput = it },
                    singleLine = true,
                    placeholder = { Text("Enter note title", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().testTag("rename_title_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryScarlet,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitleInput.isNotBlank()) {
                            viewModel.updateNoteTitle(note.id, newTitleInput)
                            showRenameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryScarlet),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Delete Confirm Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Delete Note?", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete this note and any attached audio recording.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteNote(note.id)
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryScarlet),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Export Bottom Sheet
    if (showExportSheet) {
        ExportBottomSheet(
            note = note,
            viewModel = viewModel,
            onDismiss = { showExportSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    note: NoteEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val isAudio = note.getSourceTypeEnum() != NoteSourceType.TEXT

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Export Note",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select your preferred format to export or copy",
                color = TextMuted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            ExportFormatRow("Plain Text (.txt)", "Standard text with note headers") {
                val content = viewModel.getExportText(note, ExportFormat.TXT)
                shareExportedText(context, "${note.title}.txt", content)
                onDismiss()
            }

            Spacer(modifier = Modifier.height(10.dp))

            ExportFormatRow("Markdown (.md)", "Formatted with headings and bullet points") {
                val content = viewModel.getExportText(note, ExportFormat.MARKDOWN)
                shareExportedText(context, "${note.title}.md", content)
                onDismiss()
            }

            if (isAudio) {
                Spacer(modifier = Modifier.height(10.dp))

                ExportFormatRow("Subtitles (.srt)", "Timestamped subtitle file") {
                    val content = viewModel.getExportText(note, ExportFormat.SRT)
                    shareExportedText(context, "${note.title}.srt", content)
                    onDismiss()
                }

                Spacer(modifier = Modifier.height(10.dp))

                ExportFormatRow("WebVTT (.vtt)", "Web video text track") {
                    val content = viewModel.getExportText(note, ExportFormat.VTT)
                    shareExportedText(context, "${note.title}.vtt", content)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
fun ExportFormatRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextMuted, fontSize = 12.sp)
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryScarlet),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export", fontSize = 12.sp)
            }
        }
    }
}

fun shareNote(context: Context, title: String, body: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title\n\n$body")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share note")
    context.startActivity(shareIntent)
}

fun shareExportedText(context: Context, fileName: String, content: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        putExtra(Intent.EXTRA_TEXT, content)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Export $fileName")
    context.startActivity(shareIntent)
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Note Content", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
}

fun formatFullDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMM d, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
