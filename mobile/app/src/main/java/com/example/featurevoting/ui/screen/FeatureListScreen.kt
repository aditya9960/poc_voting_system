// UI Screen
// ui/screen/FeatureListScreen.kt
package com.example.featurevoting.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.featurevoting.viewmodel.FeatureViewModel
import com.example.featurevoting.data.model.Feature

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureListScreen(
    viewModel: FeatureViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var sortBy by remember { mutableStateOf("vote_count") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Feature Requests") },
            actions = {
                IconButton(onClick = { viewModel.loadFeatures(sortBy) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )

        // Sort Options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                onClick = {
                    sortBy = "vote_count"
                    viewModel.loadFeatures(sortBy)
                },
                label = { Text("Most Voted") },
                selected = sortBy == "vote_count"
            )
            FilterChip(
                onClick = {
                    sortBy = "created_at"
                    viewModel.loadFeatures(sortBy)
                },
                label = { Text("Recent") },
                selected = sortBy == "created_at"
            )
        }

        // Content
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.clearError() }) {
                            Text("Retry")
                        }
                    }
                }

                uiState.features.isEmpty() -> {
                    Text(
                        text = "No features found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.features) { feature ->
                            FeatureCard(
                                feature = feature,
                                onVoteClick = { viewModel.voteFeature(feature.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    feature: Feature,
    onVoteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    )