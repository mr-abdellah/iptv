package com.example.iptv.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptv.data.model.*
import com.example.iptv.data.repository.XtreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MoviesUiState(
        val isLoading: Boolean = false,
        val categories: List<Category> = emptyList(),
        val movies: List<Movie> = emptyList(),
        val errorMessage: String? = null,
        val selectedCategory: Category? = null
)

data class MovieDetailUiState(
        val isLoading: Boolean = false,
        val movieDetail: MovieDetailResponse? = null,
        val errorMessage: String? = null
)

class MoviesViewModel(private val repository: XtreamRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    private val _movieDetailState = MutableStateFlow(MovieDetailUiState())
    val movieDetailState: StateFlow<MovieDetailUiState> = _movieDetailState.asStateFlow()

    init {
        loadMovieCategories()
    }

    fun loadMovieCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val categories = repository.getMovieCategories()
                _uiState.value = _uiState.value.copy(categories = categories, isLoading = false)

                // Auto-select first category
                if (categories.isNotEmpty()) {
                    loadMovies(categories.first())
                }
            } catch (e: Exception) {
                _uiState.value =
                        _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to load movie categories: ${e.message}"
                        )
            }
        }
    }

    fun loadMovies(category: Category) {
        viewModelScope.launch {
            _uiState.value =
                    _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null,
                            selectedCategory = category
                    )

            try {
                val movies = repository.getMovies(category.categoryId)
                _uiState.value = _uiState.value.copy(movies = movies, isLoading = false)
            } catch (e: Exception) {
                _uiState.value =
                        _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to load movies: ${e.message}"
                        )
            }
        }
    }

    fun loadMovieDetail(movieId: Int) {
        viewModelScope.launch {
            _movieDetailState.value =
                    _movieDetailState.value.copy(isLoading = true, errorMessage = null)

            try {
                val movieDetail = repository.getMovieDetail(movieId)
                _movieDetailState.value =
                        _movieDetailState.value.copy(movieDetail = movieDetail, isLoading = false)
            } catch (e: Exception) {
                _movieDetailState.value =
                        _movieDetailState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to load movie details: ${e.message}"
                        )
            }
        }
    }

    fun searchMovies(query: String) {
        if (query.isBlank()) {
            _uiState.value.selectedCategory?.let { loadMovies(it) }
            return
        }

        val filteredMovies =
                _uiState.value.movies.filter { movie ->
                    movie.name.contains(query, ignoreCase = true)
                }

        _uiState.value = _uiState.value.copy(movies = filteredMovies)
    }
}
