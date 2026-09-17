package com.mporttech.pro.navigation

object NavigationRoutes {
    val topLevel = listOf(
        Screen.Dashboard.route,
        Screen.Network.route,
        Screen.Tools.route,
        Screen.Alerts.route,
        Screen.Profile.route
    )

    fun isTopLevel(route: String?): Boolean =
        route != null && topLevel.any { route == it || route.startsWith("$it/") }
}
