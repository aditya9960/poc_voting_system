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