package dev.kok.app.data.remote.gemini

import dev.kok.app.domain.model.AssistantAction
import dev.kok.app.domain.model.ChatMessage
import dev.kok.app.domain.model.ChatRole
import dev.kok.app.domain.model.FoodCategory
import dev.kok.app.domain.model.MuscleRole
import dev.kok.app.domain.repository.AssistantSettings
import dev.kok.app.domain.repository.AssistantSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class GeminiGatewayTest {
    private val settings = object : AssistantSettingsRepository {
        override fun observe(): Flow<AssistantSettings?> = flowOf(AssistantSettings("k", "m"))
        override suspend fun get() = AssistantSettings("k", "m")
        override suspend fun save(apiKey: String?, model: String) = Unit
        override suspend fun clearApiKey() = Unit
    }
    private val gateway = GeminiGateway(OkHttpClient(), settings, Dispatchers.Unconfined)
    private val today = LocalDate.of(2026, 11, 20)

    private fun modelCall(name: String, args: String) = ChatMessage(
        id = 2,
        role = ChatRole.MODEL,
        text = "",
        createdAt = Instant.EPOCH,
        toolName = name,
        toolPayloadJson = """{"role":"model","parts":[{"functionCall":{"name":"$name","args":$args},"thoughtSignature":"abc"}]}""",
    )

    @Test
    fun `log_meal ayristirilir ve gelecek tarih bugune cekilir`() {
        val msg = modelCall(
            "log_meal",
            """{"date":"2030-01-01","meals":[{"name":"Pilav","grams":180,"kcal":234,"protein_g":4.3,"carb_g":50,"fat_g":0.5,"fiber_g":0.7,"category":"grain_rice"}]}""",
        )
        val action = gateway.parseAction(msg, today) as AssistantAction.LogMeals
        assertEquals(1, action.meals.size)
        assertEquals(today, action.meals[0].date)
        assertEquals(FoodCategory.GRAIN_RICE, action.meals[0].category)
        assertEquals(234.0, action.meals[0].nutrients.kcal, 0.001)
    }

    @Test
    fun `bilinmeyen kategori OTHER olur ve dun tarihi korunur`() {
        val msg = modelCall(
            "log_meal",
            """{"date":"2026-11-19","meals":[{"name":"X","grams":10,"kcal":5,"protein_g":0,"carb_g":1,"fat_g":0,"fiber_g":0,"category":"SNACK"}]}""",
        )
        val action = gateway.parseAction(msg, today) as AssistantAction.LogMeals
        assertEquals(FoodCategory.OTHER, action.meals[0].category)
        assertEquals(LocalDate.of(2026, 11, 19), action.meals[0].date)
    }

    @Test
    fun `program ayristirilir ve gecersiz kas rolu atlanir`() {
        val msg = modelCall(
            "create_or_update_workout_program",
            """{"name":"PPL","activate":true,"days":[{"name":"İtiş","exercises":[{"name":"Landmine Press","sets":3,"reps_min":10,"reps_max":8,"muscles":[{"code":"chest","role":"PRIMARY"},{"code":"TRICEPS","role":"WRONG"}]}]}]}""",
        )
        val action = gateway.parseAction(msg, today) as AssistantAction.UpsertProgram
        val ex = action.draft.days.single().exercises.single()
        assertEquals(10, ex.repsMin)
        assertEquals(10, ex.repsMax) // max < min ise min'e çekilir
        assertEquals(mapOf("CHEST" to MuscleRole.PRIMARY), ex.muscles)
    }

    @Test
    fun `yanitsiz fonksiyon cagrisina iptal yaniti sentezlenir`() {
        val history = listOf(
            ChatMessage(1, ChatRole.USER, "pilav yedim", Instant.EPOCH),
            modelCall("log_meal", """{"meals":[]}"""),
            ChatMessage(3, ChatRole.USER, "boşver", Instant.EPOCH),
        )
        val contents = gateway.buildContents(history)
        assertEquals(4, contents.size)
        val synthesized = contents[2].jsonObject
        assertEquals("user", synthesized["role"]!!.jsonPrimitive.content)
        assertTrue(synthesized.toString().contains("functionResponse"))
        // Düşünce imzası geçmişte korunur
        assertTrue(contents[1].toString().contains("thoughtSignature"))
    }

    @Test
    fun `gecmis kullanici mesajiyla baslar`() {
        val history = listOf(
            ChatMessage(1, ChatRole.TOOL, "x", Instant.EPOCH, toolName = "log_meal", toolPayloadJson = "{}"),
            ChatMessage(2, ChatRole.USER, "merhaba", Instant.EPOCH),
        )
        val contents = gateway.buildContents(history)
        assertEquals(1, contents.size)
    }

    @Test
    fun `fonksiyon cagrisi olmayan mesaj aksiyon dondurmez`() {
        val msg = ChatMessage(5, ChatRole.MODEL, "selam", Instant.EPOCH, toolPayloadJson = """{"role":"model","parts":[{"text":"selam"}]}""")
        assertNull(gateway.parseAction(msg, today))
    }
}
