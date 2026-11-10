package org.company.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import org.company.app.core.common.Constant
import org.company.app.domain.model.categories.VideoCategories
import org.company.app.domain.model.channel.Channel
import org.company.app.domain.model.comments.Comments
import org.company.app.domain.model.search.Search
import org.company.app.domain.model.videos.Youtube

class YoutubeClientApi(
    private val client: HttpClient
) {
    private fun getRandomVideoIds(): String? {
        val videoIds = listOf(
            "Ra9KOsF4BjY,XTtTtlVhh5g,NIM_drzGMJk,-yMaznEn1LM,NiMWG8KyBY4,bQpGIcRphCo,urXQVXswcgc,8OVCL0lMV1I,8qcqpwJ1LBw,C_sEA0c94zs,wxBtwCZtDAg,IX0QVURTqb8,74pnrYPozcU,MDZlKThUOSw,At4T3Ujv4xk,AIt_6ReFzEw,D_rY6oJlCoM,pXtmG1b1-Pw,Fq23VNXRV40,gzlwegTk1AM,Y9HKtiq0q9A,wjY_1Z27_sE,GFo32hjd1QY,aTCq7fHpcww,-Cqe6xIe3ys"
        )
        return videoIds.shuffled().firstOrNull()
    }

    private suspend fun executeRequest(endpoint: String, key: String, params: Map<String, String>): HttpResponse {
        return client.get(endpoint) {
            parameter("key", key)
            params.forEach { (k, v) -> parameter(k, v) }
        }
    }

    private suspend inline fun <reified T> safeGet(endpoint: String, params: Map<String, String> = emptyMap()): T {
        val currentKey = Constant.API_KEY.trim()
        
        return try {
            val response = executeRequest(endpoint, currentKey, params)
            if (response.status.isSuccess()) {
                val body: T = response.body()
                if ((body is Youtube && body.items.isEmpty()) || (body is Search && body.items.isNullOrEmpty())) {
                    if (currentKey != Constant.DEFAULT_API_KEY) {
                        println("YouTube API: Primary key returned empty. Trying fallback.")
                        executeRequest(endpoint, Constant.DEFAULT_API_KEY, params).body()
                    } else body
                } else body
            } else {
                if (currentKey != Constant.DEFAULT_API_KEY) {
                    println("YouTube API: Primary key failed (${response.status}). Trying fallback.")
                    executeRequest(endpoint, Constant.DEFAULT_API_KEY, params).body()
                } else {
                    throw Exception("YouTube API Error: ${response.status}")
                }
            }
        } catch (e: Throwable) {
            if (currentKey != Constant.DEFAULT_API_KEY) {
                println("YouTube API: Primary request crashed. Trying fallback. Error: ${e.message}")
                executeRequest(endpoint, Constant.DEFAULT_API_KEY, params).body()
            } else {
                throw e
            }
        }
    }

    suspend fun getVideoList(userRegion: String): Youtube {
        val region = userRegion.ifBlank { "US" }
        return safeGet("videos", mapOf(
            "part" to "snippet,contentDetails,statistics",
            "chart" to "mostPopular",
            "regionCode" to region,
            "maxResults" to "50"
        ))
    }

    suspend fun getRelevance(): Youtube {
        return safeGet("videos", mapOf(
            "part" to "snippet,contentDetails,statistics",
            "id" to (getRandomVideoIds() ?: "")
        ))
    }

    suspend fun getVideosUsingIds(ids: String): Youtube {
        return safeGet("videos", mapOf(
            "id" to ids,
            "part" to "snippet,contentDetails,statistics",
            "maxResults" to "20"
        ))
    }

    suspend fun getChannelDetails(channelId: String): Channel {
        return safeGet("channels", mapOf(
            "part" to "snippet,statistics,contentDetails,brandingSettings",
            "id" to channelId
        ))
    }

    suspend fun getChannelBranding(channelId: String): Channel {
        return safeGet("channels", mapOf(
            "part" to "brandingSettings",
            "id" to channelId
        ))
    }

    suspend fun getRelevanceVideos(): Youtube {
        return safeGet("search", mapOf(
            "part" to "snippet",
            "order" to "date",
            "type" to "video",
            "videoEmbeddable" to "true",
            "maxResults" to "50",
            "regionCode" to "us"
        ))
    }

    suspend fun getSearch(query: String, userRegion: String): Search {
        val region = userRegion.ifBlank { "US" }
        return safeGet("search", mapOf(
            "part" to "snippet",
            "q" to query,
            "type" to "video",
            "maxResults" to "50",
            "regionCode" to region
        ))
    }

    suspend fun getPlaylists(channelId: String): Youtube {
        return safeGet("playlists", mapOf(
            "part" to "snippet,contentDetails",
            "channelId" to channelId,
            "maxResults" to "50"
        ))
    }

    suspend fun getChannelSections(channelId: String): Youtube {
        return safeGet("channelSections", mapOf(
            "part" to "snippet,contentDetails",
            "channelId" to channelId
        ))
    }

    suspend fun getChannelLiveStreams(channelID: String): Search {
        return safeGet("search", mapOf(
            "part" to "snippet",
            "eventType" to "live",
            "type" to "video",
            "channelId" to channelID,
            "maxResults" to "50",
            "regionCode" to "us"
        ))
    }

    suspend fun getChannelVideos(playlistID: String): Youtube {
        return safeGet("playlistItems", mapOf(
            "part" to "snippet,contentDetails",
            "maxResults" to "50",
            "playlistId" to playlistID
        ))
    }

    suspend fun getOwnChannelVideos(channelId: String): Search {
        return safeGet("search", mapOf(
            "part" to "snippet",
            "channelId" to channelId,
            "type" to "video",
            "maxResults" to "50"
        ))
    }

    suspend fun getChannelCommunity(channelId: String): Youtube {
        return safeGet("activities", mapOf(
            "part" to "snippet,contentDetails",
            "channelId" to channelId,
            "maxResults" to "50"
        ))
    }

    suspend fun getComments(videoId: String, order: String): Comments {
        return safeGet("commentThreads", mapOf(
            "part" to "snippet,replies",
            "videoId" to videoId,
            "order" to order,
            "maxResults" to "100"
        ))
    }

    suspend fun getVideoCategories(): VideoCategories {
        return safeGet("videoCategories", mapOf(
            "part" to "snippet",
            "maxResults" to "50",
            "regionCode" to "us"
        ))
    }

    suspend fun getSingleVideoDetail(videoId: String): Youtube {
        return safeGet("videos", mapOf(
            "part" to "snippet,contentDetails,statistics",
            "id" to videoId
        ))
    }

    suspend fun getMultipleVideos(videoId: String): Youtube {
        return safeGet("videos", mapOf(
            "part" to "snippet,contentDetails,statistics",
            "id" to videoId
        ))
    }

    suspend fun getChannelSearch(channelID: String, query: String): Search {
        return safeGet("search", mapOf(
            "part" to "snippet",
            "channelId" to channelID,
            "q" to query
        ))
    }
}