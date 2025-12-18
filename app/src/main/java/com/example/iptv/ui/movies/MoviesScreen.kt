package com.example.iptv.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.iptv.data.model.Category
import com.example.iptv.data.model.Movie
import com.example.iptv.data.repository.XtreamRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
        repository: XtreamRepository,
        onMovieClick: (Movie) -> Unit,
        onBack: () -> Unit,
        viewModel: MoviesViewModel = viewModel { MoviesViewModel(repository) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = "Movies",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Movies")
                        }
                    }
                }

                // Search Bar
                if (showSearch) {
                    OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.searchMovies(it)
                            },
                            label = { Text("Search movies...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                            onClick = {
                                                searchQuery = ""
                                                viewModel.searchMovies("")
                                            }
                                    ) { Icon(Icons.Default.Clear, contentDescription = "Clear") }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            keyboardOptions =
                                    KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                            singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Categories
        if (uiState.categories.isNotEmpty()) {
            LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.categories) { category ->
                    CategoryChip(
                            category = category,
                            isSelected = category == uiState.selectedCategory,
                            onClick = { viewModel.loadMovies(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading movies...")
                        }
                    }
                }
                uiState.errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                    text = uiState.errorMessage ?: "Unknown error",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadMovieCategories() }) { Text("Retry") }
                        }
                    }
                }
                uiState.movies.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                                text =
                                        if (searchQuery.isNotEmpty())
                                                "No movies found for \"$searchQuery\""
                                        else "No movies available",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.movies) { movie ->
                            MovieCard(movie = movie, onClick = { onMovieClick(movie) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: Category, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
            onClick = onClick,
            label = { Text(category.categoryName) },
            selected = isSelected,
            modifier = Modifier.focusable()
    )
}

@Composable
private fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).clickable { onClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster Image
            AsyncImage(
                    model = movie.streamIcon,
                    contentDescription = movie.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
            )

            // Gradient overlay for better text readability
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(
                                            brush =
                                                    Brush.verticalGradient(
                                                            colors =
                                                                    listOf(
                                                                            Color.Transparent,
                                                                            Color.Black.copy(
                                                                                    alpha = 0.7f
                                                                            )
                                                                    ),
                                                            startY = 200f
                                                    )
                                    )
            )

            // Movie Title
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(
                        text = movie.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )

                // Rating if available
                movie.rating5Based?.let { rating ->
                    if (rating > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = "★ ${String.format("%.1f", rating)}/5",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Yellow
                        )
                    }
                }
            }
        }
    }
}
