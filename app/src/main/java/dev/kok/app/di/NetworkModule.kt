package dev.kok.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.kok.app.data.remote.gemini.GeminiGateway
import dev.kok.app.data.security.SecureSettingsStore
import dev.kok.app.domain.repository.AssistantGateway
import dev.kok.app.domain.repository.AssistantSettingsRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        // Düşünen modeller uzun yanıt süresine ihtiyaç duyabilir.
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantModule {
    @Binds @Singleton abstract fun gateway(impl: GeminiGateway): AssistantGateway
    @Binds @Singleton abstract fun settings(impl: SecureSettingsStore): AssistantSettingsRepository
}
