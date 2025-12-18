package com.example.iptv.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptv.data.model.Channel
import com.example.iptv.data.repository.XtreamRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChannelsUiState(
        val channels: List<Channel> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val categoryName: String = ""
)

class ChannelsViewModel(private val repository: XtreamRepository) : ViewModel() {

        private val _uiState = MutableStateFlow(ChannelsUiState())
        val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

        private var favoritesRepository: com.example.iptv.data.repository.FavoritesRepository? =
                null

        fun setFavoritesRepository(
                favRepository: com.example.iptv.data.repository.FavoritesRepository
        ) {
                favoritesRepository = favRepository
        }

        private var channelsJob: kotlinx.coroutines.Job? = null
        private val categoryCache = mutableMapOf<String, List<Channel>>()

        // NEW: Cache to map streamId to Channel for instant favorites lookup
        private val channelByIdCache = mutableMapOf<Int, Channel>()

        private var lastChannelLoadTime = 0L
        private val minChannelLoadInterval = 5000L

        fun loadChannels(categoryId: String, categoryName: String) {
                // Check cache first for instant loading
                categoryCache[categoryId]?.let { cachedChannels ->
                        _uiState.value =
                                _uiState.value.copy(
                                        channels = cachedChannels,
                                        isLoading = false,
                                        errorMessage = null,
                                        categoryName = categoryName
                                )
                        return
                }

                // Prevent too frequent requests to same category
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastChannelLoadTime < minChannelLoadInterval &&
                                _uiState.value.categoryName == categoryName
                ) {
                        return
                }

                // Cancel any ongoing request
                channelsJob?.cancel()

                // Don't start new request if already loading same category
                if (_uiState.value.isLoading && _uiState.value.categoryName == categoryName) {
                        return
                }

                lastChannelLoadTime = currentTime
                _uiState.value =
                        _uiState.value.copy(
                                isLoading = true,
                                errorMessage = null,
                                categoryName = categoryName
                        )

                channelsJob =
                        viewModelScope.launch {
                                try {
                                        // Small delay to prevent rapid requests
                                        kotlinx.coroutines.delay(200)

                                        repository
                                                .getChannels(categoryId)
                                                .onSuccess { channels ->
                                                        // Cache the results
                                                        categoryCache[categoryId] = channels

                                                        // NEW: Update channel by ID cache for fast
                                                        // favorites
                                                        channels.forEach { channel ->
                                                                channelByIdCache[channel.streamId] =
                                                                        channel
                                                        }

                                                        _uiState.value =
                                                                _uiState.value.copy(
                                                                        channels = channels,
                                                                        isLoading = false,
                                                                        errorMessage = null
                                                                )
                                                }
                                                .onFailure { error ->
                                                        _uiState.value =
                                                                _uiState.value.copy(
                                                                        isLoading = false,
                                                                        errorMessage = error.message
                                                                                        ?: "Failed to load channels"
                                                                )
                                                }
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                        // Ignore cancellation
                                }
                        }
        }

        fun getStreamUrl(streamId: Int): String? {
                return repository.getStreamUrl(streamId)
        }

        fun loadFavoriteChannels() {
                _uiState.value =
                        _uiState.value.copy(
                                isLoading = true,
                                errorMessage = null,
                                categoryName = "⭐ Favorites"
                        )

                viewModelScope.launch {
                        try {
                                val favoriteIds =
                                        favoritesRepository?.favorites?.value ?: emptySet()

                                if (favoriteIds.isEmpty()) {
                                        _uiState.value =
                                                _uiState.value.copy(
                                                        channels = emptyList(),
                                                        isLoading = false,
                                                        errorMessage = null
                                                )
                                        return@launch
                                }

                                // NEW: Instantly get favorites from cache (no API calls!)
                                val favoriteChannels =
                                        favoriteIds.mapNotNull { id -> channelByIdCache[id] }

                                // If we have all favorites in cache, show them instantly
                                if (favoriteChannels.size == favoriteIds.size) {
                                        _uiState.value =
                                                _uiState.value.copy(
                                                        channels = favoriteChannels,
                                                        isLoading = false,
                                                        errorMessage = null
                                                )
                                } else {
                                        // Some favorites not in cache - need to load them
                                        // Only load categories we haven't loaded yet
                                        val missingIds =
                                                favoriteIds.filter { id ->
                                                        !channelByIdCache.containsKey(id)
                                                }

                                        // Load channels from API to find missing favorites
                                        repository.getCategories().onSuccess { categories ->
                                                // Load channels from each category until we find
                                                // all favorites
                                                for (category in categories) {
                                                        if (missingIds.isEmpty()) break

                                                        repository.getChannels(category.categoryId)
                                                                .onSuccess { channels ->
                                                                        channels.forEach { channel
                                                                                ->
                                                                                channelByIdCache[
                                                                                        channel.streamId] =
                                                                                        channel
                                                                        }
                                                                }
                                                }
                                        }

                                        // Now get all favorites from updated cache
                                        val allFavorites =
                                                favoriteIds.mapNotNull { id ->
                                                        channelByIdCache[id]
                                                }

                                        _uiState.value =
                                                _uiState.value.copy(
                                                        channels = allFavorites,
                                                        isLoading = false,
                                                        errorMessage = null
                                                )
                                }
                        } catch (error: Exception) {
                                _uiState.value =
                                        _uiState.value.copy(
                                                isLoading = false,
                                                errorMessage = error.message
                                                                ?: "Failed to load favorites"
                                        )
                        }
                }
        }

        private var searchJob: kotlinx.coroutines.Job? = null

        fun searchChannels(query: String) {
                // Cancel previous search to prevent multiple concurrent searches
                searchJob?.cancel()

                _uiState.value =
                        _uiState.value.copy(
                                isLoading = true,
                                errorMessage = null,
                                categoryName = "Search: $query"
                        )

                searchJob =
                        viewModelScope.launch {
                                try {
                                        // Add delay to prevent rapid API calls (throttling)
                                        kotlinx.coroutines.delay(500)

                                        // Search in cache first (instant!)
                                        val cachedResults =
                                                channelByIdCache
                                                        .values
                                                        .filter { channel ->
                                                                channel.name.contains(
                                                                        query,
                                                                        ignoreCase = true
                                                                )
                                                        }
                                                        .take(20)

                                        if (cachedResults.isNotEmpty()) {
                                                // Show cached results immediately
                                                _uiState.value =
                                                        _uiState.value.copy(
                                                                channels = cachedResults,
                                                                isLoading = false,
                                                                errorMessage = null
                                                        )
                                        } else {
                                                // No cached results, search in first category
                                                val searchResults = mutableListOf<Channel>()

                                                repository.getCategories().onSuccess { categories ->
                                                        if (categories.isNotEmpty()) {
                                                                val firstCategory =
                                                                        categories.first()
                                                                repository.getChannels(
                                                                                firstCategory
                                                                                        .categoryId
                                                                        )
                                                                        .onSuccess { channels ->
                                                                                searchResults
                                                                                        .addAll(
                                                                                                channels
                                                                                                        .filter {
                                                                                                                channel
                                                                                                                ->
                                                                                                                channel.name
                                                                                                                        .contains(
                                                                                                                                query,
                                                                                                                                ignoreCase =
                                                                                                                                        true
                                                                                                                        )
                                                                                                        }
                                                                                                        .take(
                                                                                                                20
                                                                                                        )
                                                                                        )
                                                                        }
                                                        }
                                                }

                                                _uiState.value =
                                                        _uiState.value.copy(
                                                                channels = searchResults,
                                                                isLoading = false,
                                                                errorMessage = null
                                                        )
                                        }
                                } catch (error: Exception) {
                                        if (error !is CancellationException) {
                                                _uiState.value =
                                                        _uiState.value.copy(
                                                                isLoading = false,
                                                                errorMessage = error.message
                                                                                ?: "Failed to search channels"
                                                        )
                                        }
                                }
                        }
        }
}
