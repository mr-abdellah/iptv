package com.example.iptv.data.model

import com.google.gson.annotations.SerializedName

data class UserInfo(
        @SerializedName("username") val username: String,
        @SerializedName("password") val password: String,
        @SerializedName("message") val message: String?,
        @SerializedName("auth") val auth: Int,
        @SerializedName("status") val status: String?,
        @SerializedName("exp_date") val expDate: String?,
        @SerializedName("is_trial") val isTrial: String?,
        @SerializedName("active_cons") val activeCons: String?,
        @SerializedName("created_at") val createdAt: String?,
        @SerializedName("max_connections") val maxConnections: String?,
        @SerializedName("allowed_output_formats") val allowedOutputFormats: List<String>?
)

data class ServerInfo(
        @SerializedName("url") val url: String?,
        @SerializedName("port") val port: String?,
        @SerializedName("https_port") val httpsPort: String?,
        @SerializedName("server_protocol") val serverProtocol: String?,
        @SerializedName("rtmp_port") val rtmpPort: String?,
        @SerializedName("timezone") val timezone: String?,
        @SerializedName("timestamp_now") val timestampNow: Long?
)

data class AuthResponse(
        @SerializedName("user_info") val userInfo: UserInfo,
        @SerializedName("server_info") val serverInfo: ServerInfo
)

data class Category(
        @SerializedName("category_id") val categoryId: String,
        @SerializedName("category_name") val categoryName: String,
        @SerializedName("parent_id") val parentId: Int?,
        @SerializedName("category_logo") val categoryLogo: String? = null
)

data class Channel(
        @SerializedName("num") val num: Int,
        @SerializedName("name") val name: String,
        @SerializedName("stream_type") val streamType: String,
        @SerializedName("stream_id") val streamId: Int,
        @SerializedName("stream_icon") val streamIcon: String?,
        @SerializedName("epg_channel_id") val epgChannelId: String?,
        @SerializedName("added") val added: String?,
        @SerializedName("category_id") val categoryId: String,
        @SerializedName("custom_sid") val customSid: String?,
        @SerializedName("tv_archive") val tvArchive: Int?,
        @SerializedName("direct_source") val directSource: String?,
        @SerializedName("tv_archive_duration") val tvArchiveDuration: String?
)

data class EpgProgram(
        @SerializedName("id") val id: String,
        @SerializedName("epg_id") val epgId: String,
        @SerializedName("title") val title: String,
        @SerializedName("lang") val language: String?,
        @SerializedName("start") val startTime: String,
        @SerializedName("end") val endTime: String,
        @SerializedName("description") val description: String?,
        @SerializedName("channel_id") val channelId: String,
        @SerializedName("start_timestamp") val startTimestamp: Long,
        @SerializedName("stop_timestamp") val stopTimestamp: Long
)
