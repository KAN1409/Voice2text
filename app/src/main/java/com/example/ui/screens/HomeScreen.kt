package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.NoteEntity
import com.example.domain.model.NoteSourceType
import com.example.ui.MainViewModel
import com.example.ui.components.CaptureOptionSheet
import com.example.ui.components.RecordingModalDialog
import com.example.ui.components.TextNoteEditorDialog
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
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToNoteDetail: (Long) -> Unit,
    onNavigateToArchived: () -> Unit,
    onNavigateToVocabulary: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val allNotes by viewModel.allActiveNotes.collectAsState()
    val pinnedNotes by viewModel.pinnedNotes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var showRecordingDialog by remember { mutableStateOf(false) }
    var showCaptureOptions by remember { mutableStateOf(false) }
    var showTextNoteEditor by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") } // All, Voice, Text, Pinned

    // Audio importer SAF launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importAudio(it) }
    }

    // Permission launcher for recording
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = viewModel.startRecording()
            if (started) {
                showRecordingDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Voice2text",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryScarlet.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Notes",
                                color = PrimaryScarlet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToArchived,
                        modifier = Modifier.testTag("archive_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archived Notes",
                            tint = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToVocabulary,
                        modifier = Modifier.testTag("vocab_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spellcheck,
                            contentDescription = "Custom Vocabulary",
                            tint = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCaptureOptions = true },
                containerColor = PrimaryScarlet,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("capture_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Capture Note",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Search notes by title, body, or keywords...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryScarlet,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter chips row
            if (searchQuery.isBlank()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf("All", "Voice", "Text", "Pinned")
                    items(filters) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryScarlet,
                                selectedLabelColor = Color.White,
                                containerColor = SurfaceDark,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = SurfaceBorder,
                                selectedBorderColor = PrimaryScarlet,
                                enabled = true,
                                selected = selectedFilter == filter
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Notes List Content
            val displayedNotes = when {
                searchQuery.isNotBlank() -> searchResults
                selectedFilter == "Voice" -> allNotes.filter { it.getSourceTypeEnum() == NoteSourceType.VOICE || it.getSourceTypeEnum() == NoteSourceType.IMPORTED_AUDIO }
                selectedFilter == "Text" -> allNotes.filter { it.getSourceTypeEnum() == NoteSourceType.TEXT }
                selectedFilter == "Pinned" -> pinnedNotes
                else -> allNotes
            }

            if (displayedNotes.isEmpty()) {
                EmptyNotesView(
                    isSearch = searchQuery.isNotBlank(),
                    onStartVoice = {
                        val hasMicPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasMicPermission) {
                            val started = viewModel.startRecording()
                            if (started) showRecordingDialog = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStartText = { showTextNoteEditor = true },
                    onStartImport = { audioPickerLauncher.launch("audio/*") }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pinned notes section (only when on All view without active search)
                    if (searchQuery.isBlank() && selectedFilter == "All" && pinnedNotes.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = PrimaryScarlet,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PINNED",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        items(pinnedNotes, key = { "pinned_${it.id}" }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { onNavigateToNoteDetail(note.id) },
                                onTogglePin = { viewModel.togglePin(note) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "OTHER NOTES (${allNotes.size - pinnedNotes.size})",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        val unpinnedNotes = allNotes.filter { !it.isPinned }
                        items(unpinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { onNavigateToNoteDetail(note.id) },
                                onTogglePin = { viewModel.togglePin(note) }
                            )
                        }
                    } else {
                        // Regular list
                        items(displayedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { onNavigateToNoteDetail(note.id) },
                                onTogglePin = { viewModel.togglePin(note) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Capture Options Modal Bottom Sheet
    if (showCaptureOptions) {
        CaptureOptionSheet(
            onDismiss = { showCaptureOptions = false },
            onSelectVoice = {
                val hasMicPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasMicPermission) {
                    val started = viewModel.startRecording()
                    if (started) showRecordingDialog = true
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onSelectText = { showTextNoteEditor = true },
            onSelectImport = { audioPickerLauncher.launch("audio/*") }
        )
    }

    // Text Note Editor Dialog
    if (showTextNoteEditor) {
        TextNoteEditorDialog(
            onDismiss = { showTextNoteEditor = false },
            onSave = { title, body ->
                showTextNoteEditor = false
                viewModel.createTextNote(title, body) { newId ->
                    onNavigateToNoteDetail(newId)
                }
            }
        )
    }

    // Voice Recording Modal Dialog
    if (showRecordingDialog) {
        val recordingState by viewModel.audioRecorder.recordingState.collectAsState()
        val elapsedMs by viewModel.audioRecorder.elapsedTimeMs.collectAsState()
        val amplitudes by viewModel.audioRecorder.amplitudesHistory.collectAsState()

        RecordingModalDialog(
            elapsedTimeMs = elapsedMs,
            recordingState = recordingState,
            amplitudes = amplitudes,
            onPause = { viewModel.pauseRecording() },
            onResume = { viewModel.resumeRecording() },
            onCancel = {
                viewModel.cancelRecording()
                showRecordingDialog = false
            },
            onStopAndTranscribe = {
                viewModel.stopAndTranscribe()
                showRecordingDialog = false
            }
        )
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit
) {
    val sourceType = note.getSourceTypeEnum()
    val isAudio = sourceType != NoteSourceType.TEXT
    val dateFormatted = formatCompactDate(note.createdAt)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = if (note.isPinned) {
            BorderStroke(1.dp, PrimaryScarlet.copy(alpha = 0.5f))
        } else {
            BorderStroke(1.dp, SurfaceBorder)
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: Source badge & Pin icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (sourceType) {
                        NoteSourceType.VOICE -> {
                            SourceBadge(
                                icon = Icons.Default.Mic,
                                label = if (note.durationMs > 0) formatDuration(note.durationMs) else "Voice",
                                tint = PrimaryScarlet,
                                bg = PrimaryScarlet.copy(alpha = 0.12f)
                            )
                        }
                        NoteSourceType.TEXT -> {
                            SourceBadge(
                                icon = Icons.Default.EditNote,
                                label = "Text Note",
                                tint = Color(0xFF388AF6),
                                bg = Color(0xFF388AF6).copy(alpha = 0.12f)
                            )
                        }
                        NoteSourceType.IMPORTED_AUDIO -> {
                            SourceBadge(
                                icon = Icons.Default.FileOpen,
                                label = "Imported Audio",
                                tint = Color(0xFF30D158),
                                bg = Color(0xFF30D158).copy(alpha = 0.12f)
                            )
                        }
                    }

                    if (note.titleWasAutoGenerated) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Auto-titled",
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormatted,
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(28.dp).testTag("pin_note_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin" else "Pin",
                            tint = if (note.isPinned) PrimaryScarlet else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title prominently
            Text(
                text = note.title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Body preview (1-3 lines)
            Text(
                text = note.body,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun SourceBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    bg: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EmptyNotesView(
    isSearch: Boolean,
    onStartVoice: () -> Unit,
    onStartText: () -> Unit,
    onStartImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(1.dp, SurfaceBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearch) Icons.Default.Search else Icons.Default.EditNote,
                contentDescription = null,
                tint = PrimaryScarlet,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSearch) "No Matching Notes Found" else "No Notes Yet",
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isSearch) {
                "Try searching with different terms in Arabic or English"
            } else {
                "Capture your thoughts instantly using voice, text, or imported audio"
            },
            color = TextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (!isSearch) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.Button(
                    onClick = onStartVoice,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = PrimaryScarlet),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Voice Note", fontSize = 13.sp)
                }

                androidx.compose.material3.OutlinedButton(
                    onClick = onStartText,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Text Note", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}

fun formatCompactDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000L -> "Just now"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 86400_000L -> "${diff / 3600_000L}h ago"
        diff < 7 * 86400_000L -> "${diff / 86400_000L}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
