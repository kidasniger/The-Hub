package com.thehub.hb.ui.videoviewer

import android.annotation.SuppressLint
import android.text.TextUtils
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.thehub.hb.ui.theme.HubBackground
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.utils.VideoLinkDetector

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoViewerScreen(
    videoUrl: String,
    onClose: () -> Unit
) {
    val preparedUrl = remember(videoUrl) { prepareVideoUrl(videoUrl) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HubBackground)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            return false
                        }
                    }
                    webChromeClient = WebChromeClient()
                    loadPreparedUrl(preparedUrl, context.packageName)
                }
            },
            update = { webView ->
                if (webView.url != preparedUrl) {
                    webView.loadPreparedUrl(preparedUrl, webView.context.packageName)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
                .size(44.dp)
                .background(Color(0x99000000), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fermer",
                tint = HubWhite,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun WebView.loadPreparedUrl(url: String, packageName: String) {
    if (url.startsWith("data:text/html")) {
        loadDataWithBaseURL(
            null,
            url.removePrefix("data:text/html,"),
            "text/html",
            "UTF-8",
            "https://$packageName"
        )
    } else {
        // YouTube error 153 occurs when the embedded player receives no HTTP Referer.
        // Android WebView does not provide one by default. YouTube documents
        // android-app://<package> as the app identity for this WebView integration.
        val headers = if (url.contains("youtube.com/embed/")) {
            mapOf("Referer" to "android-app://$packageName")
        } else {
            emptyMap()
        }
        loadUrl(url, headers)
    }
}

private fun prepareVideoUrl(url: String): String {
    val cleaned = url.trim()
    if (VideoLinkDetector.isDirectMediaUrl(cleaned)) {
        val safeUrl = TextUtils.htmlEncode(cleaned)
        val html = """
            <!doctype html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
              <style>
                html, body { margin:0; padding:0; width:100%; height:100%; background:#000; }
                body { display:flex; align-items:center; justify-content:center; }
                video { width:100%; height:100%; max-height:100vh; background:#000; }
              </style>
            </head>
            <body>
              <video controls autoplay playsinline preload="metadata">
                <source src="$safeUrl">
              </video>
            </body>
            </html>
        """.trimIndent()
        return "data:text/html," + html
    }

    val uri = android.net.Uri.parse(cleaned)
    val host = uri.host?.lowercase().orEmpty()

    if (host == "youtube.com" || host == "www.youtube.com" || host == "m.youtube.com" || host == "youtu.be" || host.endsWith(".youtube.com")) {
        val id = extractYouTubeId(uri)
        if (!id.isNullOrBlank()) {
            return "https://www.youtube.com/embed/$id?autoplay=1&rel=0&playsinline=1"
        }
    }

    if (host.contains("vimeo.com")) {
        val id = uri.pathSegments.lastOrNull { it.all(Char::isDigit) }
        if (!id.isNullOrBlank()) {
            return "https://player.vimeo.com/video/$id?autoplay=1"
        }
    }

    if (host == "dailymotion.com" || host == "www.dailymotion.com" || host == "dai.ly") {
        val id = uri.pathSegments.firstOrNull { it.isNotBlank() && it != "video" }
        if (!id.isNullOrBlank()) {
            return "https://www.dailymotion.com/embed/video/$id"
        }
    }

    return cleaned
}

private fun extractYouTubeId(uri: android.net.Uri): String? {
    if (uri.host?.lowercase() == "youtu.be") {
        return uri.pathSegments.firstOrNull()
    }

    uri.getQueryParameter("v")?.takeIf { it.isNotBlank() }?.let { return it }

    val segments = uri.pathSegments
    val markerIndex = segments.indexOfFirst { it == "shorts" || it == "embed" }
    return if (markerIndex >= 0) segments.getOrNull(markerIndex + 1) else null
}
