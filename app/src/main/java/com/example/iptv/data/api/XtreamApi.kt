package com.example.iptv.data.api

import com.example.iptv.data.model.AuthResponse
import com.example.iptv.data.model.Category
import com.example.iptv.data.model.Channel
import com.example.iptv.data.model.EpgProgram
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
}
