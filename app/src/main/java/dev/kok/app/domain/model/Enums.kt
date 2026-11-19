package dev.kok.app.domain.model

enum class Sex { MALE, FEMALE }

/** Aktivite düzeyi ve TDEE çarpanı (Mifflin-St Jeor ile kullanılan standart katsayılar). */
enum class ActivityLevel(val multiplier: Double, val label: String) {
    SEDENTARY(1.2, "Hareketsiz (masa başı)"),
    LIGHT(1.375, "Hafif (haftada 1–3 antrenman)"),
    MODERATE(1.55, "Orta (haftada 3–5 antrenman)"),
    ACTIVE(1.725, "Yüksek (haftada 6–7 antrenman)"),
    VERY_ACTIVE(1.9, "Çok yüksek (günde 2 antrenman / fiziksel iş)"),
}

enum class EntrySource { MANUAL, GEMINI_CHAT }

enum class FoodCategory(val label: String) {
    GRAIN_RICE("Tahıl / Pirinç"),
    PROTEIN("Protein"),
    VEGETABLE("Sebze"),
    FRUIT("Meyve"),
    DAIRY("Süt Ürünü"),
    OTHER("Diğer"),
}

enum class MuscleRole { PRIMARY, SECONDARY }

enum class ChatRole { USER, MODEL, TOOL }

enum class BodyRegion { UPPER_FRONT, UPPER_BACK, CORE, LOWER, ARMS }
