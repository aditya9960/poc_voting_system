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