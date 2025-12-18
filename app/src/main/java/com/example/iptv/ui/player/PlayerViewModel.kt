package com.example.iptv.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
        val isPlaying: Boolean = false,
        val channelName: String = "",
        val streamUrl: String = "",
        val errorMessage: String? = null
)

class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null

    fun initializePlayer(context: Context, streamUrl: String, channelName: String) {
        // 1. Reset state to avoid flickering old data
        _uiState.value =
                _uiState.value.copy(
                        channelName = channelName,
                        streamUrl = streamUrl,
                        errorMessage = null,
                        isPlaying = false
                )

        try {
            // 2. Stop previous audio immediately
            exoPlayer?.stop()
            exoPlayer?.release()

            // 3. Initialize new player
            exoPlayer =
                    ExoPlayer.Builder(context)
                            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                            .build()
                            .apply {
                                val mediaItem =
                                        MediaItem.Builder()
                                                .setUri(streamUrl)
                                                .setLiveConfiguration(
                                                        MediaItem.LiveConfiguration.Builder()
                                                                .setMaxPlaybackSpeed(1.02f)
                                                                .build()
                                                )
                                                .build()

                                setMediaItem(mediaItem)
                                prepare()
                                playWhenReady = true
                            }

            _uiState.value = _uiState.value.copy(isPlaying = true)
        } catch (e: Exception) {
            _uiState.value =
                    _uiState.value.copy(
                            errorMessage = "Playback failed: ${e.message}",
                            isPlaying = false
                    )
        }
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun pausePlayer() {
        exoPlayer?.pause()
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    fun resumePlayer() {
        exoPlayer?.play()
        _uiState.value = _uiState.value.copy(isPlaying = true)
    }

    fun releasePlayer() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
