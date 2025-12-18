package com.example.iptv.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.iptv.data.model.Episode
import com.example.iptv.data.model.Series
import com.example.iptv.data.repository.XtreamRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
        series: Series,
        repository: XtreamRepository,
        onEpisodePlay: (Episode, Series) -> Unit,
        onBack: () -> Unit,
        viewModel: SeriesViewModel = viewModel { SeriesViewModel(repository) }
) {
    val seriesDetailState by viewModel.seriesDetailState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(series.seriesId) { viewModel.loadSeriesDetail(series.seriesId) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            seriesDetailState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading series details...")
                    }
                }
            }
            seriesDetailState.errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val errorMsg = seriesDetailState.errorMessage
                        Text(
                                text = errorMsg ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadSeriesDetail(series.seriesId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            seriesDetailState.seriesDetail != null -> {
                val seriesDetail = seriesDetailState.seriesDetail!!
                val seriesInfo = seriesDetail.info
                val episodes = seriesDetail.episodes
                val selectedSeason = seriesDetailState.selectedSeason

                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    // Header with poster and basic info
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                        // Background image
                        val backgroundImage =
                                seriesInfo.backdropPath?.firstOrNull()
                                        ?: seriesInfo.cover ?: series.cover
                        AsyncImage(
                                model = backgroundImage,
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

                        // Series info overlay
                        Row(
                                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                                verticalAlignment = Alignment.Bottom
                        ) {
                            // Poster
                            AsyncImage(
                                    model = seriesInfo.cover ?: series.cover,
                                    contentDescription = series.name,
                                    modifier =
                                            Modifier.width(120.dp)
                                                    .height(180.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Series details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = seriesInfo.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Rating and release date row
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    seriesInfo.rating5Based?.let { rating ->
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

                                    seriesInfo.releaseDate?.let { releaseDate ->
                                        if (seriesInfo.rating5Based != null &&
                                                        seriesInfo.rating5Based > 0
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

                                seriesInfo.episodeRunTime?.let { runtime ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                            text = "Episode runtime: $runtime",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                    )
                                }

                                // Season count
                                if (seriesInfo.seasons?.isNotEmpty() == true) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                            text = "${seriesInfo.seasons.size} seasons",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // Series details
                    Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                    )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                    text = "About",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            seriesInfo.plot?.let { plot ->
                                Text(
                                        text = plot,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            seriesInfo.genre?.let { genre ->
                                DetailItem(label = "Genre", value = genre)
                            }

                            seriesInfo.director?.let { director ->
                                DetailItem(label = "Director", value = director)
                            }

                            seriesInfo.cast?.let { cast ->
                                DetailItem(label = "Cast", value = cast)
                            }
                        }
                    }

                    // Season selector
                    if (episodes.isNotEmpty()) {
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
                                        text = "Seasons",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(episodes.keys.toList().sorted()) { seasonKey ->
                                        val seasonNumber = seasonKey.toIntOrNull() ?: 1
                                        FilterChip(
                                                onClick = { viewModel.selectSeason(seasonNumber) },
                                                label = { Text("Season $seasonNumber") },
                                                selected = seasonNumber == selectedSeason
                                        )
                                    }
                                }
                            }
                        }

                        // Episodes list
                        val selectedSeasonEpisodes =
                                episodes[selectedSeason.toString()] ?: emptyList()

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
                                        text = "Season $selectedSeason Episodes",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                selectedSeasonEpisodes.forEach { episode ->
                                    EpisodeCard(
                                            episode = episode,
                                            onClick = { onEpisodePlay(episode, series) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (selectedSeasonEpisodes.isEmpty()) {
                                    Text(
                                            text = "No episodes available for this season",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color =
                                                    MaterialTheme.colorScheme.onSurface.copy(
                                                            alpha = 0.6f
                                                    ),
                                            modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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

@Composable
private fun EpisodeCard(episode: Episode, onClick: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Episode thumbnail placeholder
            Box(
                    modifier =
                            Modifier.size(80.dp, 45.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = "Episode ${episode.episodeNum}: ${episode.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )

                episode.info?.plot?.let { plot ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                            text = plot,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                    )
                }

                episode.info?.duration?.let { duration ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                            text = duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
