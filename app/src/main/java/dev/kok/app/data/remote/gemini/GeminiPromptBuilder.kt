package dev.kok.app.data.remote.gemini

import dev.kok.app.domain.model.AssistantContext
import dev.kok.app.domain.model.Sex
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Sistem talimatı: rol, kurallar ve güncel uygulama bağlamı. */
internal object GeminiPromptBuilder {

    fun systemInstruction(ctx: AssistantContext): String = buildString {
        val tr = Locale.forLanguageTag("tr-TR")
        appendLine("Sen 'Kök' adlı kişisel beslenme ve antrenman uygulamasının asistanısın. Her zaman Türkçe, kısa ve net yanıt ver.")
        appendLine("Bugünün tarihi: ${ctx.today} (${ctx.today.format(DateTimeFormatter.ofPattern("EEEE", tr))}).")
        appendLine()
        appendLine("KURALLAR")
        appendLine("- Kullanıcı bir şey yediğini/içtiğini söylediğinde log_meal aracını çağır. Her yiyeceği ayrı öğe yap.")
        appendLine("- Gramaj verilmemişse Türkiye'deki tipik porsiyonu varsay (ör. 1 tabak pilav ≈ 180 g, 1 kase yoğurt ≈ 200 g, 1 dilim ekmek ≈ 30 g, 1 yumurta ≈ 55 g).")
        appendLine("- Besin değerlerini güvenilir besin veri tabanı ortalamalarına göre tahmin et; pişmiş/çiğ durumunu dikkate al.")
        appendLine("- category: pirinç/bulgur/ekmek/makarna=GRAIN_RICE, et/tavuk/balık/yumurta/baklagil=PROTEIN, sebze=VEGETABLE, meyve=FRUIT, süt/yoğurt/peynir/ayran=DAIRY, diğer=OTHER.")
        appendLine("- Spor programı oluşturma veya değiştirme isteklerinde create_or_update_workout_program aracını çağır.")
        appendLine("- Mevcut programı güncellerken aynı adı kullan ve programın TAM halini (değişmeyen günler dahil) gönder.")
        appendLine("- Katalogdaki egzersiz adlarını birebir kullan; katalogda yoksa yeni ad ver ve muscles alanını yalnızca aşağıdaki kas kodlarıyla doldur.")
        appendLine("- Araç çağrısı kullanıcı onayına sunulur. Sonucu araç yanıtı gelmeden 'kaydettim' deme.")
        appendLine("- Tıbbi teşhis koyma; sağlık riski görürsen uzmana yönlendir.")
        appendLine()
        ctx.profile?.let { p ->
            appendLine("KULLANICI PROFİLİ")
            appendLine(
                "- ${if (p.sex == Sex.MALE) "Erkek" else "Kadın"}, ${p.age} yaş, ${p.heightCm.toInt()} cm, ${p.weightKg} kg, aktivite: ${p.activityLevel.label}",
            )
            val g = p.goals
            appendLine("- Günlük hedefler: protein ${g.proteinG.toInt()} g, karbonhidrat ${g.carbG.toInt()} g, yağ ${g.fatG.toInt()} g, lif ${g.fiberG.toInt()} g, su ${g.waterMl} ml")
            appendLine()
        }
        appendLine("KAS GRUBU KODLARI")
        appendLine(ctx.muscles.joinToString { "${it.code} (${it.displayName})" })
        appendLine()
        appendLine("EGZERSİZ KATALOĞU")
        appendLine(ctx.exerciseNames.joinToString())
        appendLine()
        if (ctx.programs.isEmpty()) {
            appendLine("MEVCUT PROGRAMLAR: yok")
        } else {
            appendLine("MEVCUT PROGRAMLAR")
            ctx.programs.forEach { prog ->
                appendLine("* ${prog.name}${if (prog.isActive) " [AKTİF]" else ""}")
                prog.days.forEach { day ->
                    val ex = day.exercises.joinToString("; ") { pe ->
                        val reps = if (pe.targetRepsMin == pe.targetRepsMax) "${pe.targetRepsMin}" else "${pe.targetRepsMin}-${pe.targetRepsMax}"
                        "${pe.exercise.name} ${pe.targetSets}x$reps" + (pe.targetWeightKg?.let { " @${it}kg" } ?: "")
                    }
                    appendLine("  - ${day.name}: $ex")
                }
            }
        }
    }
}
