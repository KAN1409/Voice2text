package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.RecordingState
import com.example.data.local.entity.TranscriptionEntity
import com.example.domain.model.ProviderStatus
import com.example.ui.MainViewModel
import com.example.ui.components.LiveWaveformVisualizer
import com.example.ui.components.RecordingModalDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryScarlet
import com.example.ui.theme.PrimaryScarletDark
import com.example.ui.theme.PrimaryScarletGlow
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
    onNavigateToResult: (Long) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToVocabulary: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val recentItems by viewModel.recentTranscriptions.collectAsState()
    val providerHealthMap by viewModel.providerHealthMap.collectAsState()
    val todayGeminiMins by viewModel.todayGeminiMinutes.collectAsState()
    val todayGroqMins by viewModel.todayGroqMinutes.collectAsState()

    val recordingState by viewModel.audioRecorder.recordingState.collectAsState()
    val elapsedTimeMs by viewModel.audioRecorder.elapsedTimeMs.collectAsState()
    val amplitudes by viewModel.audioRecorder.amplitudesHistory.collectAsState()

    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = viewModel.startRecording()
            if (!started) {
                Toast.makeText(context, "Could not start audio recorder", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice notes", Toast.LENGTH_LONG).show()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAudio(uri)
        }
    }

    // Modal when active recording is running
    if (recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED) {
        RecordingModalDialog(
            elapsedTimeMs = elapsedTimeMs,
            recordingState = recordingState,
            amplitudes = amplitudes,
            onPause = { viewModel.pauseRecording() },
            onResume = { viewModel.resumeRecording() },
            onStopAndTranscribe = { viewModel.stopAndTranscribe() },
            onCancel = { viewModel.cancelRecording() }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "VOICE TRANSCRIBER",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Egyptian Arabic + English",
                            color = PrimaryScarlet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToVocabulary,
                        modifier = Modifier.testTag("nav_vocabulary_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = "Custom Vocabulary",
                            tint = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Providers Status Bar
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ProviderHealthBar(
                    geminiHealth = providerHealthMap["Gemini"]?.status ?: ProviderStatus.AVAILABLE,
                    groqHealth = providerHealthMap["Groq Whisper"]?.status ?: ProviderStatus.AVAILABLE,
                    geminiMins = todayGeminiMins,
                    groqMins = todayGroqMins,
                    onOpenSettings = onNavigateToSettings
                )
            }

            // Main Recording Section
            item {
                Spacer(modifier = Modifier.height(36.dp))

                val infiniteTransition = rememberInfiniteTransition()
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(PrimaryScarletGlow)
                    )

                    // Secondary ring
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                            .border(2.dp, PrimaryScarlet.copy(alpha = 0.4f), CircleShape)
                    )

                    // Main mic button
                    Box(
                        modifier = Modifier
                            .size(118.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(PrimaryScarlet, PrimaryScarletDark)
                                )
                            )
                            .clickable {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    viewModel.startRecording()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            .testTag("record_voice_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record Voice Note",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Tap to Record",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Verbatim Egyptian Arabic & English mixed speech",
                    color = TextMuted,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Import Audio Button
                OutlinedButton(
                    onClick = {
                        audioPickerLauncher.launch("audio/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(48.dp)
                        .testTag("import_audio_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceElevated
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Import Audio File",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Supports M4A, MP3, WAV, AAC, MP4, OGG, OPUS, FLAC",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            // Recent Transcriptions Header
            item {
                Spacer(modifier = Modifier.height(36.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT TRANSCRIPTIONS",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    if (recentItems.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onNavigateToHistory() }
                                .padding(vertical = 4.dp)
                                .testTag("view_all_history_button")
                        ) {
                            Text(
                                text = "View All (${recentItems.size})",
                                color = PrimaryScarlet,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = PrimaryScarlet,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Recent Transcriptions List
            if (recentItems.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                        color = SurfaceDark
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No transcriptions yet",
                                color = TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Record or import your first voice note above",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(recentItems, key = { it.id }) { item ->
                    RecentTranscriptionCard(
                        item = item,
                        onClick = { onNavigateToResult(item.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun ProviderHealthBar(
    geminiHealth: ProviderStatus,
    groqHealth: ProviderStatus,
    geminiMins: Double,
    groqMins: Double,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
        color = SurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gemini status
            ProviderChip(
                name = "Gemini",
                status = geminiHealth,
                minsToday = geminiMins,
                onClick = onOpenSettings
            )

            // Separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(SurfaceBorder)
            )

            // Groq status
            ProviderChip(
                name = "Groq Whisper",
                status = groqHealth,
                minsToday = groqMins,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
fun ProviderChip(
    name: String,
    status: ProviderStatus,
    minsToday: Double,
    onClick: () -> Unit
) {
    val statusColor = when (status) {
        ProviderStatus.AVAILABLE -> AccentGreen
        ProviderStatus.RATE_LIMITED -> AccentAmber
        ProviderStatus.UNAVAILABLE -> ErrorRed
        ProviderStatus.MISSING_KEY -> TextMuted
    }

    val statusText = when (status) {
        ProviderStatus.AVAILABLE -> "Active"
        ProviderStatus.RATE_LIMITED -> "Rate Limited"
        ProviderStatus.UNAVAILABLE -> "Error"
        ProviderStatus.MISSING_KEY -> "Setup Key"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = "$name • $statusText",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Today: ${String.format(Locale.US, "%.1f", minsToday)}m",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun RecentTranscriptionCard(
    item: TranscriptionEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("recent_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SurfaceBorder, SurfaceBorder)))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Duration badge
                val totalSecs = item.durationMs / 1000
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceElevated
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", mins, secs),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Transcript preview snippet
            Text(
                text = item.transcript,
                color = TextMuted,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer with provider badge and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.provider,
                        color = PrimaryScarlet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • ar-EG + en",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                Text(
                    text = dateFormat.format(Date(item.createdAt)),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
