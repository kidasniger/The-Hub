package com.thehub.hb.ui.videoviewer

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.thehub.hb.ui.theme.HubBackground
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.utils.VideoLinkDetector
import com.thehub.hb.utils.VideoSourceType
import java.util.Locale

@Composable
fun VideoViewerScreen(
    videoUrl: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val cleanedUrl = remember(videoUrl) { videoUrl.trim() }
    val sourceType = remember(cleanedUrl) {
        VideoLinkDetector.sourceType(cleanedUrl)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (sourceType == VideoSourceType.YOUTUBE) {
            val videoId = remember(cleanedUrl) {
                VideoLinkDetector.youtubeVideoId(cleanedUrl)
            }

            if (videoId.isNullOrBlank()) {
                VideoPlaybackError(
                    sourceType = sourceType,
                    onRetry = { },
                    onOpenExternally = { openExternally(context, cleanedUrl) },
                    showRetry = false
                )
            } else {
                val isShort = VideoLinkDetector.isYouTubeShorts(cleanedUrl)

                YouTubePlayerContent(
                    videoId = videoId,
                    isShort = isShort,
                    modifier = Modifier.fillMaxSize()
                )

                // Immersive app chrome: the player owns the video controls while
                // The Hub only adds a lightweight close action and source badge.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                Color.Black.copy(alpha = 0.58f),
                                CircleShape
                            )
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = HubWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(
                                Color.Black.copy(alpha = 0.48f),
                                CircleShape
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (isShort) "YouTube Shorts" else "YouTube",
                            color = HubWhite,
                            fontSize = 12.sp
                        )
                    }
                }

                // Subtle top/bottom scrims keep the controls and close button
                // legible without covering the actual video content.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.50f)
                                )
                            )
                        )
                )
            }
        } else {
            GenericVideoViewer(
                videoUrl = cleanedUrl,
                onClose = onClose
            )
        }
    }
}

@Composable
private fun YouTubePlayerContent(
    videoId: String,
    isShort: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val playerView = remember(videoId, context, isShort) {
        YouTubePlayerView(context).apply {
            enableAutomaticInitialization = false
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            if (isShort) {
                matchParent()
            } else {
                wrapContent()
            }
        }
    }

    DisposableEffect(lifecycleOwner, playerView, videoId) {
        lifecycleOwner.lifecycle.addObserver(playerView)

        val options = IFramePlayerOptions.Builder(context)
            .origin("https://" + context.packageName.lowercase(Locale.ROOT))
            .controls(1)
            .fullscreen(if (isShort) 0 else 1)
            .rel(0)
            .build()

        playerView.initialize(
            object : AbstractYouTubePlayerListener() {
                override fun onReady(player: YouTubePlayer) {
                    player.loadVideo(videoId, 0f)
                }
            },
            true,
            options
        )

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(playerView)
            playerView.release()
        }
    }

    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val playerWidth = if (isShort) {
            minOf(
                maxWidth,
                maxHeight * (9f / 16f)
            )
        } else {
            minOf(
                maxWidth,
                maxHeight * (16f / 9f)
            )
        }

        val playerHeight = if (isShort) {
            playerWidth * (16f / 9f)
        } else {
            playerWidth * (9f / 16f)
        }

        Box(
            modifier = Modifier
                .width(playerWidth)
                .height(playerHeight)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { playerView },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun GenericVideoViewer(
    videoUrl: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val cleanedUrl = remember(videoUrl) { videoUrl.trim() }
    val prepared = remember(cleanedUrl, context.packageName) {
        prepareGenericVideoUrl(cleanedUrl, context.packageName)
    }

    var isLoading by remember(prepared) { mutableStateOf(true) }
    var hasError by remember(prepared) { mutableStateOf(false) }
    var reloadToken by remember(prepared) { mutableStateOf(0) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        key(prepared.cacheKey, reloadToken) {
            AndroidView(
                factory = { androidContext ->
                    WebView(androidContext).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(false)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView,
                                url: String,
                                favicon: android.graphics.Bitmap?
                            ) {
                                isLoading = true
                                hasError = false
                                super.onPageStarted(view, url, favicon)
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                isLoading = false
                                super.onPageFinished(view, url)
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                if (request.isForMainFrame) {
                                    isLoading = false
                                    hasError = true
                                }
                                super.onReceivedError(view, request, error)
                            }
                        }

                        webChromeClient = WebChromeClient()
                        loadPreparedVideo(prepared)
                    }
                },
                update = { webView ->
                    if (webView.url != prepared.url && reloadToken == 0) {
                        webView.loadPreparedVideo(prepared)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isLoading && !hasError) {
            CircularProgressIndicator(
                color = HubWhite,
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.Center)
            )
        }

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

        if (hasError) {
            VideoPlaybackError(
                sourceType = prepared.sourceType,
                onRetry = {
                    hasError = false
                    isLoading = true
                    reloadToken++
                },
                onOpenExternally = {
                    openExternally(context, cleanedUrl)
                }
            )
        }
    }
}

private data class PreparedVideo(
    val url: String,
    val sourceType: VideoSourceType,
    val additionalHeaders: Map<String, String> = emptyMap(),
    val isInlineHtml: Boolean = false
) {
    val cacheKey: String
        get() = url + additionalHeaders.entries.sortedBy { it.key }
            .joinToString(separator = "&") { entry -> entry.key + "=" + entry.value }
}

private fun prepareGenericVideoUrl(
    url: String,
    packageName: String
): PreparedVideo {
    val cleaned = url.trim()
    val sourceType = VideoLinkDetector.sourceType(cleaned)

    return when (sourceType) {
        VideoSourceType.DIRECT_MEDIA -> PreparedVideo(
            url = buildDirectMediaHtml(cleaned),
            sourceType = sourceType,
            isInlineHtml = true
        )

        VideoSourceType.YOUTUBE -> {
            val id = VideoLinkDetector.youtubeVideoId(cleaned)
            if (id.isNullOrBlank()) {
                PreparedVideo(cleaned, sourceType)
            } else {
                val appReferrer = "https://" + packageName.lowercase(Locale.ROOT)
                PreparedVideo(
                    url = buildYouTubeEmbedUrl(
                        videoId = id,
                        appReferrer = appReferrer
                    ),
                    sourceType = sourceType,
                    additionalHeaders = mapOf("Referer" to appReferrer)
                )
            }
        }

        VideoSourceType.VIMEO -> {
            val id = Uri.parse(cleaned).pathSegments.lastOrNull { it.all(Char::isDigit) }
            if (id.isNullOrBlank()) {
                PreparedVideo(cleaned, sourceType)
            } else {
                PreparedVideo(
                    url = buildEmbedHtml(
                        iframeUrl = "https://player.vimeo.com/video/$id?autoplay=1",
                        title = "Vimeo"
                    ),
                    sourceType = sourceType,
                    isInlineHtml = true
                )
            }
        }

        VideoSourceType.DAILYMOTION -> {
            val id = Uri.parse(cleaned).pathSegments
                .firstOrNull { it.isNotBlank() && it != "video" }
            if (id.isNullOrBlank()) {
                PreparedVideo(cleaned, sourceType)
            } else {
                PreparedVideo(
                    url = buildEmbedHtml(
                        iframeUrl = "https://www.dailymotion.com/embed/video/$id?autoplay=1",
                        title = "Dailymotion"
                    ),
                    sourceType = sourceType,
                    isInlineHtml = true
                )
            }
        }

        VideoSourceType.EXTERNAL_VIDEO_PAGE -> PreparedVideo(
            url = cleaned,
            sourceType = sourceType
        )
    }
}

private fun buildYouTubeEmbedUrl(
    videoId: String,
    appReferrer: String
): String {
    return Uri.Builder()
        .scheme("https")
        .authority("www.youtube.com")
        .appendPath("embed")
        .appendPath(videoId)
        .appendQueryParameter("autoplay", "1")
        .appendQueryParameter("rel", "0")
        .appendQueryParameter("playsinline", "1")
        .appendQueryParameter("origin", appReferrer)
        .appendQueryParameter("widget_referrer", appReferrer)
        .build()
        .toString()
}

private fun buildEmbedHtml(
    iframeUrl: String,
    title: String
): String {
    val safeIframeUrl = TextUtils.htmlEncode(iframeUrl)
    val safeTitle = TextUtils.htmlEncode(title)

    return """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            html, body {
              margin: 0;
              padding: 0;
              width: 100%;
              height: 100%;
              background: #000;
              overflow: hidden;
            }
            body {
              display: flex;
              align-items: center;
              justify-content: center;
            }
            iframe {
              width: 100%;
              height: 100%;
              border: 0;
              background: #000;
            }
          </style>
        </head>
        <body>
          <iframe
            src="$safeIframeUrl"
            title="$safeTitle"
            allow="autoplay; encrypted-media; fullscreen; picture-in-picture"
            referrerpolicy="strict-origin-when-cross-origin"
            allowfullscreen>
          </iframe>
        </body>
        </html>
    """.trimIndent()
}

private fun buildDirectMediaHtml(mediaUrl: String): String {
    val safeUrl = TextUtils.htmlEncode(mediaUrl)

    return """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            html, body {
              margin: 0;
              padding: 0;
              width: 100%;
              height: 100%;
              background: #000;
              overflow: hidden;
            }
            body {
              display: flex;
              align-items: center;
              justify-content: center;
            }
            video {
              width: 100%;
              height: 100%;
              max-height: 100vh;
              background: #000;
              object-fit: contain;
            }
          </style>
        </head>
        <body>
          <video controls autoplay playsinline preload="metadata">
            <source src="$safeUrl">
          </video>
        </body>
        </html>
    """.trimIndent()
}

private fun WebView.loadPreparedVideo(prepared: PreparedVideo) {
    if (prepared.isInlineHtml) {
        loadDataWithBaseURL(
            "https://" + context.packageName.lowercase(Locale.ROOT) + "/",
            prepared.url,
            "text/html",
            "UTF-8",
            null
        )
    } else if (prepared.additionalHeaders.isEmpty()) {
        loadUrl(prepared.url)
    } else {
        loadUrl(prepared.url, prepared.additionalHeaders)
    }
}

@Composable
private fun VideoPlaybackError(
    sourceType: VideoSourceType,
    onRetry: () -> Unit,
    onOpenExternally: () -> Unit,
    showRetry: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = when (sourceType) {
                    VideoSourceType.YOUTUBE -> "Impossible de lire cette vidéo YouTube ici."
                    VideoSourceType.DIRECT_MEDIA -> "Ce fichier vidéo ne peut pas être lu ici."
                    else -> "Cette plateforme ne permet pas la lecture intégrée."
                },
                color = HubWhite
            )

            Text(
                text = "Tu peux réessayer ou ouvrir le lien dans l'application / le navigateur compatible.",
                color = HubSecondary
            )

            if (showRetry) {
                Button(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(" Réessayer")
                }
            }

            Button(onClick = onOpenExternally) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(" Ouvrir le lien")
            }
        }
    }
}

private fun openExternally(
    context: Context,
    url: String
) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // No compatible external handler installed.
    }
}
