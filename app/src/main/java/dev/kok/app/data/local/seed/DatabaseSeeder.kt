package dev.kok.app.data.local.seed

import androidx.room.withTransaction
import dev.kok.app.data.local.KokDatabase
import dev.kok.app.data.local.entity.ExerciseEntity
import dev.kok.app.data.local.entity.ExerciseMuscleCrossRef
import dev.kok.app.data.local.entity.MuscleGroupEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Katalog verisini idempotent biçimde yükler (UNIQUE + IGNORE).
 * Her açılışta çalışır; yalnızca eksik satırları ekler, kullanıcı verisine dokunmaz.
 */
@Singleton
class DatabaseSeeder @Inject constructor(private val db: KokDatabase) {

    suspend fun seed() {
        val dao = db.workoutDao()
        db.withTransaction {
            dao.insertMuscleGroups(
                SeedCatalog.muscles.map {
                    MuscleGroupEntity(code = it.code, displayName = it.displayName, meshNodeId = it.meshNodeId, bodyRegion = it.region)
                },
            )
            val muscleIds = dao.getMuscleGroups().associate { it.code to it.id }
            SeedCatalog.exercises.forEach { seed ->
                val existing = dao.findExerciseByName(seed.name)
                if (existing == null) {
                    val id = dao.insertExercise(ExerciseEntity(name = seed.name))
                    dao.insertCrossRefs(
                        seed.muscles.mapNotNull { (code, role) ->
                            muscleIds[code]?.let { ExerciseMuscleCrossRef(id, it, role) }
                        },
                    )
                }
            }
        }
    }
}
