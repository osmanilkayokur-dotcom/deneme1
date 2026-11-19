package dev.kok.app.domain.usecase

import dev.kok.app.domain.model.BodyRegion
import dev.kok.app.domain.model.Exercise
import dev.kok.app.domain.model.MuscleGroup
import dev.kok.app.domain.model.MuscleRole
import dev.kok.app.domain.model.MuscleTarget
import dev.kok.app.domain.model.PlannedExercise
import dev.kok.app.domain.model.WorkoutDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutUseCasesTest {
    private val chest = MuscleGroup(1, "CHEST", "Göğüs", "mesh_chest", BodyRegion.UPPER_FRONT)
    private val triceps = MuscleGroup(2, "TRICEPS", "Triceps", "mesh_tri", BodyRegion.ARMS)

    private fun planned(id: Long, vararg targets: MuscleTarget) =
        PlannedExercise(id, Exercise(id, "E$id", targets.toList()), id.toInt(), 3, 8, 12, null, null)

    private fun day(id: Long, vararg ex: PlannedExercise) = WorkoutDay(id, 1, id.toInt(), "Gün $id", ex.toList())

    @Test
    fun `birincil rol ikincile ustun gelir`() {
        val d = day(
            1,
            planned(1, MuscleTarget(chest, MuscleRole.PRIMARY), MuscleTarget(triceps, MuscleRole.SECONDARY)),
            planned(2, MuscleTarget(triceps, MuscleRole.PRIMARY)),
        )
        val h = MuscleHighlight.of(d)
        assertEquals(MuscleRole.PRIMARY, h.roleOf("CHEST"))
        assertEquals(MuscleRole.PRIMARY, h.roleOf("TRICEPS"))
        assertNull(h.roleOf("QUADS"))
    }

    @Test
    fun `sonraki gun donguseldir`() {
        val days = listOf(day(10), day(11), day(12))
        assertEquals(10L, ObserveTrainingTodayUseCase.suggestNext(days, null)?.id)
        assertEquals(11L, ObserveTrainingTodayUseCase.suggestNext(days, 10)?.id)
        assertEquals(10L, ObserveTrainingTodayUseCase.suggestNext(days, 12)?.id)
        assertEquals(10L, ObserveTrainingTodayUseCase.suggestNext(days, 999)?.id)
        assertNull(ObserveTrainingTodayUseCase.suggestNext(emptyList(), 1))
    }
}
