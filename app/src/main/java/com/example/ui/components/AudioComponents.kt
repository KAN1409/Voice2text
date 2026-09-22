package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.RecordingState
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
import java.util.Locale

@Composable
fun LiveWaveformVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    isRecording: Boolean = true,
    waveColor: Color = PrimaryScarlet
) {
    Canvas(modifier = modifier.fillMaxWidth().height(90.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val barCount = 45
        val barWidth = 4.dp.toPx()
        val spacing = (width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

        val recentAmps = if (amplitudes.size >= barCount) {
            amplitudes.takeLast(barCount)
        } else {
            val padCount = barCount - amplitudes.size
            List(padCount) { 0.08f } + amplitudes
        }

        for (i in 0 until barCount) {
            val amp = recentAmps.getOrElse(i) { 0.08f }.coerceIn(0.05f, 1f)
            val barHeight = (height * amp * 0.85f).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + spacing)
            val y = centerY - (barHeight / 2f)

            drawRoundRect(
                color = if (isRecording) waveColor else waveColor.copy(alpha = 0.4f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
fun RecordingModalDialog(
    elapsedTimeMs: Long,
    recordingState: RecordingState,
    amplitudes: List<Float>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopAndTranscribe: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Prevent accidental dismiss while recording */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark.copy(alpha = 0.96f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (recordingState == RecordingState.RECORDING) PrimaryScarlet else AccentGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (recordingState == RecordingState.RECORDING) "Recording..." else "Paused",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Elapsed Time
                val totalSecs = elapsedTimeMs / 1000
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                val millis = (elapsedTimeMs % 1000) / 100
                Text(
                    text = String.format(Locale.US, "%02d:%02d.%d", mins, secs, millis),
                    color = TextPrimary,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Waveform Canvas
                LiveWaveformVisualizer(
                    amplitudes = amplitudes,
                    isRecording = recordingState == RecordingState.RECORDING,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                            .border(1.dp, SurfaceBorder, CircleShape)
                            .testTag("cancel_recording_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Recording",
                            tint = TextSecondary
                        )
                    }

                    // Pause / Resume
                    IconButton(
                        onClick = {
                            if (recordingState == RecordingState.RECORDING) onPause() else onResume()
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                            .border(1.dp, SurfaceBorder, CircleShape)
                            .testTag("pause_resume_button")
                    ) {
                        Icon(
                            imageVector = if (recordingState == RecordingState.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (recordingState == RecordingState.RECORDING) "Pause" else "Resume",
                            tint = TextPrimary
                        )
                    }

                    // Stop & Transcribe
                    IconButton(
                        onClick = onStopAndTranscribe,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(PrimaryScarlet, PrimaryScarletDark))
                            )
                            .testTag("stop_and_transcribe_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done & Transcribe",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioPlayerBar(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
        color = SurfaceElevated
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Play/Pause Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryScarlet)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause Audio" else "Play Audio",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val curSecs = currentPositionMs / 1000
                    val durSecs = durationMs / 1000
                    Text(
                        text = String.format(
                            Locale.US,
                            "%02d:%02d / %02d:%02d",
                            curSecs / 60, curSecs % 60,
                            durSecs / 60, durSecs % 60
                        ),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Speed selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable {
                            val nextSpeed = when (playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            onSpeedChange(nextSpeed)
                        }
                        .testTag("player_speed_button")
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Slider
            val progress = if (durationMs > 0) {
                (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progress,
                onValueChange = { frac ->
                    onSeek((frac * durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryScarlet,
                    activeTrackColor = PrimaryScarlet,
                    inactiveTrackColor = SurfaceBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("player_seek_slider")
            )
        }
    }
}
