package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.local.entities.HabitRecordEntity
import com.example.data.local.entities.PrayerRecordEntity
import com.example.domain.models.PrayerReasons
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald800
import com.example.ui.theme.StatusCongregation
import com.example.ui.theme.StatusMissed

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyReflectionSheet(
    prayers: List<PrayerRecordEntity>,
    habits: List<HabitRecordEntity>,
    currentStruggledHabit: String?,
    currentReason: String?,
    currentCustomReason: String?,
    currentNote: String?,
    onDismiss: () -> Unit,
    onSave: (struggledHabit: String?, reason: String?, customReason: String?, note: String?) -> Unit
) {
    var selectedHabit by remember { mutableStateOf(currentStruggledHabit) }
    var selectedReason by remember { mutableStateOf(currentReason) }
    var customReasonText by remember { mutableStateOf(currentCustomReason ?: "") }
    var noteText by remember { mutableStateOf(currentNote ?: "") }
    var showCustomInput by remember { mutableStateOf(currentReason == "سبب آخر") }

    val completedPrayers = prayers.count { it.status == "CONGREGATION" || it.status == "INDIVIDUAL" }
    val congregationCount = prayers.count { it.status == "CONGREGATION" }
    val individualCount = prayers.count { it.status == "INDIVIDUAL" }
    val completedHabitsCount = habits.count { it.isCompleted }

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
            Text(
                text = "محاسبة النفس 🤍",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "وقفة سريعة لختام يومك واستعداد أفضل للغد",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Day Summary Pill
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "حصاد يومك اليوم:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🕌 $congregationCount صلاة جماعة | ✓ $individualCount منفرد",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StatusCongregation
                        )
                        Text(
                            text = "🌿 $completedHabitsCount عبادات منجزة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reflection Question: What was hardest to maintain today?
            Text(
                text = "ما أكثر عبادة واجهت فيها صعوبة اليوم؟ (اختياري)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            )

            val habitOptions = listOf("صلاة الفجر في وقتها", "صلاة الجماعة بالمسجد", "ورد القرآن", "الأذكار والاستغفار", "صلاة الوتر", "صلاة الضحى")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                habitOptions.forEach { habit ->
                    val isSelected = selectedHabit == habit
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedHabit = if (isSelected) null else habit
                        },
                        label = { Text(habit, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reason for struggle
            Text(
                text = "ما السبب الرئيسي؟",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrayerReasons.REFLECTION_STRUGGLE_REASONS.forEach { reason ->
                    val isSelected = selectedReason == reason
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
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
                    onSave(selectedHabit, selectedReason, if (showCustomInput) customReasonText else null, if (noteText.isBlank()) null else noteText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_reflection_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("حفظ المحاسبة وتجهيز الغد", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
