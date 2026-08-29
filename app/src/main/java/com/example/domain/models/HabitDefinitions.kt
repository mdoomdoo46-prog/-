package com.example.domain.models

data class HabitDefinition(
    val key: String,
    val titleArabic: String,
    val categoryArabic: String,
    val defaultTarget: Int = 1,
    val unitArabic: String = "مرة",
    val isCounter: Boolean = false,
    val iconName: String = "check"
)

object DefaultHabits {
    val QURAN_WIRD = HabitDefinition("quran_wird", "ورد القرآن", "القرآن الكريم", 1, "ورد", isCounter = false, iconName = "book")
    val SLEEP_AZKAR = HabitDefinition("sleep_azkar", "أذكار قبل النوم", "الأذكار", 1, "مرة", isCounter = false, iconName = "bed")
    val DUA_DAILY = HabitDefinition("dua_daily", "الدعاء مرة واحدة يوميًا", "الدعاء", 1, "مرة", isCounter = false, iconName = "hands")
    val WITR = HabitDefinition("witr_prayer", "صلاة الوتر", "السنن والرواتب", 1, "صلاة", isCounter = false, iconName = "star")
    val DUHA = HabitDefinition("duha_prayer", "صلاة الضحى", "السنن والرواتب", 1, "صلاة", isCounter = false, iconName = "sun")

    // Counters
    val ISTIGHFAR_200 = HabitDefinition("counter_istighfar", "200 استغفار", "الأذكار والمسبحة", 200, "استغفار", isCounter = true, iconName = "counter")
    val TAHMID_33 = HabitDefinition("counter_tahmid", "33 الحمد لله", "الأذكار والمسبحة", 33, "تسبيح", isCounter = true, iconName = "counter")
    val TASBIH_33 = HabitDefinition("counter_tasbih", "33 سبحان الله", "الأذكار والمسبحة", 33, "تسبيح", isCounter = true, iconName = "counter")
    val TAKBIR_33 = HabitDefinition("counter_takbir", "33 الله أكبر", "الأذكار والمسبحة", 33, "تسبيح", isCounter = true, iconName = "counter")
    val SALAT_NABI_MORNING = HabitDefinition("counter_salat_nabi_morning", "الصلاة على النبي ﷺ (صباحًا)", "الأذكار والمسبحة", 10, "مرات", isCounter = true, iconName = "counter")
    val SALAT_NABI_EVENING = HabitDefinition("counter_salat_nabi_evening", "الصلاة على النبي ﷺ (مساءً)", "الأذكار والمسبحة", 10, "مرات", isCounter = true, iconName = "counter")

    val ALL_DAILY_HABITS = listOf(
        QURAN_WIRD,
        DUHA,
        WITR,
        SLEEP_AZKAR,
        DUA_DAILY
    )

    val ALL_COUNTERS = listOf(
        ISTIGHFAR_200,
        TASBIH_33,
        TAHMID_33,
        TAKBIR_33,
        SALAT_NABI_MORNING,
        SALAT_NABI_EVENING
    )
}

object PrayerReasons {
    val MISSED_REASONS = listOf(
        "نسيت",
        "كنت نائمًا",
        "كنت مشغولًا",
        "كنت خارج المنزل",
        "كنت متعبًا",
        "لم أجد جماعة",
        "فاتني الوقت",
        "ظرف طارئ",
        "سبب آخر",
        "لا أريد الإجابة"
    )

    val INDIVIDUAL_REASONS = listOf(
        "لم أجد جماعة",
        "كنت في المنزل",
        "كنت خارج المنزل",
        "استيقظت متأخرًا",
        "كنت مشغولًا",
        "لم أستطع الوصول للمسجد",
        "سبب آخر",
        "لا أريد الإجابة"
    )

    val REFLECTION_STRUGGLE_REASONS = listOf(
        "نسيت",
        "كنت مشغولًا",
        "لم يكن عندي وقت",
        "كنت متعبًا",
        "بدأت اليوم متأخرًا",
        "لم أشعر بالحماس",
        "حصل ظرف",
        "سبب آخر",
        "لا أريد الإجابة"
    )
}
