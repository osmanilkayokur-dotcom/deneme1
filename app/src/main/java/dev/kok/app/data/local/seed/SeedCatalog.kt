package dev.kok.app.data.local.seed

import dev.kok.app.domain.model.BodyRegion
import dev.kok.app.domain.model.MuscleRole
import dev.kok.app.domain.model.MuscleRole.PRIMARY
import dev.kok.app.domain.model.MuscleRole.SECONDARY

/**
 * Uygulamanın ilk açılışında yüklenen sabit katalog.
 * `meshNodeId` değerleri Faz 4'te seçilecek .glb modelin node adlarıyla eşlenecek.
 */
object SeedCatalog {

    data class MuscleSeed(val code: String, val displayName: String, val meshNodeId: String, val region: BodyRegion)

    data class ExerciseSeed(val name: String, val muscles: Map<String, MuscleRole>)

    val muscles = listOf(
        MuscleSeed("CHEST", "Göğüs", "mesh_pectoralis_major", BodyRegion.UPPER_FRONT),
        MuscleSeed("FRONT_DELT", "Ön Omuz", "mesh_deltoid_anterior", BodyRegion.UPPER_FRONT),
        MuscleSeed("SIDE_DELT", "Yan Omuz", "mesh_deltoid_lateral", BodyRegion.UPPER_FRONT),
        MuscleSeed("REAR_DELT", "Arka Omuz", "mesh_deltoid_posterior", BodyRegion.UPPER_BACK),
        MuscleSeed("BICEPS", "Biceps", "mesh_biceps_brachii", BodyRegion.ARMS),
        MuscleSeed("TRICEPS", "Triceps", "mesh_triceps_brachii", BodyRegion.ARMS),
        MuscleSeed("FOREARMS", "Ön Kol", "mesh_forearm_flexors", BodyRegion.ARMS),
        MuscleSeed("LATS", "Kanat (Latissimus)", "mesh_latissimus_dorsi", BodyRegion.UPPER_BACK),
        MuscleSeed("TRAPS", "Trapez", "mesh_trapezius", BodyRegion.UPPER_BACK),
        MuscleSeed("RHOMBOIDS", "Romboid / Orta Sırt", "mesh_rhomboids", BodyRegion.UPPER_BACK),
        MuscleSeed("LOWER_BACK", "Bel (Erector Spinae)", "mesh_erector_spinae", BodyRegion.UPPER_BACK),
        MuscleSeed("ABS", "Karın", "mesh_rectus_abdominis", BodyRegion.CORE),
        MuscleSeed("OBLIQUES", "Yan Karın", "mesh_obliques", BodyRegion.CORE),
        MuscleSeed("GLUTES", "Kalça", "mesh_gluteus_maximus", BodyRegion.LOWER),
        MuscleSeed("QUADS", "Ön Bacak (Quadriceps)", "mesh_quadriceps", BodyRegion.LOWER),
        MuscleSeed("HAMSTRINGS", "Arka Bacak (Hamstring)", "mesh_hamstrings", BodyRegion.LOWER),
        MuscleSeed("ADDUCTORS", "İç Bacak (Adduktor)", "mesh_adductors", BodyRegion.LOWER),
        MuscleSeed("CALVES", "Kalf", "mesh_gastrocnemius", BodyRegion.LOWER),
    )

    val exercises = listOf(
        ExerciseSeed("Bench Press", mapOf("CHEST" to PRIMARY, "FRONT_DELT" to SECONDARY, "TRICEPS" to SECONDARY)),
        ExerciseSeed("Incline Dumbbell Press", mapOf("CHEST" to PRIMARY, "FRONT_DELT" to SECONDARY, "TRICEPS" to SECONDARY)),
        ExerciseSeed("Dumbbell Fly", mapOf("CHEST" to PRIMARY, "FRONT_DELT" to SECONDARY)),
        ExerciseSeed("Cable Crossover", mapOf("CHEST" to PRIMARY, "FRONT_DELT" to SECONDARY)),
        ExerciseSeed("Dips", mapOf("CHEST" to PRIMARY, "TRICEPS" to PRIMARY, "FRONT_DELT" to SECONDARY)),
        ExerciseSeed("Şınav", mapOf("CHEST" to PRIMARY, "TRICEPS" to SECONDARY, "FRONT_DELT" to SECONDARY, "ABS" to SECONDARY)),
        ExerciseSeed("Overhead Press", mapOf("FRONT_DELT" to PRIMARY, "SIDE_DELT" to SECONDARY, "TRICEPS" to SECONDARY, "TRAPS" to SECONDARY)),
        ExerciseSeed("Dumbbell Shoulder Press", mapOf("FRONT_DELT" to PRIMARY, "SIDE_DELT" to SECONDARY, "TRICEPS" to SECONDARY)),
        ExerciseSeed("Lateral Raise", mapOf("SIDE_DELT" to PRIMARY, "TRAPS" to SECONDARY)),
        ExerciseSeed("Face Pull", mapOf("REAR_DELT" to PRIMARY, "RHOMBOIDS" to SECONDARY, "TRAPS" to SECONDARY)),
        ExerciseSeed("Reverse Fly", mapOf("REAR_DELT" to PRIMARY, "RHOMBOIDS" to SECONDARY)),
        ExerciseSeed("Triceps Pushdown", mapOf("TRICEPS" to PRIMARY)),
        ExerciseSeed("Overhead Triceps Extension", mapOf("TRICEPS" to PRIMARY)),
        ExerciseSeed("Skull Crusher", mapOf("TRICEPS" to PRIMARY)),
        ExerciseSeed("Barfiks", mapOf("LATS" to PRIMARY, "BICEPS" to SECONDARY, "RHOMBOIDS" to SECONDARY, "FOREARMS" to SECONDARY)),
        ExerciseSeed("Lat Pulldown", mapOf("LATS" to PRIMARY, "BICEPS" to SECONDARY, "RHOMBOIDS" to SECONDARY)),
        ExerciseSeed("Barbell Row", mapOf("LATS" to PRIMARY, "RHOMBOIDS" to PRIMARY, "REAR_DELT" to SECONDARY, "BICEPS" to SECONDARY, "LOWER_BACK" to SECONDARY)),
        ExerciseSeed("Seated Cable Row", mapOf("RHOMBOIDS" to PRIMARY, "LATS" to PRIMARY, "BICEPS" to SECONDARY, "REAR_DELT" to SECONDARY)),
        ExerciseSeed("One Arm Dumbbell Row", mapOf("LATS" to PRIMARY, "RHOMBOIDS" to SECONDARY, "BICEPS" to SECONDARY)),
        ExerciseSeed("Shrug", mapOf("TRAPS" to PRIMARY, "FOREARMS" to SECONDARY)),
        ExerciseSeed("Deadlift", mapOf("LOWER_BACK" to PRIMARY, "GLUTES" to PRIMARY, "HAMSTRINGS" to PRIMARY, "TRAPS" to SECONDARY, "QUADS" to SECONDARY, "FOREARMS" to SECONDARY)),
        ExerciseSeed("Romanian Deadlift", mapOf("HAMSTRINGS" to PRIMARY, "GLUTES" to PRIMARY, "LOWER_BACK" to SECONDARY)),
        ExerciseSeed("Barbell Curl", mapOf("BICEPS" to PRIMARY, "FOREARMS" to SECONDARY)),
        ExerciseSeed("Dumbbell Curl", mapOf("BICEPS" to PRIMARY, "FOREARMS" to SECONDARY)),
        ExerciseSeed("Hammer Curl", mapOf("BICEPS" to PRIMARY, "FOREARMS" to PRIMARY)),
        ExerciseSeed("Squat", mapOf("QUADS" to PRIMARY, "GLUTES" to PRIMARY, "ADDUCTORS" to SECONDARY, "LOWER_BACK" to SECONDARY, "HAMSTRINGS" to SECONDARY)),
        ExerciseSeed("Front Squat", mapOf("QUADS" to PRIMARY, "GLUTES" to SECONDARY, "ABS" to SECONDARY)),
        ExerciseSeed("Leg Press", mapOf("QUADS" to PRIMARY, "GLUTES" to SECONDARY, "HAMSTRINGS" to SECONDARY)),
        ExerciseSeed("Bulgarian Split Squat", mapOf("QUADS" to PRIMARY, "GLUTES" to PRIMARY, "ADDUCTORS" to SECONDARY)),
        ExerciseSeed("Lunge", mapOf("QUADS" to PRIMARY, "GLUTES" to PRIMARY, "HAMSTRINGS" to SECONDARY)),
        ExerciseSeed("Leg Extension", mapOf("QUADS" to PRIMARY)),
        ExerciseSeed("Leg Curl", mapOf("HAMSTRINGS" to PRIMARY, "CALVES" to SECONDARY)),
        ExerciseSeed("Hip Thrust", mapOf("GLUTES" to PRIMARY, "HAMSTRINGS" to SECONDARY)),
        ExerciseSeed("Adductor Machine", mapOf("ADDUCTORS" to PRIMARY)),
        ExerciseSeed("Standing Calf Raise", mapOf("CALVES" to PRIMARY)),
        ExerciseSeed("Plank", mapOf("ABS" to PRIMARY, "OBLIQUES" to SECONDARY, "LOWER_BACK" to SECONDARY)),
        ExerciseSeed("Hanging Leg Raise", mapOf("ABS" to PRIMARY, "OBLIQUES" to SECONDARY, "FOREARMS" to SECONDARY)),
        ExerciseSeed("Cable Crunch", mapOf("ABS" to PRIMARY, "OBLIQUES" to SECONDARY)),
        ExerciseSeed("Russian Twist", mapOf("OBLIQUES" to PRIMARY, "ABS" to SECONDARY)),
        ExerciseSeed("Back Extension", mapOf("LOWER_BACK" to PRIMARY, "GLUTES" to SECONDARY, "HAMSTRINGS" to SECONDARY)),
    )
}
