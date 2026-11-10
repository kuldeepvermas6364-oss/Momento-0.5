package org.company.app.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

fun extractYoutubeVideoId(input: String?): String {
    if (input.isNullOrBlank()) return ""
    val trimmed = input.trim()
    if (!trimmed.contains("/") && !trimmed.contains("=") && !trimmed.contains("?")) {
        return trimmed
    }
    if (trimmed.contains("youtu.be/")) {
        return trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("&").substringBefore("/")
    }
    if (trimmed.contains("v=")) {
        return trimmed.substringAfter("v=").substringBefore("&").substringBefore("?")
    }
    if (trimmed.contains("/shorts/")) {
        return trimmed.substringAfter("/shorts/").substringBefore("?").substringBefore("&").substringBefore("/")
    }
    if (trimmed.contains("/embed/")) {
        return trimmed.substringAfter("/embed/").substringBefore("?").substringBefore("&").substringBefore("/")
    }
    return trimmed.substringAfterLast("/").substringBefore("?").substringBefore("&")
}

@Composable
fun YoutubeVideoPlayer(
    modifier: Modifier = Modifier,
    youtubeURL: String?,
    autoPlay: Boolean = true,
    isPlaying: (Boolean) -> Unit = {},
    isLoading: (Boolean) -> Unit = {},
    onVideoEnded: () -> Unit = {}
) {
    val mContext = LocalContext.current
    val mLifeCycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val videoId = remember(youtubeURL) { extractYoutubeVideoId(youtubeURL) }
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    val playerFragment = remember { YouTubePlayerView(mContext) }

    LaunchedEffect(videoId, player) {
        val p = player
        if (videoId.isNotEmpty() && p != null) {
            if (autoPlay) {
                p.loadVideo(videoId, 0f)
            } else {
                p.cueVideo(videoId, 0f)
            }
        }
    }

    val playerStateListener = remember(videoId) {
        object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)
                player = youTubePlayer
                if (videoId.isNotEmpty()) {
                    if (autoPlay) {
                        youTubePlayer.loadVideo(videoId, 0f)
                    } else {
                        youTubePlayer.cueVideo(videoId, 0f)
                    }
                }
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                super.onStateChange(youTubePlayer, state)
                when (state) {
                    PlayerConstants.PlayerState.BUFFERING -> {
                        isLoading(true)
                        isPlaying(false)
                    }
                    PlayerConstants.PlayerState.PLAYING -> {
                        isLoading(false)
                        isPlaying(true)
                    }
                    PlayerConstants.PlayerState.ENDED -> {
                        isPlaying(false)
                        isLoading(false)
                        onVideoEnded()
                    }
                    else -> {}
                }
            }

            override fun onError(
                youTubePlayer: YouTubePlayer,
                error: PlayerConstants.PlayerError
            ) {
                super.onError(youTubePlayer, error)
                println("YouTubePlayer Error Reason = $error")
            }
        }
    }

    val playerBuilder = remember {
        IFramePlayerOptions.Builder(mContext).apply {
            controls(1)
            fullscreen(0)
            autoplay(if (autoPlay) 1 else 0)
            modestBranding(1)
            ccLoadPolicy(1)
            rel(0)
        }
    }

    AndroidView(
        modifier = modifier.background(Color.Black),
        factory = {
            playerFragment.apply {
                enableAutomaticInitialization = false
                initialize(playerStateListener, playerBuilder.build())
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            try {
                playerFragment.removeYouTubePlayerListener(playerStateListener)
                playerFragment.release()
            } catch (e: Exception) {
                // Ignore
            }
            player = null
        }
    }

    DisposableEffect(mLifeCycleOwner) {
        val lifecycle = mLifeCycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> player?.play()
                Lifecycle.Event.ON_PAUSE -> player?.pause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

fun splitLinkForVideoId(url: String?): String {
    return extractYoutubeVideoId(url)
}