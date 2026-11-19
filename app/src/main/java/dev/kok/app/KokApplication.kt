package dev.kok.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.kok.app.core.ApplicationScope
import dev.kok.app.data.local.seed.DatabaseSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class KokApplication : Application() {
    @Inject lateinit var seeder: DatabaseSeeder

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch { seeder.seed() }
    }
}
