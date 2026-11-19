package dev.kok.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.kok.app.data.repository.BodyRepositoryImpl
import dev.kok.app.data.repository.ChatRepositoryImpl
import dev.kok.app.data.repository.NutritionRepositoryImpl
import dev.kok.app.data.repository.ProfileRepositoryImpl
import dev.kok.app.data.repository.WorkoutRepositoryImpl
import dev.kok.app.domain.repository.BodyRepository
import dev.kok.app.domain.repository.ChatRepository
import dev.kok.app.domain.repository.NutritionRepository
import dev.kok.app.domain.repository.ProfileRepository
import dev.kok.app.domain.repository.WorkoutRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun profile(impl: ProfileRepositoryImpl): ProfileRepository
    @Binds @Singleton abstract fun nutrition(impl: NutritionRepositoryImpl): NutritionRepository
    @Binds @Singleton abstract fun workout(impl: WorkoutRepositoryImpl): WorkoutRepository
    @Binds @Singleton abstract fun body(impl: BodyRepositoryImpl): BodyRepository
    @Binds @Singleton abstract fun chat(impl: ChatRepositoryImpl): ChatRepository
}
