package dev.kok.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.kok.app.data.local.KokDatabase
import dev.kok.app.data.local.KokMigrations
import dev.kok.app.data.local.dao.ChatDao
import dev.kok.app.data.local.dao.ProfileDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KokDatabase =
        Room.databaseBuilder(context, KokDatabase::class.java, KokDatabase.NAME)
            .addMigrations(*KokMigrations.ALL)
            .build()

    @Provides fun provideProfileDao(db: KokDatabase): ProfileDao = db.profileDao()
    @Provides fun provideChatDao(db: KokDatabase): ChatDao = db.chatDao()
}
