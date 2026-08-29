package com.example.ui.screens.counter

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ripple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.DefaultHabits
import com.example.ui.components.MicroCelebrationBanner
import com.example.ui.theme.*

@Composable
fun CounterScreen(
    viewModel: CounterViewModel,
    initialCounterKey: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialCounterKey) {
        if (initialCounterKey != null) {
            viewModel.selectCounter(initialCounterKey)
        }
    }

    val selectedEntity = uiState.counters.find { it.counterKey == uiState.selectedCounterKey }
    val currentCount = selectedEntity?.count ?: 0
    val targetCount = selectedEntity?.target ?: 100
    val titleArabic = selectedEntity?.titleArabic ?: "المسبحة والأذكار"
    val progress = (currentCount.toFloat() / targetCount.toFloat()).coerceAtMost(1f)
    val isCompleted = currentCount >= targetCount

    fun performHaptic() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(35)
                }
            }
        } catch (e: Exception) {
            // gracefully ignore
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "المسبحة والأذكار 🤍",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "ألا بذكر الله تطمئن القلوب",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Counter Types Horizontal Selector
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DefaultHabits.ALL_COUNTERS) { habitDef ->
                    val isSelected = habitDef.key == uiState.selectedCounterKey
                    val entity = uiState.counters.find { it.counterKey == habitDef.key }
                    val itemDone = (entity?.count ?: 0) >= habitDef.defaultTarget

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCounter(habitDef.key) },
                        label = {
                            Text(
                                text = if (itemDone) "${habitDef.titleArabic} ✓" else habitDef.titleArabic,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            // Main Active Dhikr Phrase
            Text(
                text = titleArabic,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "الهدف اليومي: $targetCount مرة",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(0.6f))

            // Huge Circular Touch Target Area for One-Handed Interaction
            Box(
                modifier = Modifier
                    .size(270.dp)
                    .clip(CircleShape)
                    .shadow(16.dp, CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isCompleted) {
                                listOf(Emerald700, Emerald900)
                            } else {
                                listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
                            }
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 135.dp, color = MaterialTheme.colorScheme.primary),
                        onClick = {
                            performHaptic()
                            viewModel.increment()
                        }
                    )
                    .testTag("big_counter_tap_btn"),
                contentAlignment = Alignment.Center
            ) {
                // Circular Progress Indicator Ring
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(250.dp),
                    color = if (isCompleted) WarmGold else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    strokeWidth = 10.dp
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$currentCount",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 62.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isCompleted) "ما شاء الله! اكتمل الهدف" else "اضغط هنا للعد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCompleted) WarmGoldLight else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Progress Summary Pill
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الإنجاز الحالي:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$currentCount من $targetCount ($((progress * 100).toInt()}%)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) StatusCongregation else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Celebration Banner
        MicroCelebrationBanner(
            visible = uiState.showCelebration,
            title = uiState.celebrationTitle,
            subtitle = "تقبل الله طاعتك ورزقك الثبات والسكينة",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // Reset Confirmation Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("إعادة ضبط العداد") },
                text = { Text("هل تريد تصفير عداد \"$titleArabic\" لهذا اليوم؟") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.reset()
                            showResetDialog = false
                        }
                    ) {
                        Text("نعم، تصفير", color = StatusMissed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}
