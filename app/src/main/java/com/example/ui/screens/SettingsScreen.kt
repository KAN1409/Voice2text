package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AccuracyMode
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.PrimaryScarlet
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val accuracyMode by viewModel.accuracyMode.collectAsState()
    val geminiKeyStored by viewModel.geminiKey.collectAsState()
    val groqKeyStored by viewModel.groqKey.collectAsState()
    val geminiTestResult by viewModel.geminiTestResult.collectAsState()
    val groqTestResult by viewModel.groqTestResult.collectAsState()
    val todayGeminiMins by viewModel.todayGeminiMinutes.collectAsState()
    val todayGroqMins by viewModel.todayGroqMinutes.collectAsState()

    var geminiInput by remember(geminiKeyStored) { mutableStateOf(geminiKeyStored) }
    var groqInput by remember(groqKeyStored) { mutableStateOf(groqKeyStored) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showGroqKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Accuracy Mode Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ACCURACY & SPEED MODE",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SurfaceBorder, SurfaceBorder)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AccuracyModeOption(
                            title = "Maximum Accuracy (Recommended)",
                            description = "Uses Gemini 2.5 Flash with automatic failover to Groq Whisper Large V3. Best for Egyptian slang + English.",
                            selected = accuracyMode == AccuracyMode.MAXIMUM_ACCURACY,
                            onClick = { viewModel.setAccuracyMode(AccuracyMode.MAXIMUM_ACCURACY) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AccuracyModeOption(
                            title = "Balanced Mode",
                            description = "Standard rate optimization across Gemini & Whisper Large V3.",
                            selected = accuracyMode == AccuracyMode.BALANCED,
                            onClick = { viewModel.setAccuracyMode(AccuracyMode.BALANCED) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AccuracyModeOption(
                            title = "Fast Mode",
                            description = "Uses Whisper Large V3 Turbo for instant transcription.",
                            selected = accuracyMode == AccuracyMode.FAST,
                            onClick = { viewModel.setAccuracyMode(AccuracyMode.FAST) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // API Keys Section
            item {
                Text(
                    text = "CLOUD PROVIDER API KEYS",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SurfaceBorder, SurfaceBorder)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Gemini Key
                        Text(
                            text = "Gemini API Key (Google AI)",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = geminiInput,
                            onValueChange = { geminiInput = it },
                            placeholder = { Text("Enter Gemini API key", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                    Icon(
                                        imageVector = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Key Visibility",
                                        tint = TextMuted
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_api_key_input"),
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = geminiTestResult ?: "Free tier supported",
                                color = if (geminiTestResult?.startsWith("✓") == true) AccentGreen else TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedButton(
                                onClick = { viewModel.testGeminiConnection() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp).testTag("test_gemini_key_button")
                            ) {
                                Text("Test", fontSize = 12.sp, color = TextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Groq Key
                        Text(
                            text = "Groq API Key (Whisper Large V3)",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = groqInput,
                            onValueChange = { groqInput = it },
                            placeholder = { Text("Enter Groq API key", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            visualTransformation = if (showGroqKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showGroqKey = !showGroqKey }) {
                                    Icon(
                                        imageVector = if (showGroqKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Key Visibility",
                                        tint = TextMuted
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("groq_api_key_input"),
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = groqTestResult ?: "Free tier supported",
                                color = if (groqTestResult?.startsWith("✓") == true) AccentGreen else TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedButton(
                                onClick = { viewModel.testGroqConnection() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp).testTag("test_groq_key_button")
                            ) {
                                Text("Test", fontSize = 12.sp, color = TextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save Keys Button
                        Button(
                            onClick = {
                                viewModel.saveKeys(geminiInput, groqInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryScarlet),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_api_keys_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save API Keys", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Usage Stats Overview
            item {
                Text(
                    text = "TODAY'S USAGE",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SurfaceBorder, SurfaceBorder)))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Gemini", color = TextSecondary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", todayGeminiMins)} min",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(SurfaceBorder))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Groq Whisper", color = TextSecondary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", todayGroqMins)} min",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // About Section
            item {
                Text(
                    text = "ABOUT",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SurfaceBorder, SurfaceBorder)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "VoiceTranscriber v1.0",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Specialized AI engine for Egyptian Arabic & English code-switching speech transcription with intelligent multi-provider failover.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccuracyModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SurfaceElevated else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) PrimaryScarlet else TextMuted,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
