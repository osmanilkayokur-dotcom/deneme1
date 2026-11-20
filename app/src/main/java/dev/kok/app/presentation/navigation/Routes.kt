package dev.kok.app.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable data object DashboardRoute

@Serializable data class ProfileRoute(val showBack: Boolean = true)

/** [date] ISO yyyy-MM-dd */
@Serializable data class AddMealRoute(val date: String)

@Serializable data object ChatRoute

@Serializable data object SettingsRoute

@Serializable data object WorkoutRoute

/** [programId] 0 ise yeni program. */
@Serializable data class ProgramEditorRoute(val programId: Long = 0)

@Serializable data object BodyRoute

@Serializable data object StatsRoute
