package org.company.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.withTimeout
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

    private suspend inline fun <reified T> safeGet(endpoint: String): T {
        val currentKey = Constant.API_KEY.trim()
        val separator = if (endpoint.contains("?")) "&" else "?"
        val baseUrl = Constant.BASE_URL.removeSuffix("/")
        val path = endpoint.removePrefix("/")
        
        // 1. Try Primary Key (User Secret)
        if (currentKey.isNotEmpty() && currentKey != Constant.DEFAULT_API_KEY) {
            try {
                val fullUrl = "$baseUrl/$path${separator}key=$currentKey"
                println("YouTube API [Primary]: $fullUrl")
                val response: HttpResponse = withTimeout(8000) {
                    client.get(fullUrl)
                }
                if (response.status.isSuccess()) {
                    val body: T = response.body()
                    // Check if it's an empty "success" response (often happens with bad keys or limits)
                    if (body is Youtube && body.items.isEmpty()) {
                        println("YouTube API [Primary] returned 0 items. Falling back.")
                    } else if (body is Search && body.items.isNullOrEmpty()) {
                        println("YouTube API [Primary Search] returned 0 items. Falling back.")
                    } else {
                        return body
                    }
                } else {
                    println("YouTube API [Primary] failed with status: ${response.status}")
                }
            } catch (e: Throwable) {
                println("YouTube API [Primary] error: ${e.message}")
            }
        }

        // 2. Try Default Fallback Key
        val fallbackUrl = "$baseUrl/$path${separator}key=${Constant.DEFAULT_API_KEY}"
        println("YouTube API [Fallback]: $fallbackUrl")
        return try {
            val response: HttpResponse = withTimeout(10000) {
                client.get(fallbackUrl)
            }
            response.body()
        } catch (e: Throwable) {
            println("YouTube API [Fallback] fatal error: ${e.message}")
            throw e
        }
    }

    suspend fun getVideoList(userRegion: String): Youtube {
        val region = userRegion.ifBlank { "US" }
        return safeGet("videos?part=snippet,contentDetails,statistics,liveStreamingDetails,player,recordingDetails&chart=mostPopular&regionCode=$region&maxResults=50")
    }

    suspend fun getRelevance(): Youtube {
        return safeGet("videos?part=contentDetails%2Csnippet%2Cstatistics&id=${getRandomVideoIds()}")
    }

    suspend fun getVideosUsingIds(ids: String): Youtube {
        return safeGet("videos?id=$ids&part=snippet,contentDetails,statistics&maxResults=20")
    }

    suspend fun getChannelDetails(channelId: String): Channel {
        return safeGet("channels?part=contentDetails,brandingSettings,contentOwnerDetails,localizations,snippet,statistics,status,topicDetails&id=$channelId")
    }

    suspend fun getChannelBranding(channelId: String): Channel {
        return safeGet("channels?part=brandingSettings&id=$channelId")
    }

    suspend fun getRelevanceVideos(): Youtube {
        return safeGet("search?part=snippet&order=date&type=video&videoEmbeddable=true&maxResults=50&regionCode=us")
    }

    suspend fun getSearch(query: String, userRegion: String): Search {
        val region = userRegion.ifBlank { "US" }
        return safeGet("search?part=snippet&q=$query&type=video&maxResults=50&regionCode=$region")
    }

    suspend fun getPlaylists(channelId: String): Youtube {
        return safeGet("playlists?part=snippet,contentDetails,localizations,player,status&channelId=$channelId&maxResults=50")
    }

    suspend fun getChannelSections(channelId: String): Youtube {
        return safeGet("channelSections?part=snippet,contentDetails&channelId=$channelId")
    }

    suspend fun getChannelLiveStreams(channelID: String): Search {
        return safeGet("search?part=snippet&eventType=live&type=video&channelId=$channelID&maxResults=50&regionCode=us")
    }

    suspend fun getChannelVideos(playlistID: String): Youtube {
        return safeGet("playlistItems?part=snippet,contentDetails,status&maxResults=50&playlistId=$playlistID")
    }

    suspend fun getOwnChannelVideos(channelId: String): Search {
        return safeGet("search?part=snippet&channelId=$channelId&type=video&maxResults=50")
    }

    suspend fun getChannelCommunity(channelId: String): Youtube {
        return safeGet("activities?part=snippet,contentDetails&channelId=$channelId&maxResults=50")
    }

    suspend fun getComments(videoId: String, order: String): Comments {
        return safeGet("commentThreads?part=snippet,replies&videoId=$videoId&order=$order&maxResults=100")
    }

    suspend fun getVideoCategories(): VideoCategories {
        return safeGet("videoCategories?part=snippet&maxResults=50&regionCode=us")
    }

    suspend fun getSingleVideoDetail(videoId: String): Youtube {
        return safeGet("videos?part=snippet,contentDetails,statistics&id=$videoId")
    }

    suspend fun getMultipleVideos(videoId: String): Youtube {
        return safeGet("videos?part=snippet,contentDetails,statistics&id=$videoId")
    }

    suspend fun getChannelSearch(channelID: String, query: String): Search {
        return safeGet("search?part=snippet&channelId=$channelID&q=$query")
    }
}