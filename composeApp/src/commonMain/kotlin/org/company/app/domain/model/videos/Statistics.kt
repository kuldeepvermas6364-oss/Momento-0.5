package org.company.app.domain.model.videos


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Statistics(
    @SerialName("commentCount")
    val commentCount: String? = null,
    @SerialName("favoriteCount")
    val favoriteCount: String = "0",
    @SerialName("likeCount")
    val likeCount: String? = null,
    @SerialName("viewCount")
    val viewCount: String = "0"
)