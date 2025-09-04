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