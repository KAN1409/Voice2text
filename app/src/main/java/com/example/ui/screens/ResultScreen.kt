package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TranscriptionEntity
import com.example.data.repository.ExportFormat
import com.example.ui.MainViewModel
import com.example.ui.components.AudioPlayerBar
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ErrorRed
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
fun ResultScreen(
    recordId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val record by viewModel.selectedRecord.collectAsState()

    // Audio player state
    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()
    val currentPositionMs by viewModel.audioPlayer.currentPositionMs.collectAsState()
    val durationMs by viewModel.audioPlayer.durationMs.collectAsState()
    val playbackSpeed by viewModel.audioPlayer.playbackSpeed.collectAsState()

    var showEditTitleDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var isEditingTranscript by remember { mutableStateOf(false) }
    var editedTranscriptText by remember { mutableStateOf("") }
    var editedTitleText by remember { mutableStateOf("") }

    LaunchedEffect(recordId) {
        viewModel.selectRecord(recordId)
    }

    LaunchedEffect(record) {
        record?.let {
            editedTranscriptText = it.transcript
            editedTitleText = it.title
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                record?.let {
                                    editedTitleText = it.title
                                    showEditTitleDialog = true
                                }
                            }
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = record?.title ?: "Transcription",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename Title",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.audioPlayer.release()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("result_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Copy action
                    IconButton(
                        onClick = {
                            record?.let {
                                copyToClipboard(context, it.transcript)
                                Toast.makeText(context, "Transcript copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("result_copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Text",
                            tint = TextPrimary
                        )
                    }

                    // Share action
                    IconButton(
                        onClick = {
                            record?.let {
                                shareText(context, it.title, it.transcript)
                            }
                        },
                        modifier = Modifier.testTag("result_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TextPrimary
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.testTag("result_more_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Actions",
                                tint = TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(SurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as TXT", color = TextPrimary) },
                                onClick = {
                                    showMoreMenu = false
                                    record?.let {
                                        val content = viewModel.getExportText(it, ExportFormat.TXT)
                                        shareText(context, "${it.title}.txt", content)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Markdown (.md)", color = TextPrimary) },
                                onClick = {
                                    showMoreMenu = false
                                    record?.let {
                                        val content = viewModel.getExportText(it, ExportFormat.MARKDOWN)
                                        shareText(context, "${it.title}.md", content)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Subtitles (.srt)", color = TextPrimary) },
                                onClick = {
                                    showMoreMenu = false
                                    record?.let {
                                        val content = viewModel.getExportText(it, ExportFormat.SRT)
                                        shareText(context, "${it.title}.srt", content)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as WebVTT (.vtt)", color = TextPrimary) },
                                onClick = {
                                    showMoreMenu = false
                                    record?.let {
                                        val content = viewModel.getExportText(it, ExportFormat.VTT)
                                        shareText(context, "${it.title}.vtt", content)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Re-transcribe Audio", color = PrimaryScarlet) },
                                onClick = {
                                    showMoreMenu = false
                                    record?.let { viewModel.retranscribeRecord(it) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = ErrorRed) },
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
        if (record == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Transcript not found", color = TextMuted)
            }
            return@Scaffold
        }

        val item = record!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Provider Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PrimaryScarlet)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.provider} • ${item.model}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Language & Date
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                Text(
                    text = dateFormat.format(Date(item.createdAt)),
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Player Bar
            val effectiveDuration = if (durationMs > 0) durationMs else item.durationMs
            AudioPlayerBar(
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = effectiveDuration,
                playbackSpeed = playbackSpeed,
                onTogglePlay = { viewModel.audioPlayer.togglePlayPause() },
                onSeek = { viewModel.audioPlayer.seekTo(it) },
                onSpeedChange = { viewModel.audioPlayer.setSpeed(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Transcript Box Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(SurfaceBorder, SurfaceBorder))
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSCRIPT (${item.wordCount} words)",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row {
                            if (isEditingTranscript) {
                                Button(
                                    onClick = {
                                        viewModel.updateRecordTranscript(item.id, editedTranscriptText)
                                        isEditingTranscript = false
                                        Toast.makeText(context, "Changes saved", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryScarlet),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp).testTag("save_transcript_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", fontSize = 12.sp)
                                }
                            } else {
                                TextButton(
                                    onClick = {
                                        editedTranscriptText = item.transcript
                                        isEditingTranscript = true
                                    },
                                    modifier = Modifier.testTag("edit_transcript_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryScarlet)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit", color = PrimaryScarlet, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isEditingTranscript) {
                        OutlinedTextField(
                            value = editedTranscriptText,
                            onValueChange = { editedTranscriptText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transcript_edit_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryScarlet,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated
                            ),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 8
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = item.transcript,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.testTag("transcript_text_display")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Rename Title Dialog
    if (showEditTitleDialog) {
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Rename Transcription", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedTitleText,
                    onValueChange = { editedTitleText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryScarlet,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("rename_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedTitleText.isNotBlank()) {
                            record?.let { viewModel.updateRecordTitle(it.id, editedTitleText) }
                        }
                        showEditTitleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryScarlet)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Delete Transcription", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this transcription?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        record?.let {
                            viewModel.deleteRecord(it.id)
                            onNavigateBack()
                        }
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Transcription", text)
    clipboard.setPrimaryClip(clip)
}

private fun shareText(context: Context, title: String, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Transcription"))
}
