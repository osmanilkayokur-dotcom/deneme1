package dev.kok.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.kok.app.presentation.feature.dashboard.DashboardRoute as DashboardScreen
import dev.kok.app.presentation.feature.chat.ChatRoute as ChatScreen
import dev.kok.app.presentation.feature.nutrition.AddMealRouteScreen
import dev.kok.app.presentation.feature.settings.SettingsRoute as SettingsScreen
import dev.kok.app.presentation.feature.profile.ProfileRoute as ProfileScreen
import kotlin.reflect.KClass

private data class TopTab(val label: String, val icon: ImageVector, val route: Any, val routeClass: KClass<*>)

private val tabs = listOf(
    TopTab("Bugün", Icons.Outlined.Eco, DashboardRoute, DashboardRoute::class),
    TopTab("Asistan", Icons.AutoMirrored.Outlined.Chat, ChatRoute, ChatRoute::class),
    TopTab("Profil", Icons.Outlined.Person, ProfileRoute(showBack = false), ProfileRoute::class),
)

@Composable
fun KokApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBar = tabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true } &&
        backStack?.toRouteOrNull<ProfileRoute>()?.showBack != true

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            NavHost(navController = nav, startDestination = DashboardRoute) {
                composable<DashboardRoute> {
                    DashboardScreen(
                        onAddMeal = { date -> nav.navigate(AddMealRoute(date.toString())) },
                        onOpenProfile = { nav.navigate(ProfileRoute(showBack = true)) },
                    )
                }
                composable<ProfileRoute> { entry ->
                    val route = entry.toRoute<ProfileRoute>()
                    ProfileScreen(
                        showBack = route.showBack,
                        onDone = { if (route.showBack) nav.popBackStack() },
                    )
                }
                composable<ChatRoute> {
                    ChatScreen(onOpenSettings = { nav.navigate(SettingsRoute) })
                }
                composable<SettingsRoute> {
                    SettingsScreen(onBack = { nav.popBackStack() })
                }
                composable<AddMealRoute> {
                    AddMealRouteScreen(
                        onBack = { nav.popBackStack() },
                        onSaved = { nav.popBackStack() },
                    )
                }
            }
        }
    }
}

private inline fun <reified T : Any> androidx.navigation.NavBackStackEntry.toRouteOrNull(): T? =
    if (destination.hasRoute(T::class)) toRoute<T>() else null
