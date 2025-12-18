package com.example.iptv.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Categories : Screen("categories")
    object Channels : Screen("channels/{categoryId}/{categoryName}") {
        fun createRoute(categoryId: String, categoryName: String): String {
            return "channels/$categoryId/$categoryName"
        }
    }
    object Movies : Screen("movies")
    object MovieDetail : Screen("movie_detail/{movieId}") {
        fun createRoute(movieId: Int): String {
            return "movie_detail/$movieId"
        }
    }
    object Series : Screen("series")
    object SeriesDetail : Screen("series_detail/{seriesId}") {
        fun createRoute(seriesId: Int): String {
            return "series_detail/$seriesId"
        }
    }
    object Player : Screen("player/{streamUrl}/{title}") {
        fun createRoute(streamUrl: String, title: String): String {
            return "player/${java.net.URLEncoder.encode(streamUrl, "UTF-8")}/${java.net.URLEncoder.encode(title, "UTF-8")}"
        }
    }
    object VodPlayer : Screen("vod_player/{contentId}/{contentType}") {
        fun createRoute(contentId: String, contentType: String): String {
            return "vod_player/${java.net.URLEncoder.encode(contentId, "UTF-8")}/${java.net.URLEncoder.encode(contentType, "UTF-8")}"
        }
    }
}
