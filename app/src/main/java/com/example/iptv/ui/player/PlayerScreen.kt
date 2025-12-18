package com.example.iptv.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PlayerScreen(
        streamUrl: String,
        channelName: String,
        currentChannel: com.example.iptv.data.model.Channel,
        channelList: List<com.example.iptv.data.model.Channel>,
        currentChannelIndex: Int,
        repository: com.example.iptv.data.repository.XtreamRepository,
        favoritesRepository: com.example.iptv.data.repository.FavoritesRepository,
        onChannelChange: (com.example.iptv.data.model.Channel) -> Unit,
        onBack: () -> Unit,
        viewModel: PlayerViewModel = viewModel(key = currentChannel.streamId.toString())
) {
        val context = LocalContext.current
        val uiState by viewModel.uiState.collectAsState()
        val favorites by favoritesRepository.favorites.collectAsState()
        val isFavorite = favorites.contains(currentChannel.streamId)

        // Initialize player only once to prevent crashes
        var initialized by remember { mutableStateOf(false) }

        // Controls visibility
        var showControls by remember { mutableStateOf(true) }

        // Focus requester for TV remote support
        val screenFocusRequester = remember { FocusRequester() }

        // Interaction source for transparent click layer
        val interactionSource = remember { MutableInteractionSource() }

        // Auto-hide controls after 5 seconds
        LaunchedEffect(showControls) {
                if (showControls) {
                        kotlinx.coroutines.delay(5000)
                        showControls = false
                }
        }

        // Request focus when controls are hidden (for remote control)
        LaunchedEffect(showControls) {
                if (!showControls) {
                        screenFocusRequester.requestFocus()
                }
        }

        // Initialize player
        if (!initialized) {
                viewModel.initializePlayer(context, streamUrl, channelName)
                initialized = true
        }

        DisposableEffect(Unit) { onDispose { viewModel.releasePlayer() } }

        // Handle Back Button
        BackHandler { onBack() }

        // Channel navigation functions
        val onPreviousChannel = {
                if (currentChannelIndex > 0) {
                        onChannelChange(channelList[currentChannelIndex - 1])
                }
        }

        val onNextChannel = {
                if (currentChannelIndex < channelList.size - 1) {
                        onChannelChange(channelList[currentChannelIndex + 1])
                }
        }

        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(Color.Black)
                                // TV Remote key handling
                                .focusRequester(screenFocusRequester)
                                .focusable()
                                .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.key) {
                                                        Key.DirectionUp -> {
                                                                onNextChannel()
                                                                true
                                                        }
                                                        Key.DirectionDown -> {
                                                                onPreviousChannel()
                                                                true
                                                        }
                                                        Key.Enter, Key.DirectionCenter -> {
                                                                showControls = !showControls
                                                                true
                                                        }
                                                        else -> false
                                                }
                                        } else false
                                }
        ) {
                uiState.errorMessage?.let { errorMessage ->
                        // Error state
                        Card(modifier = Modifier.align(Alignment.Center).padding(32.dp)) {
                                Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                        Text(
                                                text = "Playback Error",
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = MaterialTheme.colorScheme.error
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                                text = errorMessage,
                                                style = MaterialTheme.typography.bodyMedium
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                                onClick = {
                                                        viewModel.initializePlayer(
                                                                context,
                                                                streamUrl,
                                                                channelName
                                                        )
                                                }
                                        ) { Text("Retry") }
                                }
                        }
                }
                        ?: run {
                                // Video player using TextureView
                                AndroidView(
                                        factory = { ctx ->
                                                android.view.TextureView(ctx).apply {
                                                        layoutParams =
                                                                ViewGroup.LayoutParams(
                                                                        ViewGroup.LayoutParams
                                                                                .MATCH_PARENT,
                                                                        ViewGroup.LayoutParams
                                                                                .MATCH_PARENT
                                                                )
                                                        keepScreenOn = true
                                                }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        update = { textureView ->
                                                try {
                                                        val player = viewModel.getPlayer()
                                                        if (player != null) {
                                                                player.setVideoTextureView(
                                                                        textureView
                                                                )
                                                        }
                                                } catch (e: Exception) {
                                                        println(
                                                                "Error setting TextureView: ${e.message}"
                                                        )
                                                }
                                        }
                                )

                                // Transparent click layer - toggles controls on tap
                                Box(
                                        modifier =
                                                Modifier.fillMaxSize().clickable(
                                                                interactionSource =
                                                                        interactionSource,
                                                                indication = null
                                                        ) { showControls = !showControls }
                                )

                                // Video Player Controls Overlay
                                if (showControls) {
                                        VideoPlayerControls(
                                                channelName = currentChannel.name,
                                                isFavorite = isFavorite,
                                                currentChannelIndex = currentChannelIndex,
                                                totalChannels = channelList.size,
                                                onFavoriteClick = {
                                                        favoritesRepository.toggleFavorite(
                                                                currentChannel.streamId
                                                        )
                                                },
                                                onPreviousChannel = onPreviousChannel,
                                                onNextChannel = onNextChannel,
                                                onBack = onBack
                                        )
                                }
                        }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerControls(
        channelName: String,
        isFavorite: Boolean,
        currentChannelIndex: Int,
        totalChannels: Int,
        onFavoriteClick: () -> Unit,
        onPreviousChannel: () -> Unit,
        onNextChannel: () -> Unit,
        onBack: () -> Unit
) {
        Box(modifier = Modifier.fillMaxSize()) {
                // Top Controls Bar
                Card(
                        modifier =
                                Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(16.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = Color.Black.copy(alpha = 0.7f)
                                ),
                        shape = RoundedCornerShape(12.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Back button
                                IconButton(
                                        onClick = onBack,
                                        colors =
                                                IconButtonDefaults.iconButtonColors(
                                                        contentColor = Color.White
                                                )
                                ) {
                                        Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back"
                                        )
                                }

                                // Channel info
                                Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                ) {
                                        Text(
                                                text = channelName,
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                                text =
                                                        "${currentChannelIndex + 1} of $totalChannels",
                                                color = Color.White.copy(alpha = 0.8f),
                                                style = MaterialTheme.typography.bodySmall
                                        )
                                }

                                // Favorite button
                                IconButton(
                                        onClick = onFavoriteClick,
                                        colors =
                                                IconButtonDefaults.iconButtonColors(
                                                        contentColor =
                                                                if (isFavorite) Color.Red
                                                                else Color.White
                                                )
                                ) {
                                        Icon(
                                                if (isFavorite) Icons.Default.Favorite
                                                else Icons.Default.FavoriteBorder,
                                                contentDescription =
                                                        if (isFavorite) "Remove from Favorites"
                                                        else "Add to Favorites"
                                        )
                                }
                        }
                }

                // Bottom Controls - Channel Navigation
                Card(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = Color.Black.copy(alpha = 0.7f)
                                ),
                        shape = RoundedCornerShape(24.dp)
                ) {
                        Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Previous Channel
                                IconButton(
                                        onClick = onPreviousChannel,
                                        enabled = currentChannelIndex > 0,
                                        colors =
                                                IconButtonDefaults.iconButtonColors(
                                                        contentColor = Color.White,
                                                        disabledContentColor = Color.Gray
                                                ),
                                        modifier =
                                                Modifier.size(48.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                                if (currentChannelIndex > 0)
                                                                        MaterialTheme.colorScheme
                                                                                .primary.copy(
                                                                                alpha = 0.2f
                                                                        )
                                                                else Color.Transparent
                                                        )
                                ) {
                                        Icon(
                                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                                contentDescription = "Previous Channel",
                                                modifier = Modifier.size(32.dp)
                                        )
                                }

                                // Channel counter
                                Text(
                                        text = "CH",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                )

                                // Next Channel
                                IconButton(
                                        onClick = onNextChannel,
                                        enabled = currentChannelIndex < totalChannels - 1,
                                        colors =
                                                IconButtonDefaults.iconButtonColors(
                                                        contentColor = Color.White,
                                                        disabledContentColor = Color.Gray
                                                ),
                                        modifier =
                                                Modifier.size(48.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                                if (currentChannelIndex <
                                                                                totalChannels - 1
                                                                )
                                                                        MaterialTheme.colorScheme
                                                                                .primary.copy(
                                                                                alpha = 0.2f
                                                                        )
                                                                else Color.Transparent
                                                        )
                                ) {
                                        Icon(
                                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = "Next Channel",
                                                modifier = Modifier.size(32.dp)
                                        )
                                }
                        }
                }
        }
}
