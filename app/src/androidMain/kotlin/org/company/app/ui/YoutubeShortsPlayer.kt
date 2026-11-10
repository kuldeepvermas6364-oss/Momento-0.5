package org.company.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
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

@Composable
fun YoutubeShortsPlayer(
    modifier: Modifier = Modifier,
    youtubeURL: String?,
    isPlaying: (Boolean) -> Unit = {},
    isLoading: (Boolean) -> Unit = {},
    onVideoEnded: () -> Unit = {},
) {
    val mContext = LocalContext.current
    val mLifeCycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val videoId = remember(youtubeURL) { extractYoutubeVideoId(youtubeURL) }
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    val playerFragment = remember { YouTubePlayerView(mContext) }

    LaunchedEffect(videoId, player) {
        val p = player
        if (videoId.isNotEmpty() && p != null) {
            p.loadVideo(videoId, 0f)
        }
    }

    val playerStateListener = remember(videoId) {
        object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)
                player = youTubePlayer
                youTubePlayer.setLoop(true)
                if (videoId.isNotEmpty()) {
                    youTubePlayer.loadVideo(videoId, 0f)
                }
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState,
            ) {
                super.onStateChange(youTubePlayer, state)
                when (state) {
                    PlayerConstants.PlayerState.BUFFERING -> {
                        isLoading.invoke(true)
                        isPlaying.invoke(false)
                    }

                    PlayerConstants.PlayerState.PLAYING -> {
                        isLoading.invoke(false)
                        isPlaying.invoke(true)
                    }

                    PlayerConstants.PlayerState.ENDED -> {
                        isPlaying.invoke(false)
                        isLoading.invoke(false)
                        onVideoEnded.invoke()
                    }

                    else -> {}
                }
            }

            override fun onError(
                youTubePlayer: YouTubePlayer,
                error: PlayerConstants.PlayerError,
            ) {
                super.onError(youTubePlayer, error)
                println("ShortsPlayer Error = $error")
            }
        }
    }

    val playerBuilder = remember {
        IFramePlayerOptions.Builder(mContext).apply {
            controls(0)
            fullscreen(0)
            autoplay(1)
            modestBranding(0)
            rel(0)
            ccLoadPolicy(0)
            ivLoadPolicy(0)
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        factory = {
            playerFragment.apply {
                try {
                    enableAutomaticInitialization = false
                    initialize(playerStateListener, playerBuilder.build())
                } catch (e: Exception) {
                    println(e)
                }
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