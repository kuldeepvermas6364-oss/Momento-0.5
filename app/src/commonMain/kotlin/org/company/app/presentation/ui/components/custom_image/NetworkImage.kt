package org.company.app.presentation.ui.components.custom_image

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import io.kamel.core.Resource
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun NetworkImage(
    modifier: Modifier = Modifier,
    url: String,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    val cleanUrl = if (url.isBlank() || url == "null" || !url.startsWith("http")) {
        "https://i.ytimg.com/vi/YNtgQdfvhag/hqdefault.jpg"
    } else {
        url
    }
    val image: Resource<Painter> = asyncPainterResource(data = cleanUrl)
    KamelImage(
        resource = image,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onLoading = {
            Box(modifier = modifier.background(Color.Gray.copy(alpha = 0.3f)))
        },
        onFailure = {
            Box(modifier = modifier.background(Color.DarkGray))
        },
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = 50,
            easing = EaseInOutSine
        ),
    )
}