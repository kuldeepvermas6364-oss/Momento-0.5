package org.company.app.ads

import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val FIRST_AD_KEY = "1bc1af0a695b6032f01072b6b2415799"
private const val SECOND_AD_KEY = "b4d1851b4c4ca0cf14e3771ba676a973"

@Composable
actual fun AdsterraFeedAd(unitNumber: Int) {
    val safeUnit = if (unitNumber == 2) 2 else 1
    val html = if (safeUnit == 1) {
        """
        <!doctype html>
        <html><head>
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <style>html,body{margin:0;padding:0;width:100%;min-height:280px;overflow:hidden;background:transparent}#container-$FIRST_AD_KEY{width:100%;min-height:280px}</style>
        </head><body>
          <script async="async" data-cfasync="false" src="https://bicea.org/21/$FIRST_AD_KEY"></script>
          <div id="container-$FIRST_AD_KEY"></div>
        </body></html>
        """.trimIndent()
    } else {
        """
        <!doctype html>
        <html><head>
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <style>html,body{margin:0;padding:0;width:100%;height:300px;overflow:hidden;background:transparent}body{display:flex;justify-content:center;align-items:flex-start}</style>
        </head><body>
          <script>
            var atOptions = {
              'key': '$SECOND_AD_KEY',
              'format': 'iframe',
              'height': 300,
              'width': 160,
              'params': {}
            };
          </script>
          <script src="https://bicea.org/22/$SECOND_AD_KEY"></script>
        </body></html>
        """.trimIndent()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = "Sponsored",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 5.dp)
        )
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (safeUnit == 1) 290.dp else 320.dp)
                .padding(top = 20.dp),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            // Keep ad rendering in this WebView; never launch unexpected external pages.
                            val host = request.url.host.orEmpty()
                            return request.isForMainFrame && host != "bicea.org"
                        }
                    }
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    loadDataWithBaseURL(
                        "https://bicea.org/",
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            update = {}
        )
    }
}
