package com.example.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.PrayerStatus
import com.example.core.prayer.PrayerType
import com.example.data.local.entities.CounterRecordEntity
import com.example.data.local.entities.HabitRecordEntity
import com.example.data.local.entities.PrayerRecordEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCounter: (counterKey: String?) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var activePrayerToEdit by remember { mutableStateOf<PrayerRecordEntity?>(null) }
    var showReflectionSheet by remember { mutableStateOf(false) }
    var showQuranDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = 20.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Greeting & Date
            item {
                HomeHeader(
                    cityName = uiState.cityName,
                    fullArabicDate = uiState.fullArabicDate,
                    onSettingsClick = onNavigateToSettings
                )
            }

            // 2. Day Progress Overview Card
            item {
                DayProgressCard(
                    progressPercent = uiState.dailyProgressPercent,
                    encouragement = uiState.encouragementMessage
                )
            }

            // 3. Next Prayer Countdown Banner
            item {
                uiState.nextPrayerInfo?.let { nextInfo ->
                    NextPrayerBanner(
                        nextPrayerInfo = nextInfo,
                        cityName = uiState.cityName
                    )
                }
            }

            // 4. Five Daily Prayers Section
            item {
                SectionHeader(
                    title = "الصلاة المفروضة",
                    subtitle = "اضغط على الصلاة لتسجيل حالتها (جماعة / منفرد / لم أصلِّ)",
                    icon = Icons.Default.Mosque
                )
            }

            item {
                PrayersHorizontalRow(
                    prayers = uiState.prayers,
                    prayerTimes = uiState.prayerTimes,
                    onPrayerClick = { prayerRecord ->
                        activePrayerToEdit = prayerRecord
                    }
                )
            }

            // 5. Next Step Suggestion Card
            item {
                NextStepCard(suggestion = uiState.nextStepSuggestion)
            }

            // 6. Daily Ibadat Checklist Section
            item {
                SectionHeader(
                    title = "عباداتك اليومية",
                    subtitle = "حافظ على وردك والسنن الراتبة",
                    icon = Icons.AutoMirrored.Filled.MenuBook
                )
            }

            items(uiState.habits) { habit ->
                HabitCardItem(
                    habit = habit,
                    onToggle = { isChecked ->
                        if (habit.habitKey == "quran_wird") {
                            showQuranDialog = true
                        } else {
                            viewModel.toggleHabit(habit.habitKey, isChecked)
                        }
                    },
                    onOpenDetails = {
                        if (habit.habitKey == "quran_wird") {
                            showQuranDialog = true
                        }
                    }
                )
            }

            // 7. Daily Dhikr & Quick Counters Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "الأذكار والمسبحة",
                        subtitle = "اضغط للعد المباشر السريع",
                        icon = Icons.Default.TouchApp
                    )
                    TextButton(onClick = { onNavigateToCounter(null) }) {
                        Text("فتح المسبحة", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            items(uiState.counters) { counter ->
                QuickCounterRowItem(
                    counter = counter,
                    onQuickTap = { viewModel.quickIncrementCounter(counter.counterKey) },
                    onOpenFullCounter = { onNavigateToCounter(counter.counterKey) }
                )
            }

            // 8. End-of-day Reflection Prompt Card
            item {
                ReflectionPromptCard(
                    reflection = uiState.reflection,
                    onClick = { showReflectionSheet = true }
                )
            }
        }

        // Micro Celebration Banner
        MicroCelebrationBanner(
            visible = uiState.showCelebration,
            title = uiState.celebrationText,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // Prayer Bottom Sheet
        activePrayerToEdit?.let { prayer ->
            val prayerAr = when (prayer.prayer) {
                "FAJR" -> "الفجر"
                "DHUHR" -> "الظهر"
                "ASR" -> "العصر"
                "MAGHRIB" -> "المغرب"
                "ISHA" -> "العشاء"
                else -> prayer.prayer
            }
            val timeStr = when (prayer.prayer) {
                "FAJR" -> uiState.prayerTimes?.fajr ?: prayer.scheduledTime
                "DHUHR" -> uiState.prayerTimes?.dhuhr ?: prayer.scheduledTime
                "ASR" -> uiState.prayerTimes?.asr ?: prayer.scheduledTime
                "MAGHRIB" -> uiState.prayerTimes?.maghrib ?: prayer.scheduledTime
                "ISHA" -> uiState.prayerTimes?.isha ?: prayer.scheduledTime
                else -> prayer.scheduledTime
            }
            val statusEnum = try {
                PrayerStatus.valueOf(prayer.status)
            } catch (e: Exception) {
                PrayerStatus.UNRECORDED
            }

            PrayerBottomSheet(
                prayerNameArabic = prayerAr,
                prayerTime = timeStr,
                currentStatus = statusEnum,
                currentReason = prayer.reason,
                currentCustomReason = prayer.customReason,
                onDismiss = { activePrayerToEdit = null },
                onSave = { newStatus, newReason, newCustomReason ->
                    viewModel.logPrayer(prayer.prayer, newStatus, newReason, newCustomReason)
                    activePrayerToEdit = null
                }
            )
        }

        // Quran Wird Dialog
        if (showQuranDialog) {
            val quranHabit = uiState.habits.find { it.habitKey == "quran_wird" }
            QuranWirdDialog(
                initialCompleted = quranHabit?.isCompleted ?: false,
                initialNotes = quranHabit?.notes,
                onDismiss = { showQuranDialog = false },
                onSave = { isCompleted, notes ->
                    viewModel.toggleHabit("quran_wird", isCompleted, notes)
                    showQuranDialog = false
                }
            )
        }

        // Reflection Bottom Sheet
        if (showReflectionSheet) {
            DailyReflectionSheet(
                prayers = uiState.prayers,
                habits = uiState.habits,
                currentStruggledHabit = uiState.reflection?.struggledHabit,
                currentReason = uiState.reflection?.struggleReason,
                currentCustomReason = uiState.reflection?.customReason,
                currentNote = uiState.reflection?.note,
                onDismiss = { showReflectionSheet = false },
                onSave = { struggledHabit, reason, customReason, note ->
                    viewModel.saveDailyReflection(struggledHabit, reason, customReason, note)
                    showReflectionSheet = false
                }
            )
        }
    }
}

@Composable
fun HomeHeader(
    cityName: String,
    fullArabicDate: String,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "السلام عليكم 🤍",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "City",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$cityName • $fullArabicDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "الإعدادات",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DayProgressCard(
    progressPercent: Int,
    encouragement: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "يومك اليوم",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = encouragement,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Progress Circular Display
            Box(
                modifier = Modifier.size(68.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 6.dp
                )
                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun NextPrayerBanner(
    nextPrayerInfo: com.example.core.prayer.NextPrayerInfo,
    cityName: String
) {
    val nextPrayerNameAr = when (nextPrayerInfo.nextPrayer) {
        PrayerType.FAJR -> "صلاة الفجر"
        PrayerType.SUNRISE -> "الشروق"
        PrayerType.DHUHR -> "صلاة الظهر"
        PrayerType.ASR -> "صلاة العصر"
        PrayerType.MAGHRIB -> "صلاة المغرب"
        PrayerType.ISHA -> "صلاة العشاء"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Emerald800,
                        Emerald900
                    )
                )
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "الأذان القادم",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGoldLight
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = nextPrayerNameAr,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = nextPrayerInfo.timeRemainingFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = nextPrayerInfo.nextPrayerTimeFormatted,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PrayersHorizontalRow(
    prayers: List<PrayerRecordEntity>,
    prayerTimes: com.example.core.prayer.DailyPrayerTimes?,
    onPrayerClick: (PrayerRecordEntity) -> Unit
) {
    val orderedPrayers = listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        orderedPrayers.forEach { prayerKey ->
            val entity = prayers.find { it.prayer == prayerKey } ?: PrayerRecordEntity(
                id = "_$prayerKey",
                dayKey = "",
                prayer = prayerKey,
                scheduledTime = "",
                status = PrayerStatus.UNRECORDED.name
            )

            val nameAr = when (prayerKey) {
                "FAJR" -> "الفجر"
                "DHUHR" -> "الظهر"
                "ASR" -> "العصر"
                "MAGHRIB" -> "المغرب"
                "ISHA" -> "العشاء"
                else -> prayerKey
            }

            val timeStr = when (prayerKey) {
                "FAJR" -> prayerTimes?.fajr ?: entity.scheduledTime
                "DHUHR" -> prayerTimes?.dhuhr ?: entity.scheduledTime
                "ASR" -> prayerTimes?.asr ?: entity.scheduledTime
                "MAGHRIB" -> prayerTimes?.maghrib ?: entity.scheduledTime
                "ISHA" -> prayerTimes?.isha ?: entity.scheduledTime
                else -> entity.scheduledTime
            }

            PrayerCardItem(
                prayerNameArabic = nameAr,
                prayerTime = timeStr,
                status = entity.status,
                modifier = Modifier.weight(1f),
                onClick = { onPrayerClick(entity) }
            )
        }
    }
}

@Composable
fun PrayerCardItem(
    prayerNameArabic: String,
    prayerTime: String,
    status: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (statusLabel, statusBg, statusColor) = when (status) {
        "CONGREGATION" -> Triple("جماعة 🕌", StatusCongregationBg, StatusCongregation)
        "INDIVIDUAL" -> Triple("منفرد ✓", StatusIndividualBg, StatusIndividual)
        "MISSED" -> Triple("لم أصلِّ ✕", StatusMissedBg, StatusMissed)
        else -> Triple("لم تسجل ○", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("prayer_card_$prayerNameArabic"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (status != "UNRECORDED") 1.5.dp else 1.dp,
            color = if (status != "UNRECORDED") statusColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = prayerNameArabic,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = prayerTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusBg)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NextStepCard(suggestion: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                contentDescription = "Next step",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "الخطوة التالية 🤍",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun HabitCardItem(
    habit: HabitRecordEntity,
    onToggle: (Boolean) -> Unit,
    onOpenDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetails),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (habit.isCompleted) StatusCongregationBg.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (habit.isCompleted) StatusCongregation.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val icon = when (habit.habitKey) {
                    "quran_wird" -> Icons.AutoMirrored.Filled.MenuBook
                    "duha_prayer" -> Icons.Default.WbSunny
                    "witr_prayer" -> Icons.Default.Star
                    "sleep_azkar" -> Icons.Default.NightlightRound
                    else -> Icons.Default.Favorite
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (habit.isCompleted) StatusCongregation.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = habit.titleArabic,
                        tint = if (habit.isCompleted) StatusCongregation else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = habit.titleArabic,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!habit.notes.isNullOrEmpty()) {
                        Text(
                            text = habit.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Checkbox(
                checked = habit.isCompleted,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = StatusCongregation
                ),
                modifier = Modifier.testTag("habit_checkbox_${habit.habitKey}")
            )
        }
    }
}

@Composable
fun QuickCounterRowItem(
    counter: CounterRecordEntity,
    onQuickTap: () -> Unit,
    onOpenFullCounter: () -> Unit
) {
    val progress = (counter.count.toFloat() / counter.target.toFloat()).coerceAtMost(1f)
    val isFinished = counter.count >= counter.target

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenFullCounter),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFinished) StatusCongregationBg.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isFinished) StatusCongregation.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = counter.titleArabic,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isFinished) StatusCongregation else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${counter.count} / ${counter.target}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Tap Button
            Button(
                onClick = onQuickTap,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFinished) StatusCongregation else MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(46.dp)
                    .testTag("quick_counter_btn_${counter.counterKey}")
            ) {
                Icon(
                    imageVector = if (isFinished) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = "Increment",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ReflectionPromptCard(
    reflection: com.example.data.local.entities.DailyReflectionEntity?,
    onClick: () -> Unit
) {
    val isDone = reflection?.isCompleted == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) Emerald50 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        border = BorderStroke(1.dp, if (isDone) Emerald300 else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isDone) StatusCongregationBg else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (isDone) "✓" else "🌙", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isDone) "تمت محاسبة النفس اليوم 🤍" else "كيف كان يومك؟",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isDone) "اضغط لمراجعة أو تعديل الملاحظات" else "وقفة هادئة أقل من ٣٠ ثانية لختام يومك",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Open reflection",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
