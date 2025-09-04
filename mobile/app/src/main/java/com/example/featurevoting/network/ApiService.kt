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