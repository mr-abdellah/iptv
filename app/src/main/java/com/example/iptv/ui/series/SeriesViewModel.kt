package com.example.iptv.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptv.data.model.*
import com.example.iptv.data.repository.XtreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SeriesUiState(
        val isLoading: Boolean = false,
        val categories: List<Category> = emptyList(),
        val series: List<Series> = emptyList(),
        val errorMessage: String? = null,
        val selectedCategory: Category? = null
)

data class SeriesDetailUiState(
        val isLoading: Boolean = false,
        val seriesDetail: SeriesDetailResponse? = null,
        val errorMessage: String? = null,
        val selectedSeason: Int = 1
)

class SeriesViewModel(private val repository: XtreamRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    private val _seriesDetailState = MutableStateFlow(SeriesDetailUiState())
    val seriesDetailState: StateFlow<SeriesDetailUiState> = _seriesDetailState.asStateFlow()

    init {
        loadSeriesCategories()
    }

    fun loadSeriesCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val categories = repository.getSeriesCategories()
                _uiState.value = _uiState.value.copy(categories = categories, isLoading = false)

                // Auto-select first category
                if (categories.isNotEmpty()) {
                    loadSeries(categories.first())
                }
            } catch (e: Exception) {
                _uiState.value =
                        _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to load series categories: ${e.message}"
                        )
            }
        }
    }

    fun loadSeries(category: Category) {
        viewModelScope.launch {
            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null,
                            selectedCategory = category
                    )

            try {
                val series = repository.getSeries(category.categoryId)
                _uiState.value = _uiState.value.copy(series = series, isLoading = false)
            } catch (e: Exception) {
                _uiState.value =
                        _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to load series: ${e.message}"
                        )
            }
        }
    }

    fun loadSeriesDetail(seriesId: Int) {
        viewModelScope.launch {
            _seriesDetailState.value =
                    _seriesDetailState.value.copy(isLoading = true, errorMessage = null)

            try {
                val seriesDetail = repository.getSeriesDetail(seriesId)
                _seriesDetailState.value =
                        _seriesDetailState.value.copy(
                                seriesDetail = seriesDetail,
                                isLoading = false
                        )
            } catch (e: Exception) {
                _seriesDetailState.value =
                        _seriesDetailState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to load series details: ${e.message}"
                        )
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        _seriesDetailState.value = _seriesDetailState.value.copy(selectedSeason = seasonNumber)
    }

    fun searchSeries(query: String) {
        if (query.isBlank()) {
            _uiState.value.selectedCategory?.let { loadSeries(it) }
            return
        }

        val filteredSeries =
                _uiState.value.series.filter { series ->
                    series.name.contains(query, ignoreCase = true)
                }

        _uiState.value = _uiState.value.copy(series = filteredSeries)
    }
}
