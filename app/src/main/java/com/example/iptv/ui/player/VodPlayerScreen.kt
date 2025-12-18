package com.example.iptv.ui.player

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

data class VodContent(
        val id: String,
        val title: String,
        val streamUrl: String,
        val poster: String?,
        val duration: Long? = null,
        val currentTime: Long = 0L,
        val type: ContentType
)

enum class ContentType {
    MOVIE,
    EPISODE
}

@Composable
fun VodPlayerScreen(
        content: VodContent,
        playlist: List<VodContent> = emptyList(),
        onBack: () -> Unit,
        onNextContent: ((VodContent) -> Unit)? = null,
        onPreviousContent: ((VodContent) -> Unit)? = null,
        viewModel: VodPlayerViewModel = viewModel(key = content.id)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(content) {
        viewModel.initializePlayer(context, content.streamUrl, content.title)
    }

    DisposableEffect(Unit) { onDispose { viewModel.releasePlayer() } }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Video Player
        AndroidView(
                factory = { ctx ->
                    val playerView =
                            androidx.media3.ui.PlayerView(ctx).apply {
                                useController = false
                                layoutParams =
                                        ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                            }
                    viewModel.setPlayerView(playerView)
                    playerView
                },
                modifier = Modifier.fillMaxSize().clickable { showControls = !showControls }
        )

        // Custom Controls Overlay
        if (showControls) {
            VodPlayerControls(
                    uiState = uiState,
                    content = content,
                    playlist = playlist,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onSeek = { position -> viewModel.seekTo(position) },
                    onBack = onBack,
                    onNext = onNextContent,
                    onPrevious = onPreviousContent,
                    onRewind = { viewModel.rewind() },
                    onFastForward = { viewModel.fastForward() },
                    onHideControls = { showControls = false }
            )
        }

        // Show controls on touch
        LaunchedEffect(showControls) {
            if (showControls) {
                kotlinx.coroutines.delay(5000) // Hide after 5 seconds
                showControls = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VodPlayerControls(
        uiState: VodPlayerUiState,
        content: VodContent,
        playlist: List<VodContent>,
        onPlayPause: () -> Unit,
        onSeek: (Long) -> Unit,
        onBack: () -> Unit,
        onNext: ((VodContent) -> Unit)?,
        onPrevious: ((VodContent) -> Unit)?,
        onRewind: () -> Unit,
        onFastForward: () -> Unit,
        onHideControls: () -> Unit
) {
    // Top Bar
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(
                                    brush =
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    Color.Black.copy(alpha = 0.8f),
                                                                    Color.Transparent
                                                            )
                                            )
                            )
                            .padding(16.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                )
            }

            Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            ) {
                Text(
                        text = content.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                if (content.type == ContentType.EPISODE) {
                    Text(
                            text =
                                    formatDuration(uiState.currentPosition) +
                                            " / " +
                                            formatDuration(uiState.duration),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    // Bottom Controls
    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Bottom
    ) {
        // Progress Bar
        if (uiState.duration > 0) {
            Column {
                Slider(
                        value = uiState.currentPosition.toFloat(),
                        onValueChange = { newValue -> onSeek(newValue.toLong()) },
                        valueRange = 0f..uiState.duration.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                )

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                            text = formatDuration(uiState.currentPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                            text = formatDuration(uiState.duration),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Controls
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Content
            if (onPrevious != null) {
                val previousContent = getPreviousContent(content, playlist)
                IconButton(
                        onClick = { previousContent?.let { onPrevious(it) } },
                        enabled = previousContent != null,
                        modifier =
                                Modifier.size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous",
                            tint = if (previousContent != null) Color.White else Color.Gray,
                            modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Rewind
            IconButton(
                    onClick = onRewind,
                    modifier =
                            Modifier.size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Play/Pause
            IconButton(
                    onClick = onPlayPause,
                    modifier =
                            Modifier.size(72.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Fast Forward
            IconButton(
                    onClick = onFastForward,
                    modifier =
                            Modifier.size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Fast Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Next Content
            if (onNext != null) {
                val nextContent = getNextContent(content, playlist)
                IconButton(
                        onClick = { nextContent?.let { onNext(it) } },
                        enabled = nextContent != null,
                        modifier =
                                Modifier.size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next",
                            tint = if (nextContent != null) Color.White else Color.Gray,
                            modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Additional Controls Row
        Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center
        ) {
            // Subtitle Button
            IconButton(
                    onClick = { /* TODO: Implement subtitle selection */},
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) { Icon(Icons.Default.Info, contentDescription = "Subtitles", tint = Color.White) }

            Spacer(modifier = Modifier.width(16.dp))

            // Audio Track Button
            IconButton(
                    onClick = { /* TODO: Implement audio track selection */},
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Audio Track", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Settings Button
            IconButton(
                    onClick = { /* TODO: Implement player settings */},
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White) }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun getPreviousContent(current: VodContent, playlist: List<VodContent>): VodContent? {
    val currentIndex = playlist.indexOfFirst { it.id == current.id }
    return if (currentIndex > 0) playlist[currentIndex - 1] else null
}

private fun getNextContent(current: VodContent, playlist: List<VodContent>): VodContent? {
    val currentIndex = playlist.indexOfFirst { it.id == current.id }
    return if (currentIndex >= 0 && currentIndex < playlist.size - 1) {
        playlist[currentIndex + 1]
    } else null
}
