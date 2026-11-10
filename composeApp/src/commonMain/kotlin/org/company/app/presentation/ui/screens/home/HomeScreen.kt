package org.company.app.presentation.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator 
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue 
import androidx.compose.runtime.mutableStateOf 
import androidx.compose.runtime.remember 
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch 
import org.company.app.UserRegion
import org.company.app.domain.usecases.ResultState
import org.company.app.presentation.ui.components.error.ErrorScreen
import org.company.app.presentation.ui.components.shimmer.ShimmerEffectMain
import org.company.app.presentation.ui.components.video_list.VideosList
import org.company.app.presentation.viewmodel.MainViewModel
import org.company.app.presentation.viewmodel.RemoteSettingsViewModel
import org.koin.compose.koinInject

class HomeScreen() : Screen {
    @Composable
    override fun Content() {
        HomeContent()
    }
}
 
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeContent( 
    viewModel: MainViewModel = koinInject<MainViewModel>(),                                      
) {
    
    val remoteSettingsViewModel: RemoteSettingsViewModel = koinInject()
    val remoteSettings by remoteSettingsViewModel.settings.collectAsState()
    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) } 

    fun refresh() = refreshScope.launch {
        refreshing = true
        viewModel.getVideosList(UserRegion())
        delay(1500)
        refreshing = false 
    }
    
    val pullRefreshState = rememberPullRefreshState(refreshing, ::refresh)
  
    LaunchedEffect(pullRefreshState) {
        viewModel.getVideosList(UserRegion()) 
    } 
    
    if (remoteSettings.maintenanceMode) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                remoteSettings.maintenanceMessage.ifBlank { "Momento is temporarily unavailable. Please try again soon." },
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            if (remoteSettings.announcementEnabled && remoteSettings.announcement.isNotBlank()) {
                Text(
                    remoteSettings.announcement,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pullRefresh(pullRefreshState),
                contentAlignment = Alignment.Center
            ) {
                if (!refreshing) {
                    val state by viewModel.videos.collectAsState()
                    when (state) {
                        is ResultState.LOADING -> ShimmerEffectMain()
                        is ResultState.SUCCESS -> {
                            val data = (state as ResultState.SUCCESS).response
                            VideosList(data)
                        }
                        is ResultState.ERROR -> {
                            val error = (state as ResultState.ERROR).error
                            ErrorScreen(error, onRetry = {
                                viewModel.getVideosList(UserRegion())
                            })
                        }
                    }
                }
                PullRefreshIndicator(
                    refreshing, pullRefreshState,
                    Modifier.wrapContentSize().align(Alignment.TopCenter)
                )
            }
        }
    }
}
