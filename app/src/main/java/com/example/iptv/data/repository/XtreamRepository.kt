package com.example.iptv.data.repository

import com.example.iptv.data.api.XtreamApi
import com.example.iptv.data.model.AuthResponse
import com.example.iptv.data.model.Category
import com.example.iptv.data.model.Channel
import com.example.iptv.data.model.EpgProgram
import com.example.iptv.data.model.LoginCredentials
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class XtreamRepository {

    private var api: XtreamApi? = null
    private var credentials: LoginCredentials? = null

    // Global rate limiting
    private var lastRequestTime = 0L
    private val minRequestInterval = 500L // 500ms between any requests

    // Stream URL cache to prevent repeated URL generations
    private val streamUrlCache = mutableMapOf<Int, String>()

    // Request throttling
    private suspend fun throttleRequest() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime
        if (timeSinceLastRequest < minRequestInterval) {
            kotlinx.coroutines.delay(minRequestInterval - timeSinceLastRequest)
        }
        lastRequestTime = System.currentTimeMillis()
    }

    fun initialize(credentials: LoginCredentials) {
        this.credentials = credentials

        val loggingInterceptor =
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        val client =
                OkHttpClient.Builder()
                        .addInterceptor(loggingInterceptor)
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .build()

        val retrofit =
                Retrofit.Builder()
                        .baseUrl(credentials.getBaseUrl())
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

        api = retrofit.create(XtreamApi::class.java)
    }

    suspend fun authenticate(): Result<AuthResponse> =
            withContext(Dispatchers.IO) {
                try {
                    throttleRequest() // Add global rate limiting

                    val creds =
                            credentials
                                    ?: return@withContext Result.failure(
                                            Exception("No credentials provided")
                                    )

                    println("IPTV LOGIN: Attempting authentication...")
                    println("IPTV LOGIN: URL = ${creds.getBaseUrl()}")
                    println("IPTV LOGIN: Username = ${creds.username}")
                    println("IPTV LOGIN: Password = ${creds.password}")

                    val response = api?.authenticate(creds.username, creds.password)

                    println("IPTV LOGIN: Response Code = ${response?.code()}")
                    println("IPTV LOGIN: Response Message = ${response?.message()}")
                    println("IPTV LOGIN: Response Headers = ${response?.headers()}")

                    if (response?.isSuccessful == true) {
                        val authResponse = response.body()
                        println("IPTV LOGIN: Response Body = $authResponse")

                        if (authResponse?.userInfo?.auth == 1) {
                            println("IPTV LOGIN: Authentication successful!")
                            Result.success(authResponse)
                        } else {
                            val errorMsg =
                                    "Invalid credentials - Auth status: ${authResponse?.userInfo?.auth}, User info: ${authResponse?.userInfo}"
                            println("IPTV LOGIN: $errorMsg")
                            Result.failure(Exception(errorMsg))
                        }
                    } else {
                        val errorBody = response?.errorBody()?.string()
                        val errorMsg =
                                "Authentication failed - Code: ${response?.code()}, Message: ${response?.message()}, Body: $errorBody"
                        println("IPTV LOGIN: $errorMsg")
                        Result.failure(Exception(errorMsg))
                    }
                } catch (e: Exception) {
                    val errorMsg =
                            "Authentication exception: ${e.javaClass.simpleName} - ${e.message}"
                    println("IPTV LOGIN: $errorMsg")
                    println("IPTV LOGIN: Stack trace: ${e.stackTrace.joinToString("\n")}")
                    Result.failure(Exception(errorMsg, e))
                }
            }

    suspend fun getCategories(): Result<List<Category>> =
            withContext(Dispatchers.IO) {
                try {
                    throttleRequest() // Add global rate limiting

                    val creds =
                            credentials
                                    ?: return@withContext Result.failure(
                                            Exception("No credentials")
                                    )
                    val response = api?.getLiveCategories(creds.username, creds.password)

                    if (response?.isSuccessful == true) {
                        Result.success(response.body() ?: emptyList())
                    } else {
                        Result.failure(Exception("Failed to load categories"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    suspend fun getChannels(categoryId: String): Result<List<Channel>> =
            withContext(Dispatchers.IO) {
                try {
                    throttleRequest() // Add global rate limiting

                    val creds =
                            credentials
                                    ?: return@withContext Result.failure(
                                            Exception("No credentials")
                                    )
                    val response =
                            api?.getLiveStreams(
                                    creds.username,
                                    creds.password,
                                    categoryId = categoryId
                            )

                    if (response?.isSuccessful == true) {
                        Result.success(response.body() ?: emptyList())
                    } else {
                        Result.failure(Exception("Failed to load channels"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    fun getStreamUrl(streamId: Int): String? {
        // Check cache first
        streamUrlCache[streamId]?.let {
            return it
        }

        val creds = credentials ?: return null
        val streamUrl =
                "${creds.getBaseUrl()}/live/${creds.username}/${creds.password}/$streamId.m3u8"

        // Cache the URL for future use
        streamUrlCache[streamId] = streamUrl

        // Limit cache size to prevent memory issues
        if (streamUrlCache.size > 1000) {
            streamUrlCache.clear()
        }

        return streamUrl
    }

    // Helper method to clear caches if needed
    fun clearCaches() {
        streamUrlCache.clear()
    }

    suspend fun getEpg(streamId: Int): Result<List<EpgProgram>> =
            withContext(Dispatchers.IO) {
                try {
                    throttleRequest() // Add global rate limiting

                    val creds =
                            credentials
                                    ?: return@withContext Result.failure(
                                            Exception("No credentials")
                                    )
                    val response = api?.getEpg(creds.username, creds.password, streamId)

                    if (response?.isSuccessful == true) {
                        Result.success(response.body() ?: emptyList())
                    } else {
                        Result.failure(Exception("Failed to load EPG"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
}
