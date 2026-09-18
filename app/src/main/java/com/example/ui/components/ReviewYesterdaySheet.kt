package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.PrayerStatus
import com.example.data.local.entities.HabitRecordEntity
import com.example.data.local.entities.PrayerRecordEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewYesterdaySheet(
    dateFormattedArabic: String,
    unrecordedPrayers: List<PrayerRecordEntity>,
    incompleteHabits: List<HabitRecordEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, PrayerStatus>, Map<String, Boolean>) -> Unit
) {
    val prayerStatusMap = remember {
        mutableStateMapOf<String, PrayerStatus>().apply {
            unrecordedPrayers.forEach { put(it.prayer, PrayerStatus.UNRECORDED) }
        }
    }

    val habitCompletedMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            incompleteHabits.forEach { put(it.habitKey, false) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("review_yesterday_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NightlightRound,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "مراجعة مهام أمس 🌙",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateFormattedArabic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "سجّل ما أديته أمس بهدوء واطمئنان دون لوم أو استعجال.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Prayers Section
            if (unrecordedPrayers.isNotEmpty()) {
                Text(
                    text = "الصلوات التي لم تسجلها أمس:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                unrecordedPrayers.forEach { prayer ->
                    val prayerAr = when (prayer.prayer) {
                        "FAJR" -> "الفجر"
                        "DHUHR" -> "الظهر"
                        "ASR" -> "العصر"
                        "MAGHRIB" -> "المغرب"
                        "ISHA" -> "العشاء"
                        else -> prayer.prayer
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "صلاة $prayerAr",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val selected = prayerStatusMap[prayer.prayer] ?: PrayerStatus.UNRECORDED
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selected == PrayerStatus.CONGREGATION,
                                    onClick = { prayerStatusMap[prayer.prayer] = PrayerStatus.CONGREGATION },
                                    label = { Text("جماعة 🕌", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StatusCongregationBg,
                                        selectedLabelColor = StatusCongregation
                                    )
                                )

                                FilterChip(
                                    selected = selected == PrayerStatus.INDIVIDUAL,
                                    onClick = { prayerStatusMap[prayer.prayer] = PrayerStatus.INDIVIDUAL },
                                    label = { Text("منفرد ✓", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StatusIndividualBg,
                                        selectedLabelColor = StatusIndividual
                                    )
                                )

                                FilterChip(
                                    selected = selected == PrayerStatus.MISSED,
                                    onClick = { prayerStatusMap[prayer.prayer] = PrayerStatus.MISSED },
                                    label = { Text("لم أصلِّ ✕", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StatusMissedBg,
                                        selectedLabelColor = StatusMissed
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Habits Section
            if (incompleteHabits.isNotEmpty()) {
                Text(
                    text = "عبادات وسنن أمس:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                incompleteHabits.forEach { habit ->
                    val isDone = habitCompletedMap[habit.habitKey] ?: false
                    val habitIcon = when (habit.habitKey) {
                        "quran_wird" -> Icons.AutoMirrored.Filled.MenuBook
                        "duha_prayer" -> Icons.Default.WbSunny
                        "witr_prayer" -> Icons.Default.Star
                        "night_prayer" -> Icons.Default.NightsStay
                        "sleep_azkar" -> Icons.Default.NightlightRound
                        else -> Icons.Default.Favorite
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDone) StatusCongregationBg.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isDone) StatusCongregation.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = habitIcon,
                                    contentDescription = null,
                                    tint = if (isDone) StatusCongregation else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = habit.titleArabic,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (habit.habitKey == "night_prayer") {
                                        Text(
                                            text = "3 ركعات نافلة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else if (habit.habitKey == "witr_prayer") {
                                        Text(
                                            text = "سنة مؤكدة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = isDone,
                                    onClick = { habitCompletedMap[habit.habitKey] = true },
                                    label = { Text("تمت ✓", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StatusCongregationBg,
                                        selectedLabelColor = StatusCongregation
                                    )
                                )

                                FilterChip(
                                    selected = !isDone,
                                    onClick = { habitCompletedMap[habit.habitKey] = false },
                                    label = { Text("لم أفعله", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Confirm Button
            Button(
                onClick = {
                    onConfirm(prayerStatusMap.toMap(), habitCompletedMap.toMap())
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_review_yesterday_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تمت مراجعة أمس",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
