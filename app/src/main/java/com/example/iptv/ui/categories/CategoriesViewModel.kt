package com.example.iptv.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptv.data.model.Category
import com.example.iptv.data.repository.XtreamRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoriesUiState(
        val categories: List<Category> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null
)

class CategoriesViewModel(private val repository: XtreamRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null
    private var lastLoadTime = 0L
    private val minLoadInterval = 30000L // 30 seconds between category reloads

    init {
        loadCategories()
    }

    fun loadCategories() {
        // Prevent too frequent reloads
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLoadTime < minLoadInterval && _uiState.value.categories.isNotEmpty()
        ) {
            return
        }

        // Cancel any ongoing load to prevent duplicates
        loadJob?.cancel()

        // Don't reload if already loading
        if (_uiState.value.isLoading) {
            return
        }

        lastLoadTime = currentTime
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        loadJob =
                viewModelScope.launch {
                    try {
                        // Add small delay to prevent rapid fire requests
                        kotlinx.coroutines.delay(100)

                        repository
                                .getCategories()
                                .onSuccess { categories ->
                                    _uiState.value =
                                            _uiState.value.copy(
                                                    categories = categories,
                                                    isLoading = false,
                                                    errorMessage = null
                                            )
                                }
                                .onFailure { error ->
                                    _uiState.value =
                                            _uiState.value.copy(
                                                    isLoading = false,
                                                    errorMessage = error.message
                                                                    ?: "Failed to load categories"
                                            )
                                }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        // Ignore cancellation
                    }
                }
    }
}
