package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.VocabularyEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.PrimaryScarlet
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VocabularyScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val vocabList by viewModel.vocabularyList.collectAsState()

    var termInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("General") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("General", "Engineering", "Construction", "Business", "Materials", "Locations")

    val engineeringPreset = listOf(
        "PR" to "Engineering",
        "BOQ" to "Engineering",
        "Variation Order" to "Engineering",
        "RFI" to "Engineering",
        "Shop Drawing" to "Engineering",
        "Submittal" to "Engineering",
        "Inspection" to "Engineering",
        "Consultant" to "Engineering"
    )

    val egyptianBizPreset = listOf(
        "Quotation" to "Business",
        "Contractor" to "Business",
        "Invoice" to "Business",
        "Down Payment" to "Business",
        "Milestone" to "Business",
        "New Cairo" to "Locations",
        "Sheikh Zayed" to "Locations",
        "Galala" to "Materials"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Custom Vocabulary",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("vocab_back_button")
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
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add company names, abbreviations, technical phrases, and project keywords to guide Gemini and Groq speech recognition.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SurfaceBorder, SurfaceBorder)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ADD NEW TERM",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = termInput,
                                onValueChange = { termInput = it },
                                placeholder = { Text("e.g. Variation Order, BOQ, Galala...", color = TextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("vocab_term_input"),
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

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = {
                                    if (termInput.isNotBlank()) {
                                        viewModel.addVocabulary(termInput, selectedCategory)
                                        termInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryScarlet),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(52.dp).testTag("vocab_add_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Term")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category selection chips
                        Text(
                            text = "Category:",
                            color = TextMuted,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryScarlet,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceElevated,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = SurfaceBorder,
                                        selectedBorderColor = PrimaryScarlet,
                                        enabled = true,
                                        selected = selectedCategory == cat
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Quick Preset Packs
            item {
                Text(
                    text = "ONE-TAP INDUSTRY PRESETS",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.addPresetVocabulary(engineeringPreset) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryScarlet)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Engineering", color = TextPrimary, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.addPresetVocabulary(egyptianBizPreset) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryScarlet)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Business / EG", color = TextPrimary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Current Terms Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE VOCABULARY (${vocabList.size})",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // List of terms
            if (vocabList.isEmpty()) {
                item {
                    Text("No custom vocabulary added yet", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                items(vocabList, key = { it.id }) { item ->
                    VocabularyItemRow(
                        item = item,
                        onDelete = { viewModel.deleteVocabulary(item.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun VocabularyItemRow(
    item: VocabularyEntity,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp)),
        color = SurfaceDark
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.term,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.category,
                    color = PrimaryScarlet,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Term",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
