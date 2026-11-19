package dev.kok.app.data.remote.gemini

import dev.kok.app.core.IoDispatcher
import dev.kok.app.domain.model.AssistantAction
import dev.kok.app.domain.model.AssistantContext
import dev.kok.app.domain.model.AssistantReply
import dev.kok.app.domain.model.ChatMessage
import dev.kok.app.domain.model.ChatRole
import dev.kok.app.domain.model.DayDraft
import dev.kok.app.domain.model.EntrySource
import dev.kok.app.domain.model.ExerciseDraft
import dev.kok.app.domain.model.FoodCategory
import dev.kok.app.domain.model.MicroNutrients
import dev.kok.app.domain.model.MuscleRole
import dev.kok.app.domain.model.NewMeal
import dev.kok.app.domain.model.Nutrients
import dev.kok.app.domain.model.ProgramDraft
import dev.kok.app.domain.model.ToolOutcome
import dev.kok.app.domain.repository.AssistantGateway
import dev.kok.app.domain.repository.AssistantNotConfiguredException
import dev.kok.app.domain.repository.AssistantRequestException
import dev.kok.app.domain.repository.AssistantSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gemini generateContent REST istemcisi (resmi Function Calling).
 * Model içeriği (functionCall + thoughtSignature) olduğu gibi saklanıp geçmişe geri gönderilir;
 * bu, düşünen modellerde çok adımlı araç kullanımının zorunlu koşuludur.
 */
@Singleton
class GeminiGateway @Inject constructor(
    private val http: OkHttpClient,
    private val settings: AssistantSettingsRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AssistantGateway {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }

    override suspend fun generate(history: List<ChatMessage>, context: AssistantContext): AssistantReply {
        val cfg = settings.get()
        val key = cfg.apiKey ?: throw AssistantNotConfiguredException()
        val body = buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") { add(buildJsonObject { put("text", GeminiPromptBuilder.systemInstruction(context)) }) }
            }
            put("contents", buildContents(history))
            put("tools", GeminiToolSchemas.tools)
            put("toolConfig", GeminiToolSchemas.toolConfig)
            putJsonObject("generationConfig") { put("temperature", 0.4) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/models/${cfg.model}:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

        val responseText = withContext(io) { execute(request) }
        val root = json.parseToJsonElement(responseText).jsonObject
        val candidate = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw AssistantRequestException(blockedReason(root))
        val content = candidate["content"]?.jsonObject
            ?: throw AssistantRequestException("Model boş yanıt döndü (${candidate["finishReason"]?.jsonPrimitive?.contentOrNull ?: "bilinmiyor"})")
        val parts = content["parts"]?.jsonArray.orEmpty().map { it.jsonObject }

        val text = parts
            .filter { it["thought"]?.jsonPrimitive?.booleanOrNull != true }
            .mapNotNull { it["text"]?.jsonPrimitive?.contentOrNull }
            .joinToString("").trim()
        val raw = buildJsonObject {
            put("role", "model")
            put("parts", JsonArray(parts))
        }.toString()

        val call = parts.firstNotNullOfOrNull { it["functionCall"]?.jsonObject }
        if (call != null) {
            val action = decodeCall(call, context.today)
            return AssistantReply.Action(action, text.ifBlank { null }, raw)
        }
        return AssistantReply.Text(text.ifBlank { "…" }, raw)
    }

    override fun parseAction(message: ChatMessage, today: LocalDate): AssistantAction? {
        val raw = message.toolPayloadJson ?: return null
        return runCatching {
            val parts = json.parseToJsonElement(raw).jsonObject["parts"]?.jsonArray.orEmpty()
            val call = parts.firstNotNullOfOrNull { it.jsonObject["functionCall"]?.jsonObject } ?: return null
            decodeCall(call, today)
        }.getOrNull()
    }

    override fun encodeOutcome(toolName: String, outcome: ToolOutcome): String = buildJsonObject {
        put("role", "user")
        putJsonArray("parts") {
            add(
                buildJsonObject {
                    putJsonObject("functionResponse") {
                        put("name", toolName)
                        putJsonObject("response") {
                            put("status", outcome.status.name)
                            put("message", outcome.message)
                        }
                    }
                },
            )
        }
    }.toString()

    private fun decodeCall(call: JsonObject, today: LocalDate): AssistantAction {
        val name = call["name"]?.jsonPrimitive?.contentOrNull
        val args = call["args"]?.jsonObject ?: JsonObject(emptyMap())
        return when (name) {
            AssistantAction.TOOL_LOG_MEAL -> {
                val a = json.decodeFromJsonElement<LogMealArgs>(args)
                val date = a.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?.takeIf { !it.isAfter(today) } ?: today
                AssistantAction.LogMeals(a.meals.map { it.toNewMeal(date) })
            }
            AssistantAction.TOOL_UPSERT_PROGRAM -> {
                val a = json.decodeFromJsonElement<ProgramArgs>(args)
                AssistantAction.UpsertProgram(
                    ProgramDraft(
                        name = a.name,
                        source = EntrySource.GEMINI_CHAT,
                        days = a.days.map { d ->
                            DayDraft(
                                name = d.name,
                                exercises = d.exercises.map { e ->
                                    ExerciseDraft(
                                        name = e.name,
                                        sets = e.sets,
                                        repsMin = e.repsMin,
                                        repsMax = maxOf(e.repsMin, e.repsMax),
                                        weightKg = e.weightKg,
                                        notes = e.notes,
                                        muscles = e.muscles.mapNotNull { m ->
                                            runCatching { m.code.uppercase() to MuscleRole.valueOf(m.role.uppercase()) }.getOrNull()
                                        }.toMap(),
                                    )
                                },
                            )
                        },
                    ),
                    activate = a.activate,
                )
            }
            else -> throw AssistantRequestException("Bilinmeyen araç çağrısı: $name")
        }
    }

    private fun MealArg.toNewMeal(date: LocalDate) = NewMeal(
        date = date,
        name = name,
        grams = grams,
        nutrients = Nutrients(
            kcal = kcal,
            proteinG = proteinG,
            carbG = carbG,
            fatG = fatG,
            fiberG = fiberG,
            micros = MicroNutrients(sodiumMg, potassiumMg, ironMg, vitaminCMg, vitaminDMcg),
        ),
        source = EntrySource.GEMINI_CHAT,
        category = runCatching { FoodCategory.valueOf(category.uppercase()) }.getOrDefault(FoodCategory.OTHER),
    )

    /**
     * Saklanan mesajları Gemini `contents` dizisine çevirir. Yanıtsız kalmış fonksiyon çağrılarına
     * (kullanıcı onaylamadan yeni mesaj yazdıysa) "IGNORED" yanıtı sentezlenir; API her
     * functionCall'dan sonra functionResponse bekler.
     */
    internal fun buildContents(history: List<ChatMessage>): JsonArray = buildJsonArray {
        val trimmed = history.takeLast(MAX_HISTORY).dropWhile { it.role != ChatRole.USER }
        trimmed.forEachIndexed { i, msg ->
            when (msg.role) {
                ChatRole.USER -> add(
                    buildJsonObject {
                        put("role", "user")
                        putJsonArray("parts") { add(buildJsonObject { put("text", msg.text) }) }
                    },
                )
                ChatRole.MODEL -> {
                    val raw = msg.toolPayloadJson?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
                    add(
                        raw ?: buildJsonObject {
                            put("role", "model")
                            putJsonArray("parts") { add(buildJsonObject { put("text", msg.text) }) }
                        },
                    )
                    val hasCall = raw?.get("parts")?.jsonArray?.any { it.jsonObject.containsKey("functionCall") } == true
                    val next = trimmed.getOrNull(i + 1)
                    if (hasCall && next?.role != ChatRole.TOOL) {
                        add(
                            json.parseToJsonElement(
                                encodeOutcome(
                                    msg.toolName ?: "unknown",
                                    ToolOutcome(ToolOutcome.Status.CANCELLED, "Kullanıcı bu öneriyi yanıtlamadan devam etti."),
                                ),
                            ),
                        )
                    }
                }
                ChatRole.TOOL -> msg.toolPayloadJson?.let { add(json.parseToJsonElement(it)) }
            }
        }
    }

    private suspend fun execute(request: Request): String = suspendCancellableCoroutine { cont ->
        val call = http.newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWithException(AssistantRequestException("Bağlantı kurulamadı: ${e.message ?: "ağ hatası"}"))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val text = it.body?.string().orEmpty()
                        if (it.isSuccessful) {
                            cont.resume(text)
                        } else {
                            cont.resumeWithException(AssistantRequestException(errorMessage(it.code, text), it.code))
                        }
                    }
                }
            },
        )
    }

    private fun errorMessage(code: Int, body: String): String {
        val apiMessage = runCatching {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        val friendly = when (code) {
            400 -> if (apiMessage?.contains("API key", ignoreCase = true) == true) "API anahtarı geçersiz." else "İstek reddedildi."
            401, 403 -> "API anahtarı yetkisiz ya da kısıtlı."
            404 -> "Model bulunamadı; Ayarlar'dan model adını kontrol et."
            429 -> "Gemini kotası doldu; biraz sonra tekrar dene."
            in 500..599 -> "Gemini sunucusu şu an yanıt veremiyor."
            else -> "Beklenmeyen hata ($code)."
        }
        return if (apiMessage != null) "$friendly ($apiMessage)" else friendly
    }

    private fun blockedReason(root: JsonObject): String {
        val reason = root["promptFeedback"]?.jsonObject?.get("blockReason")?.jsonPrimitive?.contentOrNull
        return if (reason != null) "İstek güvenlik filtresine takıldı ($reason)." else "Model yanıt üretmedi."
    }

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private const val MAX_HISTORY = 40
    }
}
