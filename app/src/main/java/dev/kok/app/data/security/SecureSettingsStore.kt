package dev.kok.app.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.kok.app.core.IoDispatcher
import dev.kok.app.domain.repository.AssistantSettings
import dev.kok.app.domain.repository.AssistantSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini API anahtarı Android Keystore destekli AES-256 ile şifrelenmiş tercihlerde saklanır.
 * Anahtar hiçbir zaman kaynak koda, BuildConfig'e veya yedeğe girmez (yedekleme kapalı).
 */
@Singleton
class SecureSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AssistantSettingsRepository {

    /**
     * Keystore anahtarı bozulursa (ör. cihaz yedekten geri yüklendiğinde) dosya okunamaz;
     * bu durumda şifreli dosya silinip yeniden oluşturulur — kullanıcı anahtarı tekrar girer.
     */
    private val prefs: SharedPreferences by lazy {
        runCatching { create() }.getOrElse {
            context.deleteSharedPreferences(FILE_NAME)
            create()
        }
    }

    private fun create(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val state = MutableStateFlow<AssistantSettings?>(null)

    override fun observe(): Flow<AssistantSettings?> = state.asStateFlow().onStart {
        if (state.value == null) state.value = withContext(io) { read() }
    }

    override suspend fun get(): AssistantSettings = withContext(io) { read() }.also { state.value = it }

    override suspend fun save(apiKey: String?, model: String) = withContext(io) {
        prefs.edit().apply {
            if (apiKey != null) putString(KEY_API, apiKey.trim())
            putString(KEY_MODEL, model.trim().ifBlank { DEFAULT_MODEL })
        }.commit()
        state.value = read()
    }

    override suspend fun clearApiKey() = withContext(io) {
        prefs.edit().remove(KEY_API).commit()
        state.value = read()
    }

    private fun read() = AssistantSettings(
        apiKey = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() },
        model = prefs.getString(KEY_MODEL, null) ?: DEFAULT_MODEL,
    )

    companion object {
        private const val FILE_NAME = "kok_secure_settings"
        private const val KEY_API = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model"
        const val DEFAULT_MODEL = "gemini-flash-latest"
    }
}
