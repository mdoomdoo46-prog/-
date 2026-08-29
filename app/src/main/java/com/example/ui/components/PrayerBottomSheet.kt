package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.PrayerStatus
import com.example.domain.models.PrayerReasons
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrayerBottomSheet(
    prayerNameArabic: String,
    prayerTime: String,
    currentStatus: PrayerStatus,
    currentReason: String?,
    currentCustomReason: String?,
    onDismiss: () -> Unit,
    onSave: (status: PrayerStatus, reason: String?, customReason: String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var selectedReason by remember { mutableStateOf(currentReason) }
    var customReasonText by remember { mutableStateOf(currentCustomReason ?: "") }
    var showCustomInput by remember { mutableStateOf(currentReason == "سبب آخر") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "كيف صليت $prayerNameArabic؟",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "موعد الأذان: $prayerTime",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Status Buttons (3 Main Fast Choices)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Congregation (جماعة)
                StatusChoiceCard(
                    title = "🕌 جماعة",
                    subtitle = "في المسجد",
                    isSelected = selectedStatus == PrayerStatus.CONGREGATION,
                    selectedBg = StatusCongregationBg,
                    selectedBorder = StatusCongregation,
                    selectedText = StatusCongregation,
                    modifier = Modifier.weight(1f).testTag("prayer_congregation_btn"),
                    onClick = {
                        selectedStatus = PrayerStatus.CONGREGATION
                        selectedReason = null
                        showCustomInput = false
                        onSave(PrayerStatus.CONGREGATION, null, null)
                    }
                )

                // 2. Individual (منفردًا)
                StatusChoiceCard(
                    title = "✓ منفردًا",
                    subtitle = "في البيت أو العمل",
                    isSelected = selectedStatus == PrayerStatus.INDIVIDUAL,
                    selectedBg = StatusIndividualBg,
                    selectedBorder = StatusIndividual,
                    selectedText = StatusIndividual,
                    modifier = Modifier.weight(1f).testTag("prayer_individual_btn"),
                    onClick = {
                        selectedStatus = PrayerStatus.INDIVIDUAL
                    }
                )

                // 3. Missed (لم أصلِّ)
                StatusChoiceCard(
                    title = "لم أصلِّ",
                    subtitle = "فاتني الوقت",
                    isSelected = selectedStatus == PrayerStatus.MISSED,
                    selectedBg = StatusMissedBg,
                    selectedBorder = StatusMissed,
                    selectedText = StatusMissed,
                    modifier = Modifier.weight(1f).testTag("prayer_missed_btn"),
                    onClick = {
                        selectedStatus = PrayerStatus.MISSED
                    }
                )
            }

            // Optional Reasons Selection for Individual or Missed
            if (selectedStatus == PrayerStatus.INDIVIDUAL || selectedStatus == PrayerStatus.MISSED) {
                Spacer(modifier = Modifier.height(20.dp))

                val reasonTitle = if (selectedStatus == PrayerStatus.INDIVIDUAL) {
                    "لماذا لم تصلِّ جماعة؟ (اختياري)"
                } else {
                    "ما السبب؟ (اختياري للتحسين المستقبلي)"
                }

                val reasonsList = if (selectedStatus == PrayerStatus.INDIVIDUAL) {
                    PrayerReasons.INDIVIDUAL_REASONS
                } else {
                    PrayerReasons.MISSED_REASONS
                }

                Text(
                    text = reasonTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                )

                // Chips for quick reasons
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reasonsList.forEach { reason ->
                        val isChipSelected = selectedReason == reason
                        FilterChip(
                            selected = isChipSelected,
                            onClick = {
                                if (isChipSelected) {
                                    selectedReason = null
                                    showCustomInput = false
                                } else {
                                    selectedReason = reason
                                    showCustomInput = (reason == "سبب آخر")
                                }
                            },
                            label = { Text(reason, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                if (showCustomInput) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customReasonText,
                        onValueChange = { customReasonText = it },
                        placeholder = { Text("اكتب السبب بإيجاز...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onSave(selectedStatus, selectedReason, if (showCustomInput) customReasonText else null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("prayer_save_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("حفظ التحديث", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatusChoiceCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    selectedBg: Color,
    selectedBorder: Color,
    selectedText: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) selectedBg else MaterialTheme.colorScheme.surfaceVariant
    val border = if (isSelected) selectedBorder else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(width = if (isSelected) 2.dp else 1.dp, color = border, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) selectedText else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
        }
    }
}
