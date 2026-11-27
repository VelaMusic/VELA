package com.vela.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * "The Thread" — Vela's bottom navigation.
 *
 * Four destinations, lowercase labels, and one literal thread: a 28dp accent
 * line that slides along the bar to sit under the selected tab, like a thread
 * being pulled through. No badges, no red dots, no mini-player mimicry.
 * The now-playing strip (owned by the player agent) sits above this bar.
 *
 * Routes mirror the existing nav graph: "home", "search_input", "library",
 * "settings". Tune opens the settings screen owned by the player/settings
 * agent — this file only places the destination in the Thread.
 *
 * NOTE (integrator): Listen Together keeps its existing route
 * ("listen_together") but is deliberately NOT a Thread destination — four
 * tabs is the design. Its entry point must live in the player (Queue River)
 * or in Tune, placed by the player/settings agent. Do not let it go
 * unreachable: it is a backend feature, not a cut.
 */
enum class VelaDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Flow(
        route = "home",
        label = "flow",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
    ),
    Compass(
        route = "search_input",
        label = "compass",
        icon = Icons.Outlined.Search,
        selectedIcon = Icons.Filled.Search,
    ),
    Shelf(
        route = "library",
        label = "shelf",
        icon = Icons.Outlined.LibraryMusic,
        selectedIcon = Icons.Filled.LibraryMusic,
    ),
    Tune(
        route = "settings",
        label = "tune",
        icon = Icons.Outlined.Tune,
        selectedIcon = Icons.Filled.Tune,
    ),
}

@Composable
fun VelaThreadBar(navController: NavController) {
    val destinations = VelaDestination.entries
    val currentRoute = navController.currentBackStackEntryAsState().value
        ?.destination?.route
    val selectedIndex = destinations
        .indexOfFirst { it.route == currentRoute }
        .takeIf { it >= 0 } ?: 0

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val segmentWidth = maxWidth / destinations.size
        val threadOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex + segmentWidth / 2 - 14.dp,
            label = "thread",
        )
        Column {
            // The thread: a short accent line sliding under the selected tab.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = threadOffset)
                        .width(28.dp)
                        .height(3.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(50),
                        ),
                )
            }
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
            ) {
                destinations.forEachIndexed { index, destination ->
                    val selected = index == selectedIndex
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon
                                else destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.inverseOnSurface,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        }
    }
}
