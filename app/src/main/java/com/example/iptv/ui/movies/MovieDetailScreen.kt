package com.example.iptv.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.iptv.data.model.Movie
import com.example.iptv.data.repository.XtreamRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
        movie: Movie,
        repository: XtreamRepository,
        onPlay: () -> Unit,
        onBack: () -> Unit,
        viewModel: MoviesViewModel = viewModel { MoviesViewModel(repository) }
) {
    val movieDetailState by viewModel.movieDetailState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(movie.streamId) { viewModel.loadMovieDetail(movie.streamId) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            movieDetailState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading movie details...")
                    }
                }
            }
            movieDetailState.errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val errorMsg = movieDetailState.errorMessage
                        Text(
                                text = errorMsg ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadMovieDetail(movie.streamId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            movieDetailState.movieDetail != null -> {
                val movieDetail = movieDetailState.movieDetail!!
                val movieInfo = movieDetail.info

                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    // Header with poster and basic info
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                        // Background image
                        AsyncImage(
                                model = movieInfo.coverBig
                                                ?: movieInfo.movieImage ?: movie.streamIcon,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                        )

                        // Dark gradient overlay
                        Box(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .background(
                                                        brush =
                                                                Brush.verticalGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        Color.Black
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.3f
                                                                                                ),
                                                                                        Color.Black
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.8f
                                                                                                )
                                                                                )
                                                                )
                                                )
                        )

                        // Back button
                        IconButton(
                                onClick = onBack,
                                modifier =
                                        Modifier.align(Alignment.TopStart)
                                                .padding(16.dp)
                                                .background(
                                                        Color.Black.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(50)
                                                )
                        ) {
                            Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                            )
                        }

                        // Movie info overlay
                        Row(
                                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                                verticalAlignment = Alignment.Bottom
                        ) {
                            // Poster
                            AsyncImage(
                                    model = movie.streamIcon ?: movieInfo.movieImage,
                                    contentDescription = movie.name,
                                    modifier =
                                            Modifier.width(120.dp)
                                                    .height(180.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Movie details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = movieInfo.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                )

                                movieInfo.originalName?.let { originalName ->
                                    if (originalName != movieInfo.name) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                                text = originalName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Rating and release date row
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    movieInfo.rating5Based?.let { rating ->
                                        if (rating > 0) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = Color.Yellow,
                                                        modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                        text = String.format("%.1f", rating),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    movieInfo.releaseDate?.let { releaseDate ->
                                        if (movieInfo.rating5Based != null &&
                                                        movieInfo.rating5Based > 0
                                        ) {
                                            Text(
                                                    text = " • ",
                                                    color = Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                        Text(
                                                text = releaseDate,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                movieInfo.duration?.let { duration ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                            text = "Duration: $duration",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons
                    Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                    )
                    ) {
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(onClick = { onPlay() }, modifier = Modifier.weight(1f)) {
                                Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play Movie")
                            }

                            OutlinedButton(
                                    onClick = { /* TODO: Implement share */},
                                    modifier = Modifier.width(120.dp)
                            ) {
                                Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share")
                            }
                        }
                    }

                    // Movie details
                    Card(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                    )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            movieInfo.plot?.let { plot ->
                                Text(
                                        text = "Synopsis",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = plot,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            movieInfo.genre?.let { genre ->
                                DetailItem(label = "Genre", value = genre)
                            }

                            movieInfo.director?.let { director ->
                                DetailItem(label = "Director", value = director)
                            }

                            movieInfo.actors?.let { actors ->
                                DetailItem(label = "Cast", value = actors)
                            }

                            movieInfo.country?.let { country ->
                                DetailItem(label = "Country", value = country)
                            }

                            movieInfo.mpaaRating?.let { rating ->
                                DetailItem(label = "Rating", value = rating)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            else -> {
                // Fallback: Show basic movie info while waiting for details
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Movie details not available")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadMovieDetail(movie.streamId) }) {
                            Text("Load Details")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
