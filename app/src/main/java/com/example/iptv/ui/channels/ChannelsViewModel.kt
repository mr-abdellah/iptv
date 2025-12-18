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
        private var lastChannelLoadTime = 0L
        private val minChannelLoadInterval = 5000L // 5 seconds between same category loads

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

                                                        // Update cache with these channels for
                                                        // faster favorites
                                                        allChannelsCache.addAll(
                                                                channels.filter { newChannel ->
                                                                        !allChannelsCache.any {
                                                                                existing ->
                                                                                existing.streamId ==
                                                                                        newChannel
                                                                                                .streamId
                                                                        }
                                                                }
                                                        )

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

        // Cache to store all channels from all categories for quick favorites lookup
        private var allChannelsCache = mutableListOf<Channel>()
        private var cacheLoaded = false

        private suspend fun ensureCacheLoaded() {
                if (!cacheLoaded) {
                        repository.getCategories().onSuccess { categories ->
                                categories.forEach { category ->
                                        repository.getChannels(category.categoryId).onSuccess {
                                                channels ->
                                                allChannelsCache.addAll(channels)
                                        }
                                }
                        }
                        cacheLoaded = true
                }
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

                                // Ensure cache is loaded first time only
                                ensureCacheLoaded()

                                // Filter favorites from cache - instant!
                                val favoriteChannels =
                                        allChannelsCache.filter { channel ->
                                                favoriteIds.contains(channel.streamId)
                                        }

                                _uiState.value =
                                        _uiState.value.copy(
                                                channels = favoriteChannels,
                                                isLoading = false,
                                                errorMessage = null
                                        )
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

                                        // Limit search to prevent server overload
                                        // In production, implement server-side search
                                        val searchResults = mutableListOf<Channel>()

                                        // For now, search only in first category to prevent loops
                                        repository.getCategories().onSuccess { categories ->
                                                if (categories.isNotEmpty()) {
                                                        val firstCategory = categories.first()
                                                        repository.getChannels(
                                                                        firstCategory.categoryId
                                                                )
                                                                .onSuccess { channels ->
                                                                        searchResults.addAll(
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
                                                                                        ) // Limit
                                                                                // results
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
