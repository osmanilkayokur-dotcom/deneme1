package dev.kok.app.domain.engine

import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Sayısal toplamları gerçek dünya ölçeklerine çevirir:
 * "Bu ay 1,4 el arabası kadar yemek yedin." gibi.
 *
 * Her kategori için ölçek birimleri küçükten büyüğe tanımlıdır; toplamın en az
 * [MIN_WHOLE_UNITS] katına ulaştığı en büyük birim seçilir. Hiçbiri yetmezse en küçük
 * birimin yüzdesi söylenir ("bir küvetin %60'ı"). Yeni birim eklemek için listeye satır eklemek yeterlidir.
 */
class StatsMetaphorMapper @Inject constructor() {

    enum class Period(val phrase: String) { WEEK("Bu hafta"), MONTH("Bu ay") }

    data class ScaleUnit(val singular: String, val size: Double, val possessive: String)

    data class Metaphor(val headline: String, val unit: String, val amount: Double, val rawValue: Double)

    val foodUnitsKg = listOf(
        ScaleUnit("karpuz", 5.0, "bir karpuzun"),
        ScaleUnit("pirinç çuvalı", 25.0, "bir pirinç çuvalının"),
        ScaleUnit("el arabası", 80.0, "bir el arabasının"),
        ScaleUnit("kamyonet kasası", 1000.0, "bir kamyonet kasasının"),
    )

    val waterUnitsL = listOf(
        ScaleUnit("damacana", 19.0, "bir damacananın"),
        ScaleUnit("küvet", 150.0, "bir küvetin"),
        ScaleUnit("su tankeri", 5000.0, "bir su tankerinin"),
    )

    fun foodWeight(totalKg: Double, period: Period): Metaphor =
        scale(totalKg, foodUnitsKg, period) { "kadar yemek yedin." }

    fun water(totalLiters: Double, period: Period): Metaphor =
        scale(totalLiters, waterUnitsL, period) { "kadar su içtin." }

    /**
     * Koşuda yaklaşık 1 kcal/kg/km harcanır; yürüyüşte ~0,5 kcal/kg/km.
     */
    fun burnedEnergy(totalKcal: Double, weightKg: Double, period: Period): Metaphor {
        require(weightKg > 0) { "Kilo pozitif olmalı" }
        val km = totalKcal / (RUN_KCAL_PER_KG_KM * weightKg)
        val text = "${period.phrase} yaktığın enerjiyle yaklaşık ${format(km)} km koşabilirdin."
        return Metaphor(text, "km koşu", km, totalKcal)
    }

    private fun scale(value: Double, units: List<ScaleUnit>, period: Period, tail: (Double) -> String): Metaphor {
        require(units.isNotEmpty())
        val safe = value.coerceAtLeast(0.0)
        val chosen = units.lastOrNull { safe / it.size >= MIN_WHOLE_UNITS }
        return if (chosen != null) {
            val amount = safe / chosen.size
            Metaphor("${period.phrase} ${format(amount)} ${chosen.singular} ${tail(amount)}", chosen.singular, amount, safe)
        } else {
            val first = units.first()
            val percent = (safe / first.size * 100.0).roundToInt()
            Metaphor(
                "${period.phrase} ${first.possessive} %$percent'${TurkishSuffix.possessive(percent)} ${tail(percent.toDouble())}",
                first.singular,
                safe / first.size,
                safe,
            )
        }
    }

    private fun format(v: Double, maxFraction: Int = 1): String =
        NumberFormat.getNumberInstance(TR).apply {
            maximumFractionDigits = maxFraction
            minimumFractionDigits = 0
        }.format(v)

    companion object {
        const val MIN_WHOLE_UNITS = 1.0
        const val RUN_KCAL_PER_KG_KM = 1.0
        private val TR: Locale = Locale.forLanguageTag("tr-TR")
    }
}
