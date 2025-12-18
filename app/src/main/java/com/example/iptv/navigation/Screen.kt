package com.example.iptv.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Categories : Screen("categories")
    object Channels : Screen("channels/{categoryId}/{categoryName}") {
        fun createRoute(categoryId: String, categoryName: String): String {
            return "channels/$categoryId/$categoryName"
        }
    }
}
