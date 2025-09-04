// MainActivity.kt
package com.example.featurevoting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.featurevoting.ui.screen.FeatureListScreen
import com.example.featurevoting.ui.theme.FeatureVotingTheme
import com.example.featurevoting.viewmodel.FeatureViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: FeatureViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeatureVotingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FeatureListScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// Data Models
// models/Feature.kt
package com.example.featurevoting.data.model

data class Feature(
    val id: Int,
    val title: String,
    val description: String,
    val author: String,
    val status: String,
    val voteCount: Int,
    val createdAt: String,
    val updatedAt: String
)

data class FeatureResponse(
    val features: List<Feature>,
    val pagination: Pagination
)

data class Pagination(
    val page: Int,
    val perPage: Int,
    val total: Int,
    val pages: Int,
    val hasNext: Boolean,
    val hasPrev: Boolean
)

data class VoteResponse(
    val message: String,
    val featureId: Int,
    val newVoteCount: Int
)

// API Service
// network/ApiService.kt
package com.example.featurevoting.data.network

import com.example.featurevoting.data.model.FeatureResponse
import com.example.featurevoting.data.model.VoteResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("api/features")
    suspend fun getFeatures(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("sort_by") sortBy: String = "vote_count"
    ): Response<FeatureResponse>
    
    @POST("api/features/{featureId}/vote")
    suspend fun voteFeature(
        @Path("featureId") featureId: Int,
        @Body request: Map<String, Int>
    ): Response<VoteResponse>
    
    @DELETE("api/features/{featureId}/vote")
    suspend fun removeVote(
        @Path("featureId") featureId: Int,
        @Body request: Map<String, Int>
    ): Response<VoteResponse>
    
    @GET("api/users/{userId}/votes")
    suspend fun getUserVotes(
        @Path("userId") userId: Int
    ): Response<Map<String, Any>>
}

// Repository
// repository/FeatureRepository.kt
package com.example.featurevoting.data.repository

import com.example.featurevoting.data.model.Feature
import com.example.featurevoting.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getFeatures(page: Int = 1, sortBy: String = "vote_count"): Result<List<Feature>> {
        return try {
            val response = apiService.getFeatures(page = page, sortBy = sortBy)
            if (response.isSuccessful) {
                Result.success(response.body()?.features ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch features: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun voteFeature(featureId: Int, userId: Int): Result<Int> {
        return try {
            val response = apiService.voteFeature(featureId, mapOf("user_id" to userId))
            if (response.isSuccessful) {
                Result.success(response.body()?.newVoteCount ?: 0)
            } else {
                Result.failure(Exception("Failed to vote: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeVote(featureId: Int, userId: Int): Result<Int> {
        return try {
            val response = apiService.removeVote(featureId, mapOf("user_id" to userId))
            if (response.isSuccessful) {
                Result.success(response.body()?.newVoteCount ?: 0)
            } else {
                Result.failure(Exception("Failed to remove vote: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ViewModel
// viewmodel/FeatureViewModel.kt
package com.example.featurevoting.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.featurevoting.data.model.Feature
import com.example.featurevoting.data.repository.FeatureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: FeatureRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()
    
    private val currentUserId = 1 // In real app, get from authentication
    
    init {
        loadFeatures()
    }
    
    fun loadFeatures(sortBy: String = "vote_count") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            repository.getFeatures(sortBy = sortBy)
                .onSuccess { features ->
                    _uiState.value = _uiState.value.copy(
                        features = features,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    fun voteFeature(featureId: Int) {
        viewModelScope.launch {
            repository.voteFeature(featureId, currentUserId)
                .onSuccess { newVoteCount ->
                    updateFeatureVoteCount(featureId, newVoteCount)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }
    
    fun removeVote(featureId: Int) {
        viewModelScope.launch {
            repository.removeVote(featureId, currentUserId)
                .onSuccess { newVoteCount ->
                    updateFeatureVoteCount(featureId, newVoteCount)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }
    
    private fun updateFeatureVoteCount(featureId: Int, newVoteCount: Int) {
        val updatedFeatures = _uiState.value.features.map { feature ->
            if (feature.id == featureId) {
                feature.copy(voteCount = newVoteCount)
            } else {
                feature
            }
        }
        _uiState.value = _uiState.value.copy(features = updatedFeatures)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class FeatureUiState(
    val features: List<Feature> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

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