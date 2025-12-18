package com.example.iptv.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.iptv.data.model.Category
import com.example.iptv.data.model.Channel
import com.example.iptv.data.repository.FavoritesRepository
import com.example.iptv.data.repository.XtreamRepository
import com.example.iptv.ui.categories.CategoriesViewModel
import com.example.iptv.ui.channels.ChannelsViewModel
import com.example.iptv.ui.player.PlayerScreen

@Composable
fun MainScreen(
        repository: XtreamRepository,
        categoriesViewModel: CategoriesViewModel = viewModel { CategoriesViewModel(repository) },
        channelsViewModel: ChannelsViewModel = viewModel { ChannelsViewModel(repository) }
) {
    val context = LocalContext.current
    val favoritesRepository = remember { FavoritesRepository(context) }
    val categoriesUiState by categoriesViewModel.uiState.collectAsState()
    val channelsUiState by channelsViewModel.uiState.collectAsState()

    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var showPlayer by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Initialize favorites repository in channels view model
    LaunchedEffect(Unit) { channelsViewModel.setFavoritesRepository(favoritesRepository) }

    // Create special categories and combine with regular categories
    val favoritesCategory =
            Category(categoryId = "-1", categoryName = "⭐ Favorites", parentId = null)
    val searchCategory = Category(categoryId = "-2", categoryName = "🔍 Search", parentId = null)
    val allCategories = listOf(favoritesCategory, searchCategory) + categoriesUiState.categories

    // Auto-select first regular category only on initial load
    var hasInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(categoriesUiState.categories.size) {
        if (!hasInitialized && categoriesUiState.categories.isNotEmpty()) {
            hasInitialized = true
            // Select first regular category instead of favorites
            selectedCategory = categoriesUiState.categories.first()
            // Use a small delay to prevent race conditions
            kotlinx.coroutines.delay(100)
            channelsViewModel.loadChannels(
                    categoriesUiState.categories.first().categoryId,
                    categoriesUiState.categories.first().categoryName
            )
        }
    }

    // Auto-select first channel when channels are loaded (prevent excessive triggers)
    var channelInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(channelsUiState.channels.size) {
        if (!channelInitialized &&
                        channelsUiState.channels.isNotEmpty() &&
                        selectedChannel == null &&
                        !showPlayer
        ) {
            channelInitialized = true
            selectedChannel = channelsUiState.channels.first()
        }
    }

    // Reset channel initialization when category changes
    LaunchedEffect(selectedCategory?.categoryId) {
        channelInitialized = false
        selectedChannel = null
    }

    if (showPlayer && selectedChannel != null) {
        // IMPORTANT: Recalculate streamUrl every time selectedChannel changes
        val streamUrl = repository.getStreamUrl(selectedChannel!!.streamId)
        val currentChannelIndex =
                channelsUiState.channels.indexOfFirst { it.streamId == selectedChannel!!.streamId }

        if (streamUrl != null) {
            // Use key() to force PlayerScreen to recompose when channel changes
            key(selectedChannel!!.streamId) {
                PlayerScreen(
                        streamUrl = streamUrl,
                        channelName = selectedChannel!!.name,
                        currentChannel = selectedChannel!!,
                        channelList = channelsUiState.channels,
                        currentChannelIndex = currentChannelIndex,
                        repository = repository,
                        favoritesRepository = favoritesRepository,
                        onChannelChange = { newChannel -> selectedChannel = newChannel },
                        onBack = { showPlayer = false }
                )
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Categories List (Left Side)
            CategoriesSidebar(
                    categories = allCategories,
                    selectedCategory = selectedCategory,
                    isLoading = categoriesUiState.isLoading,
                    onCategorySelected = { category ->
                        selectedCategory = category
                        selectedChannel = null // Reset selected channel
                        showSearch = false
                        when (category.categoryId) {
                            "-1" -> {
                                // Load favorites
                                channelsViewModel.loadFavoriteChannels()
                            }
                            "-2" -> {
                                // Show search
                                showSearch = true
                            }
                            else -> {
                                channelsViewModel.loadChannels(
                                        category.categoryId,
                                        category.categoryName
                                )
                            }
                        }
                    },
                    modifier = Modifier.width(320.dp)
            )

            // Channels List (Right Side) with Search
            if (showSearch) {
                SearchChannels(
                        searchQuery = searchQuery,
                        onQueryChange = { query ->
                            searchQuery = query
                            if (query.length >= 2) {
                                channelsViewModel.searchChannels(query)
                            }
                        },
                        channels = channelsUiState.channels,
                        isLoading = channelsUiState.isLoading,
                        onChannelSelected = { channel ->
                            selectedChannel = channel
                            showPlayer = true
                        },
                        modifier = Modifier.fillMaxWidth()
                )
            } else {
                ChannelsList(
                        channels = channelsUiState.channels,
                        categoryName = channelsUiState.categoryName,
                        isLoading = channelsUiState.isLoading,
                        onChannelSelected = { channel ->
                            selectedChannel = channel
                            showPlayer = true
                        },
                        modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CategoriesSidebar(
        categories: List<Category>,
        selectedCategory: Category?,
        isLoading: Boolean,
        onCategorySelected: (Category) -> Unit,
        modifier: Modifier = Modifier
) {
    Surface(
            modifier = modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                    shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                        text = "📺 Categories",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp)
                )
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                categories.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                                "📭 No categories found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { category ->
                            CategoryItem(
                                    category = category,
                                    isSelected = category == selectedCategory,
                                    onClick = { onCategorySelected(category) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(category: Category, isSelected: Boolean, onClick: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                    )
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                    model = category.categoryLogo ?: "",
                    contentDescription = category.categoryName,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                    text = category.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color =
                            if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ChannelsList(
        channels: List<Channel>,
        categoryName: String,
        isLoading: Boolean,
        onChannelSelected: (Channel) -> Unit,
        modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(16.dp)) {
            Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                    shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                        text = "📡 ${categoryName.ifEmpty { "Channels" }}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(16.dp)
                )
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                    "Loading channels...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                channels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.surfaceVariant
                                        )
                        ) {
                            Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val (icon, message) =
                                        when {
                                            categoryName.contains("Favorites") ->
                                                    "💝" to
                                                            "No favorite channels yet\nAdd channels to favorites while watching!"
                                            categoryName.contains("Search") ->
                                                    "🔍" to "Start typing to search channels"
                                            else -> "📭" to "No channels available in this category"
                                        }

                                Text(
                                        text = icon,
                                        style = MaterialTheme.typography.displayMedium,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(channels) { channel ->
                            ChannelItem(channel = channel, onClick = { onChannelSelected(channel) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelItem(channel: Channel, onClick: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
    ) {
        Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Channel logo
            Card(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
            ) {
                AsyncImage(
                        model = channel.streamIcon ?: "",
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                )
            }

            // Channel info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = "📺 Channel ${channel.num}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Play icon
            Card(
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                    shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchChannels(
        searchQuery: String,
        onQueryChange: (String) -> Unit,
        channels: List<Channel>,
        isLoading: Boolean,
        onChannelSelected: (Channel) -> Unit,
        modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Search Header
            Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                    shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                        text = "🔍 Search Channels",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(16.dp)
                )
            }

            // Search Field
            OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    label = { Text("Search channels...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon =
                            if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { onQueryChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                    "Searching channels...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                searchQuery.length < 2 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.surfaceVariant
                                        )
                        ) {
                            Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                        text = "🔍",
                                        style = MaterialTheme.typography.displayMedium,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                        text = "Type at least 2 characters to search",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                channels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.surfaceVariant
                                        )
                        ) {
                            Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                        text = "😔",
                                        style = MaterialTheme.typography.displayMedium,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                        text = "No channels found for \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(channels) { channel ->
                            ChannelItem(channel = channel, onClick = { onChannelSelected(channel) })
                        }
                    }
                }
            }
        }
    }
}
