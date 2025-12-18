package com.example.iptv.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.iptv.data.model.Episode
import com.example.iptv.data.model.Movie
import com.example.iptv.data.model.Series
import com.example.iptv.data.repository.FavoritesRepository
import com.example.iptv.data.repository.XtreamRepository
import com.example.iptv.ui.login.LoginScreen
import com.example.iptv.ui.main.MainScreen
import com.example.iptv.ui.movies.MovieDetailScreen
import com.example.iptv.ui.movies.MoviesScreen
import com.example.iptv.ui.player.ContentType
import com.example.iptv.ui.player.PlayerScreen
import com.example.iptv.ui.player.VodContent
import com.example.iptv.ui.player.VodPlayerScreen
import com.example.iptv.ui.series.SeriesDetailScreen
import com.example.iptv.ui.series.SeriesScreen
import java.net.URLDecoder

// Helper functions to create VodContent
fun Movie.toVodContent(repository: XtreamRepository): VodContent {
        return VodContent(
                id = streamId.toString(),
                title = name,
                streamUrl = repository.getMovieStreamUrl(streamId, containerExtension ?: "mp4"),
                poster = streamIcon,
                type = ContentType.MOVIE
        )
}

fun Episode.toVodContent(series: Series, repository: XtreamRepository): VodContent {
        return VodContent(
                id = id,
                title = title,
                streamUrl =
                        repository.getEpisodeStreamUrl(
                                series.seriesId,
                                id,
                                containerExtension ?: "mp4"
                        ),
                poster = info?.movieImage ?: series.cover,
                duration = info?.durationSecs?.toLong()?.times(1000), // Convert to milliseconds
                type = ContentType.EPISODE
        )
}

@Composable
fun IPTVNavigation(navController: NavHostController = rememberNavController()) {
        var repository: XtreamRepository? by remember { mutableStateOf(null) }
        var selectedMovie by remember { mutableStateOf<Movie?>(null) }
        var selectedSeries by remember { mutableStateOf<Series?>(null) }

        NavHost(navController = navController, startDestination = Screen.Login.route) {
                composable(Screen.Login.route) {
                        LoginScreen(
                                onLoginSuccess = { repo ->
                                        repository = repo
                                        navController.navigate(Screen.Categories.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                }
                        )
                }

                composable(Screen.Categories.route) {
                        repository?.let { repo ->
                                MainScreen(
                                        repository = repo,
                                        onNavigateToMovies = {
                                                navController.navigate(Screen.Movies.route)
                                        },
                                        onNavigateToSeries = {
                                                navController.navigate(Screen.Series.route)
                                        }
                                )
                        }
                }

                composable(Screen.Movies.route) {
                        repository?.let { repo ->
                                MoviesScreen(
                                        repository = repo,
                                        onMovieClick = { movie ->
                                                selectedMovie = movie
                                                navController.navigate(
                                                        Screen.MovieDetail.createRoute(
                                                                movie.streamId
                                                        )
                                                )
                                        },
                                        onBack = { navController.popBackStack() }
                                )
                        }
                }

                composable(
                        route = Screen.MovieDetail.route,
                        arguments = listOf(navArgument("movieId") { type = NavType.IntType })
                ) { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
                        repository?.let { repo ->
                                selectedMovie?.let { movie ->
                                        MovieDetailScreen(
                                                movie = movie,
                                                repository = repo,
                                                onPlay = {
                                                        navController.navigate(
                                                                Screen.VodPlayer.createRoute(
                                                                        movie.streamId.toString(),
                                                                        "movie"
                                                                )
                                                        )
                                                },
                                                onBack = { navController.popBackStack() }
                                        )
                                }
                        }
                }

                composable(Screen.Series.route) {
                        repository?.let { repo ->
                                SeriesScreen(
                                        repository = repo,
                                        onSeriesClick = { series ->
                                                selectedSeries = series
                                                navController.navigate(
                                                        Screen.SeriesDetail.createRoute(
                                                                series.seriesId
                                                        )
                                                )
                                        },
                                        onBack = { navController.popBackStack() }
                                )
                        }
                }

                composable(
                        route = Screen.SeriesDetail.route,
                        arguments = listOf(navArgument("seriesId") { type = NavType.IntType })
                ) { backStackEntry ->
                        val seriesId = backStackEntry.arguments?.getInt("seriesId") ?: 0
                        repository?.let { repo ->
                                selectedSeries?.let { series ->
                                        SeriesDetailScreen(
                                                series = series,
                                                repository = repo,
                                                onEpisodePlay = { episode, _ ->
                                                        navController.navigate(
                                                                Screen.VodPlayer.createRoute(
                                                                        episode.id,
                                                                        "episode_${series.seriesId}"
                                                                )
                                                        )
                                                },
                                                onBack = { navController.popBackStack() }
                                        )
                                }
                        }
                }

                composable(
                        route = Screen.Player.route,
                        arguments =
                                listOf(
                                        navArgument("streamUrl") { type = NavType.StringType },
                                        navArgument("title") { type = NavType.StringType }
                                )
                ) { backStackEntry ->
                        val context = LocalContext.current
                        val streamUrl =
                                backStackEntry.arguments?.getString("streamUrl")?.let {
                                        URLDecoder.decode(it, "UTF-8")
                                }
                                        ?: ""
                        val title =
                                backStackEntry.arguments?.getString("title")?.let {
                                        URLDecoder.decode(it, "UTF-8")
                                }
                                        ?: ""

                        // Create a dummy channel for the player
                        val dummyChannel =
                                com.example.iptv.data.model.Channel(
                                        num = 0,
                                        name = title,
                                        streamType = "movie",
                                        streamId = 0,
                                        streamIcon = null,
                                        epgChannelId = null,
                                        added = null,
                                        categoryId = "0",
                                        customSid = null,
                                        tvArchive = null,
                                        directSource = null,
                                        tvArchiveDuration = null
                                )

                        repository?.let { repo ->
                                PlayerScreen(
                                        streamUrl = streamUrl,
                                        channelName = title,
                                        currentChannel = dummyChannel,
                                        channelList = emptyList(),
                                        currentChannelIndex = 0,
                                        repository = repo,
                                        favoritesRepository = FavoritesRepository(context),
                                        onChannelChange = {},
                                        onBack = { navController.popBackStack() }
                                )
                        }
                }

                composable(
                        route = Screen.VodPlayer.route,
                        arguments =
                                listOf(
                                        navArgument("contentId") { type = NavType.StringType },
                                        navArgument("contentType") { type = NavType.StringType }
                                )
                ) { backStackEntry ->
                        val contentId =
                                backStackEntry.arguments?.getString("contentId")?.let {
                                        URLDecoder.decode(it, "UTF-8")
                                }
                                        ?: ""
                        val contentType =
                                backStackEntry.arguments?.getString("contentType")?.let {
                                        URLDecoder.decode(it, "UTF-8")
                                }
                                        ?: ""

                        repository?.let { repo ->
                                when {
                                        contentType == "movie" -> {
                                                selectedMovie?.let { movie ->
                                                        val vodContent = movie.toVodContent(repo)
                                                        VodPlayerScreen(
                                                                content = vodContent,
                                                                onBack = {
                                                                        navController.popBackStack()
                                                                }
                                                        )
                                                }
                                        }
                                        contentType.startsWith("episode_") -> {
                                                val seriesId =
                                                        contentType
                                                                .removePrefix("episode_")
                                                                .toIntOrNull()
                                                selectedSeries?.let { series ->
                                                        if (series.seriesId == seriesId) {
                                                                // Find the episode in the series
                                                                // detail
                                                                val seriesDetailState = remember {
                                                                        mutableStateOf<
                                                                                com.example.iptv.data.model.SeriesDetailResponse?>(
                                                                                null
                                                                        )
                                                                }

                                                                LaunchedEffect(series.seriesId) {
                                                                        val detail =
                                                                                repo.getSeriesDetail(
                                                                                        series.seriesId
                                                                                )
                                                                        seriesDetailState.value =
                                                                                detail
                                                                }

                                                                seriesDetailState.value?.let {
                                                                        seriesDetail ->
                                                                        val allEpisodes =
                                                                                seriesDetail
                                                                                        .episodes
                                                                                        .values
                                                                                        .flatten()
                                                                        val currentEpisode =
                                                                                allEpisodes.find {
                                                                                        it.id ==
                                                                                                contentId
                                                                                }

                                                                        currentEpisode?.let {
                                                                                episode ->
                                                                                val vodContent =
                                                                                        episode.toVodContent(
                                                                                                series,
                                                                                                repo
                                                                                        )
                                                                                val playlist =
                                                                                        allEpisodes
                                                                                                .map {
                                                                                                        it.toVodContent(
                                                                                                                series,
                                                                                                                repo
                                                                                                        )
                                                                                                }

                                                                                VodPlayerScreen(
                                                                                        content =
                                                                                                vodContent,
                                                                                        playlist =
                                                                                                playlist,
                                                                                        onBack = {
                                                                                                navController
                                                                                                        .popBackStack()
                                                                                        },
                                                                                        onNextContent = {
                                                                                                nextContent
                                                                                                ->
                                                                                                navController
                                                                                                        .navigate(
                                                                                                                Screen.VodPlayer
                                                                                                                        .createRoute(
                                                                                                                                nextContent
                                                                                                                                        .id,
                                                                                                                                "episode_${series.seriesId}"
                                                                                                                        )
                                                                                                        )
                                                                                        },
                                                                                        onPreviousContent = {
                                                                                                prevContent
                                                                                                ->
                                                                                                navController
                                                                                                        .navigate(
                                                                                                                Screen.VodPlayer
                                                                                                                        .createRoute(
                                                                                                                                prevContent
                                                                                                                                        .id,
                                                                                                                                "episode_${series.seriesId}"
                                                                                                                        )
                                                                                                        )
                                                                                        }
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
}
