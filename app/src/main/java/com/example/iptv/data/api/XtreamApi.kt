package com.example.iptv.data.api

import com.example.iptv.data.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApi {

        @GET("player_api.php")
        suspend fun authenticate(
                @Query("username") username: String,
                @Query("password") password: String
        ): Response<AuthResponse>

        @GET("player_api.php")
        suspend fun getLiveCategories(
                @Query("username") username: String,
                @Query("password") password: String,
                @Query("action") action: String = "get_live_categories"
        ): Response<List<Category>>

        @GET("player_api.php")
        suspend fun getLiveStreams(
                @Query("username") username: String,
                @Query("password") password: String,
                @Query("action") action: String = "get_live_streams",
                @Query("category_id") categoryId: String
        ): Response<List<Channel>>

        @GET("xmltv.php")
        suspend fun getEpg(
                @Query("username") username: String,
                @Query("password") password: String,
                @Query("stream_id") streamId: Int
        ): Response<List<EpgProgram>>

        // VOD Movies
        @GET("player_api.php")
        suspend fun getMovieCategories(
                @Query("username") username: String,
                @Query("password") password: String,
                @Query("action") action: String = "get_vod_categories"
        ): Response<List<Category>>

        @GET("player_api.php")
        suspend fun getMovies(
                @Query("username") username: String,
                @Query("password") password: String,
                @Query("action") action: String = "get_vod_streams",
                @Query("category_id") categoryId: String
        ): Response<List<Movie>>

        @GET("player_api.php")
        suspend fun getMovieDetail(
                @Query("username") username: String,
                @Query("password") password: String,
                @Query("action") action: String = "get_vod_info",
                @Query("vod_id") vodId: Int
        ): Response<MovieDetailResponse>

        // VOD Series
        @GET("player_api.php")
        suspend fun getSeriesCategories(
                @Query("username") username: String,
                @Query("password") password: String,
                @Query("action") action: String = "get_series_categories"
        ): Response<List<Category>>

        @GET("player_api.php")
        suspend fun getSeries(
                @Query("username") username: String,
                @Query("password") password: String,
                @Query("action") action: String = "get_series",
                @Query("category_id") categoryId: String
        ): Response<List<Series>>

        @GET("player_api.php")
        suspend fun getSeriesDetail(
                @Query("username") username: String,
                @Query("password") password: String,
                @Query("action") action: String = "get_series_info",
                @Query("series_id") seriesId: Int
        ): Response<SeriesDetailResponse>
}
