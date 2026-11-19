package dev.kok.app.data.remote.gemini

import dev.kok.app.domain.model.AssistantAction
import dev.kok.app.domain.model.FoodCategory
import dev.kok.app.domain.model.MuscleRole
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Gemini Function Calling bildirimleri (OpenAPI alt kümesi şema). */
internal object GeminiToolSchemas {

    private fun prop(type: String, description: String, enum: List<String>? = null) = buildJsonObject {
        put("type", type)
        put("description", description)
        if (enum != null) put("enum", JsonArray(enum.map(::JsonPrimitive)))
    }

    private fun obj(required: List<String>, properties: JsonObjectBuilder.() -> Unit) = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties", properties)
        put("required", JsonArray(required.map(::JsonPrimitive)))
    }

    private fun array(description: String, items: JsonObject) = buildJsonObject {
        put("type", "ARRAY")
        put("description", description)
        put("items", items)
    }

    private val mealItem = obj(
        required = listOf("name", "grams", "kcal", "protein_g", "carb_g", "fat_g", "fiber_g", "category"),
    ) {
        put("name", prop("STRING", "Yemeğin kısa Türkçe adı, ör. 'Tavuk göğsü (ızgara)'"))
        put("grams", prop("NUMBER", "Yenen miktar gram cinsinden (porsiyon tahmini dahil)"))
        put("kcal", prop("NUMBER", "Bu miktarın toplam kalorisi"))
        put("protein_g", prop("NUMBER", "Protein (g)"))
        put("carb_g", prop("NUMBER", "Karbonhidrat (g)"))
        put("fat_g", prop("NUMBER", "Yağ (g)"))
        put("fiber_g", prop("NUMBER", "Lif (g)"))
        put("sodium_mg", prop("NUMBER", "Sodyum (mg)"))
        put("potassium_mg", prop("NUMBER", "Potasyum (mg)"))
        put("iron_mg", prop("NUMBER", "Demir (mg)"))
        put("vitamin_c_mg", prop("NUMBER", "C vitamini (mg)"))
        put("vitamin_d_mcg", prop("NUMBER", "D vitamini (mcg)"))
        put("category", prop("STRING", "Animasyon kategorisi", FoodCategory.entries.map { it.name }))
    }

    private val logMeal = buildJsonObject {
        put("name", AssistantAction.TOOL_LOG_MEAL)
        put(
            "description",
            "Kullanıcının yediğini söylediği yemekleri ayrıştırıp günlük beslenme kaydına ekler. " +
                "Her yemek ayrı bir öğe olmalı; değerler verilen gramaj için mutlak değerlerdir.",
        )
        put(
            "parameters",
            obj(required = listOf("meals")) {
                put("meals", array("Kaydedilecek yemekler", mealItem))
                put("date", prop("STRING", "Kayıt tarihi (YYYY-MM-DD). Kullanıcı 'dün' gibi bir gün belirtmediyse boş bırak."))
            },
        )
    }

    private val exerciseItem = obj(required = listOf("name", "sets", "reps_min", "reps_max")) {
        put("name", prop("STRING", "Egzersiz adı. Katalogda varsa birebir aynı adı kullan."))
        put("sets", prop("INTEGER", "Set sayısı"))
        put("reps_min", prop("INTEGER", "Alt tekrar sayısı"))
        put("reps_max", prop("INTEGER", "Üst tekrar sayısı (sabitse alt ile aynı)"))
        put("weight_kg", prop("NUMBER", "Hedef ağırlık (kg), biliniyorsa"))
        put("notes", prop("STRING", "Kısa not (tempo, dinlenme vb.)"))
        put(
            "muscles",
            array(
                "Yalnızca katalogda OLMAYAN yeni egzersizler için çalıştırdığı kaslar",
                obj(required = listOf("code", "role")) {
                    put("code", prop("STRING", "Kas grubu kodu"))
                    put("role", prop("STRING", "Birincil/ikincil", MuscleRole.entries.map { it.name }))
                },
            ),
        )
    }

    private val upsertProgram = buildJsonObject {
        put("name", AssistantAction.TOOL_UPSERT_PROGRAM)
        put(
            "description",
            "Yeni bir spor programı oluşturur ya da aynı adlı mevcut programı TAMAMEN yeniden yazar. " +
                "Güncellemede değişmeyen günler dahil programın tam halini gönder.",
        )
        put(
            "parameters",
            obj(required = listOf("name", "days")) {
                put("name", prop("STRING", "Program adı, ör. 'PPL', 'Upper/Lower'. Güncellemede mevcut adı aynen kullan."))
                put("activate", prop("BOOLEAN", "Program aktif program yapılsın mı"))
                put(
                    "days",
                    array(
                        "Sıralı antrenman günleri",
                        obj(required = listOf("name", "exercises")) {
                            put("name", prop("STRING", "Gün adı, ör. 'Gün 1: İtiş'"))
                            put("exercises", array("Günün egzersizleri (sırayla)", exerciseItem))
                        },
                    ),
                )
            },
        )
    }

    val tools: JsonArray = buildJsonArray {
        add(
            buildJsonObject {
                putJsonArray("functionDeclarations") {
                    add(logMeal)
                    add(upsertProgram)
                }
            },
        )
    }

    val toolConfig: JsonObject = buildJsonObject {
        putJsonObject("functionCallingConfig") { put("mode", "AUTO") }
    }
}
