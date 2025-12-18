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

// VOD Models for Movies and Series
data class Movie(
        @SerializedName("num") val num: Int,
        @SerializedName("name") val name: String,
        @SerializedName("stream_type") val streamType: String,
        @SerializedName("stream_id") val streamId: Int,
        @SerializedName("stream_icon") val streamIcon: String?,
        @SerializedName("rating") val rating: String?,
        @SerializedName("rating_5based") val rating5Based: Double?,
        @SerializedName("added") val added: String?,
        @SerializedName("category_id") val categoryId: String,
        @SerializedName("container_extension") val containerExtension: String?,
        @SerializedName("custom_sid") val customSid: String?,
        @SerializedName("direct_source") val directSource: String?
)

data class MovieInfo(
        @SerializedName("tmdb_id") val tmdbId: String?,
        @SerializedName("name") val name: String,
        @SerializedName("o_name") val originalName: String?,
        @SerializedName("cover_big") val coverBig: String?,
        @SerializedName("movie_image") val movieImage: String?,
        @SerializedName("releasedate") val releaseDate: String?,
        @SerializedName("episode_run_time") val episodeRunTime: String?,
        @SerializedName("youtube_trailer") val youtubeTrailer: String?,
        @SerializedName("director") val director: String?,
        @SerializedName("actors") val actors: String?,
        @SerializedName("cast") val cast: String?,
        @SerializedName("description") val description: String?,
        @SerializedName("plot") val plot: String?,
        @SerializedName("age") val age: String?,
        @SerializedName("mpaa_rating") val mpaaRating: String?,
        @SerializedName("rating_5based") val rating5Based: Double?,
        @SerializedName("country") val country: String?,
        @SerializedName("genre") val genre: String?,
        @SerializedName("backdrop_path") val backdropPath: List<String>?,
        @SerializedName("duration_secs") val durationSecs: Int?,
        @SerializedName("duration") val duration: String?,
        @SerializedName("video") val video: Boolean?,
        @SerializedName("audio") val audio: Map<String, String>?
)

data class Series(
        @SerializedName("num") val num: Int,
        @SerializedName("name") val name: String,
        @SerializedName("series_id") val seriesId: Int,
        @SerializedName("cover") val cover: String?,
        @SerializedName("plot") val plot: String?,
        @SerializedName("cast") val cast: String?,
        @SerializedName("director") val director: String?,
        @SerializedName("genre") val genre: String?,
        @SerializedName("releaseDate") val releaseDate: String?,
        @SerializedName("last_modified") val lastModified: String?,
        @SerializedName("rating") val rating: String?,
        @SerializedName("rating_5based") val rating5Based: Double?,
        @SerializedName("backdrop_path") val backdropPath: List<String>?,
        @SerializedName("youtube_trailer") val youtubeTrailer: String?,
        @SerializedName("episode_run_time") val episodeRunTime: String?,
        @SerializedName("category_id") val categoryId: String
)

data class SeriesInfo(
        @SerializedName("name") val name: String,
        @SerializedName("cover") val cover: String?,
        @SerializedName("plot") val plot: String?,
        @SerializedName("cast") val cast: String?,
        @SerializedName("director") val director: String?,
        @SerializedName("genre") val genre: String?,
        @SerializedName("releaseDate") val releaseDate: String?,
        @SerializedName("last_modified") val lastModified: String?,
        @SerializedName("rating") val rating: String?,
        @SerializedName("rating_5based") val rating5Based: Double?,
        @SerializedName("backdrop_path") val backdropPath: List<String>?,
        @SerializedName("youtube_trailer") val youtubeTrailer: String?,
        @SerializedName("episode_run_time") val episodeRunTime: String?,
        @SerializedName("seasons") val seasons: List<Season>?
)

data class Season(
        @SerializedName("air_date") val airDate: String?,
        @SerializedName("episode_count") val episodeCount: Int,
        @SerializedName("id") val id: Int,
        @SerializedName("name") val name: String,
        @SerializedName("overview") val overview: String?,
        @SerializedName("season_number") val seasonNumber: Int,
        @SerializedName("cover") val cover: String?,
        @SerializedName("cover_big") val coverBig: String?
)

data class Episode(
        @SerializedName("id") val id: String,
        @SerializedName("episode_num") val episodeNum: Int,
        @SerializedName("title") val title: String,
        @SerializedName("container_extension") val containerExtension: String?,
        @SerializedName("info") val info: EpisodeInfo?,
        @SerializedName("custom_sid") val customSid: String?,
        @SerializedName("added") val added: String?,
        @SerializedName("season") val season: Int,
        @SerializedName("direct_source") val directSource: String?
)

data class EpisodeInfo(
        @SerializedName("tmdb_id") val tmdbId: Int?,
        @SerializedName("releasedate") val releaseDate: String?,
        @SerializedName("plot") val plot: String?,
        @SerializedName("duration_secs") val durationSecs: Int?,
        @SerializedName("duration") val duration: String?,
        @SerializedName("movie_image") val movieImage: String?,
        @SerializedName("rating") val rating: String?
)

data class MovieDetailResponse(
        @SerializedName("info") val info: MovieInfo,
        @SerializedName("movie_data") val movieData: Movie
)

data class SeriesDetailResponse(
        @SerializedName("info") val info: SeriesInfo,
        @SerializedName("episodes") val episodes: Map<String, List<Episode>>
)
