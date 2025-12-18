package com.example.iptv.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VodPlayerUiState(
        val isPlaying: Boolean = false,
        val contentTitle: String = "",
        val streamUrl: String = "",
        val currentPosition: Long = 0L,
        val duration: Long = 0L,
        val bufferedPosition: Long = 0L,
        val isBuffering: Boolean = false,
        val errorMessage: String? = null
)

class VodPlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VodPlayerUiState())
    val uiState: StateFlow<VodPlayerUiState> = _uiState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var isPositionTracking = false

    fun initializePlayer(context: Context, streamUrl: String, title: String) {
        println("🎬 VOD Player: Initializing for: $title | URL: $streamUrl")

        // Reset state
        _uiState.value =
                _uiState.value.copy(
                        contentTitle = title,
                        streamUrl = streamUrl,
                        errorMessage = null,
                        isPlaying = false,
                        currentPosition = 0L,
                        duration = 0L
                )

        try {
            // Release previous player
            exoPlayer?.stop()
            exoPlayer?.release()

            // Create new ExoPlayer with optimized settings
            exoPlayer =
                    ExoPlayer.Builder(context)
                            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                            .setSeekBackIncrementMs(10000L) // 10 second rewind
                            .setSeekForwardIncrementMs(10000L) // 10 second fast forward
                            .build()
                            .apply {
                                val mediaItem = MediaItem.Builder().setUri(streamUrl).build()

                                setMediaItem(mediaItem)
                                prepare()

                                // Add listener for player events
                                addListener(
                                        object : Player.Listener {
                                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                                _uiState.value =
                                                        _uiState.value.copy(
                                                                isPlaying = isPlaying,
                                                                isBuffering =
                                                                        playbackState ==
                                                                                Player.STATE_BUFFERING
                                                        )
                                            }

                                            override fun onPlaybackStateChanged(
                                                    playbackState: Int
                                            ) {
                                                val isBuffering =
                                                        playbackState == Player.STATE_BUFFERING
                                                _uiState.value =
                                                        _uiState.value.copy(
                                                                isBuffering = isBuffering
                                                        )

                                                if (playbackState == Player.STATE_READY &&
                                                                !isPositionTracking
                                                ) {
                                                    startPositionTracking()
                                                }
                                            }

                                            override fun onPlayerError(
                                                    error: androidx.media3.common.PlaybackException
                                            ) {
                                                _uiState.value =
                                                        _uiState.value.copy(
                                                                errorMessage =
                                                                        "Playback error: ${error.message}",
                                                                isPlaying = false
                                                        )
                                            }
                                        }
                                )
                            }

            // Attach to PlayerView
            playerView?.player = exoPlayer
        } catch (e: Exception) {
            _uiState.value =
                    _uiState.value.copy(
                            errorMessage = "Failed to initialize player: ${e.message}",
                            isPlaying = false
                    )
        }
    }

    fun setPlayerView(playerView: PlayerView) {
        this.playerView = playerView
        exoPlayer?.let { player -> playerView.player = player }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun rewind() {
        exoPlayer?.let { player ->
            val newPosition = maxOf(0L, player.currentPosition - 10000L)
            player.seekTo(newPosition)
        }
    }

    fun fastForward() {
        exoPlayer?.let { player ->
            val duration = player.duration
            if (duration != C.TIME_UNSET) {
                val newPosition = minOf(duration, player.currentPosition + 10000L)
                player.seekTo(newPosition)
            }
        }
    }

    fun seekToPercentage(percentage: Float) {
        exoPlayer?.let { player ->
            val duration = player.duration
            if (duration != C.TIME_UNSET) {
                val position = (duration * percentage).toLong()
                player.seekTo(position)
            }
        }
    }

    private fun startPositionTracking() {
        if (isPositionTracking) return

        isPositionTracking = true
        viewModelScope.launch {
            while (isPositionTracking && exoPlayer != null) {
                exoPlayer?.let { player ->
                    val currentPosition = player.currentPosition
                    val duration = if (player.duration == C.TIME_UNSET) 0L else player.duration
                    val bufferedPosition = player.bufferedPosition

                    _uiState.value =
                            _uiState.value.copy(
                                    currentPosition = currentPosition,
                                    duration = duration,
                                    bufferedPosition = bufferedPosition
                            )
                }
                delay(1000) // Update every second
            }
        }
    }

    fun releasePlayer() {
        isPositionTracking = false
        exoPlayer?.let { player ->
            player.stop()
            player.release()
        }
        exoPlayer = null
        playerView?.player = null

        _uiState.value = VodPlayerUiState()
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }

    // Save playback position for resuming later
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }

    // Resume from saved position
    fun resumeFromPosition(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    // Get playback speed options
    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
    }

    // Volume control
    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }
}
