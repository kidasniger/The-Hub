package com.thehub.hb.ui.videoviewer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.loadOrCueVideo
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.utils.VideoLinkDetector
import com.thehub.hb.utils.VideoSourceType
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Lecteur vidéo plein écran — fond noir scénique, bouton fermer en verre,
 * YouTube natif ou WebView intégrée, erreurs de lecture en carte verre.
 */
@Composable
fun VideoViewerScreen(
    videoUrl: String,
    onClose: () -> Unit
) {
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val context = LocalContext.current
    val cleanedUrl = remember(videoUrl) { videoUrl.trim() }

    if (cleanedUrl.isBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = strings.lot5.videoInvalidLink,
                color = HubWhite
            )
        }
        return
    }

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
                YouTubePlayerContent(
                    videoId = videoId,
                    isShort = VideoLinkDetector.isYouTubeShorts(cleanedUrl),
                    onClose = onClose,
                    modifier = Modifier.fillMaxSize()
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

private enum class YouTubePlaybackPhase {
    LOADING,
    READY,
    PLAYING,
    PAUSED,
    BUFFERING,
    ENDED,
    ERROR
}

private class YouTubePlayerHost(context: Context) : FrameLayout(context) {
    var playerView: YouTubePlayerView? = null

    fun release(lifecycle: Lifecycle) {
        playerView?.let { player ->
            lifecycle.removeObserver(player)
            player.release()
        }
        playerView = null
        removeAllViews()
    }
}

@Composable
private fun YouTubePlayerContent(
    videoId: String,
    isShort: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle = lifecycleOwner.lifecycle

    val playerWidthRatio = if (isShort) 9f / 16f else 16f / 9f
    val playerHeightRatio = if (isShort) 16f / 9f else 9f / 16f

    var retryToken by remember(videoId, isShort) { mutableStateOf(0) }
    var phase by remember(videoId, isShort, retryToken) {
        mutableStateOf(YouTubePlaybackPhase.LOADING)
    }
    var errorName by remember(videoId, isShort, retryToken) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(videoId, isShort, retryToken) {
        phase = YouTubePlaybackPhase.LOADING
        errorName = null
        delay(15_000)
        if (
            phase == YouTubePlaybackPhase.LOADING ||
            phase == YouTubePlaybackPhase.READY ||
            phase == YouTubePlaybackPhase.BUFFERING
        ) {
            phase = YouTubePlaybackPhase.ERROR
            errorName = "PLAYER_TIMEOUT"
        }
    }

    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val playerWidth = minOf(
            maxWidth,
            maxHeight * playerWidthRatio
        )
        val playerHeight = playerWidth * playerHeightRatio

        Box(
            modifier = Modifier
                .width(playerWidth)
                .height(playerHeight)
                .background(Color.Black)
        ) {
            key(videoId, isShort, retryToken) {
                AndroidView(
                    factory = { androidContext ->
                        val host = YouTubePlayerHost(androidContext)

                        try {
                            val playerView = YouTubePlayerView(androidContext).apply {
                                enableAutomaticInitialization = false
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                host.playerView = this
                                lifecycle.addObserver(this)

                                val options = IFramePlayerOptions.Builder(androidContext)
                                    .origin(
                                        "https://" +
                                            androidContext.packageName.lowercase(Locale.ROOT)
                                    )
                                    .controls(1)
                                    .fullscreen(if (isShort) 0 else 1)
                                    .rel(0)
                                    .build()

                                initialize(
                                    object : AbstractYouTubePlayerListener() {
                                        override fun onReady(player: YouTubePlayer) {
                                            phase = YouTubePlaybackPhase.READY
                                            errorName = null
                                            player.loadOrCueVideo(lifecycle, videoId, 0f)
                                        }

                                        override fun onStateChange(
                                            player: YouTubePlayer,
                                            state: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState
                                        ) {
                                            phase = when (state.name) {
                                                "PLAYING" -> YouTubePlaybackPhase.PLAYING
                                                "PAUSED" -> YouTubePlaybackPhase.PAUSED
                                                "BUFFERING" -> YouTubePlaybackPhase.BUFFERING
                                                "ENDED" -> YouTubePlaybackPhase.ENDED
                                                "CUED", "UNSTARTED" ->
                                                    YouTubePlaybackPhase.READY
                                                else -> phase
                                            }
                                        }

                                        override fun onError(
                                            player: YouTubePlayer,
                                            error: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerError
                                        ) {
                                            errorName = error.name
                                            phase = YouTubePlaybackPhase.ERROR
                                            Log.e(
                                                "VideoViewerScreen",
                                                "YouTube playback error for " +
                                                    videoId +
                                                    ": " +
                                                    error.name
                                            )
                                        }
                                    },
                                    true,
                                    options
                                )
                            }

                            host.addView(
                                playerView,
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            )
                        } catch (exception: RuntimeException) {
                            Log.e(
                                "VideoViewerScreen",
                                "Unable to initialize YouTube player for $videoId",
                                exception
                            )
                            phase = YouTubePlaybackPhase.ERROR
                            errorName = "PLAYER_INITIALIZATION"
                        }

                        host
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { host ->
                        host.release(lifecycle)
                    }
                )
            }
        }

        if (
            phase == YouTubePlaybackPhase.LOADING ||
            phase == YouTubePlaybackPhase.READY ||
            phase == YouTubePlaybackPhase.BUFFERING
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = HubWhite,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        if (phase == YouTubePlaybackPhase.ERROR) {
            YouTubePlaybackError(
                errorName = errorName,
                onRetry = {
                    phase = YouTubePlaybackPhase.LOADING
                    errorName = null
                    retryToken++
                },
                onOpenExternally = {
                    openExternally(
                        context,
                        "https://www.youtube.com/watch?v=$videoId"
                    )
                }
            )
        }

        // Fermer — pastille verre (haut gauche).
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(palette.scrim)
                .border(1.dp, HubStroke, CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = strings.dialogClose,
                tint = HubWhite,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Erreur de lecture YouTube — carte verre + Réessayer / Ouvrir YouTube. */
@Composable
private fun YouTubePlaybackError(
    errorName: String?,
    onRetry: () -> Unit,
    onOpenExternally: () -> Unit
) {
    val strings = LocalHubStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xED000000)),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            shape = RoundedCornerShape(24.dp),
            variant = GlassVariant.Card,
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = youtubePlaybackErrorTitle(errorName, strings),
                    color = HubWhite,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )

                Text(
                    text = youtubePlaybackErrorDescription(errorName, strings),
                    color = HubSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                HubButton(
                    text = strings.lot3.retryLabel,
                    onClick = onRetry,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                HubButton(
                    text = strings.lot5.videoOpenYoutube,
                    onClick = onOpenExternally,
                    variant = HubButtonVariant.Secondary,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

private fun youtubePlaybackErrorTitle(
    errorName: String?,
    strings: com.thehub.hb.ui.theme.HubStrings
): String = when (errorName) {
    "VIDEO_NOT_FOUND" -> strings.lot5.videoErrNotFound
    "VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER",
    "VIDEO_NOT_ALLOWED_IN_EMBEDDED_PLAYER" -> strings.lot5.videoErrNotEmbeddable
    "HTML_5_PLAYER_ERROR" -> strings.lot5.videoErrEngine
    "INVALID_PARAMETER_IN_REQUEST" -> strings.lot5.videoErrInvalidLink
    "PLAYER_TIMEOUT" -> strings.lot5.videoErrTimeout
    "PLAYER_INITIALIZATION" -> strings.lot5.videoErrInit
    else -> strings.lot5.videoErrGeneric
}

private fun youtubePlaybackErrorDescription(
    errorName: String?,
    strings: com.thehub.hb.ui.theme.HubStrings
): String = when (errorName) {
    "VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER",
    "VIDEO_NOT_ALLOWED_IN_EMBEDDED_PLAYER" -> strings.lot5.videoErrNotEmbeddableDesc
    "PLAYER_TIMEOUT" -> strings.lot5.videoErrTimeoutDesc
    "PLAYER_INITIALIZATION" -> strings.lot5.videoErrInitDesc
    else -> strings.lot5.videoErrGenericDesc
}

@Composable
private fun GenericVideoViewer(
    videoUrl: String,
    onClose: () -> Unit
) {
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val context = LocalContext.current
    val cleanedUrl = remember(videoUrl) { videoUrl.trim() }
    val prepared = remember(cleanedUrl) {
        prepareGenericVideoUrl(cleanedUrl)
    }

    var isLoading by remember(prepared) { mutableStateOf(true) }
    var hasError by remember(prepared) { mutableStateOf(false) }
    var reloadToken by remember(prepared) { mutableStateOf(0) }

    LaunchedEffect(prepared.cacheKey, reloadToken) {
        isLoading = true
        hasError = false
        delay(15_000)
        if (isLoading) {
            isLoading = false
            hasError = true
        }
    }

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
                        CookieManager.getInstance().setAcceptThirdPartyCookies(
                            this,
                            prepared.sourceType == VideoSourceType.VIMEO ||
                                prepared.sourceType == VideoSourceType.DAILYMOTION
                        )

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(false)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            settings.safeBrowsingEnabled = true
                        }

                        if (prepared.sourceType == VideoSourceType.DIRECT_MEDIA) {
                            addJavascriptInterface(
                                VideoWebBridge(this) {
                                    isLoading = false
                                    hasError = true
                                },
                                "TheHubVideo"
                            )
                        }

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

                            override fun onReceivedHttpError(
                                view: WebView,
                                request: WebResourceRequest,
                                errorResponse: android.webkit.WebResourceResponse
                            ) {
                                if (
                                    request.isForMainFrame &&
                                    errorResponse.statusCode >= 400
                                ) {
                                    isLoading = false
                                    hasError = true
                                }
                                super.onReceivedHttpError(view, request, errorResponse)
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
                onRelease = { webView ->
                    webView.removeJavascriptInterface("TheHubVideo")
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.destroy()
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

        // Fermer — pastille verre (haut gauche).
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
                .size(44.dp)
                .clip(CircleShape)
                .background(palette.scrim)
                .border(1.dp, HubStroke, CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = strings.dialogClose,
                tint = HubWhite,
                modifier = Modifier.size(22.dp)
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
    url: String
): PreparedVideo {
    val cleaned = url.trim()
    val sourceType = VideoLinkDetector.sourceType(cleaned)

    return when (sourceType) {
        VideoSourceType.DIRECT_MEDIA -> PreparedVideo(
            url = buildDirectMediaHtml(cleaned),
            sourceType = sourceType,
            isInlineHtml = true
        )

        VideoSourceType.YOUTUBE -> PreparedVideo(
            url = cleaned,
            sourceType = sourceType
        )

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
          <video controls autoplay playsinline preload="metadata" onerror="window.TheHubVideo && window.TheHubVideo.onVideoError()">
            <source src="$safeUrl">
          </video>
        </body>
        </html>
    """.trimIndent()
}

private class VideoWebBridge(
    private val webView: WebView,
    private val onError: () -> Unit
) {
    @JavascriptInterface
    fun onVideoError() {
        webView.post(onError)
    }
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

/** Erreur de lecture générique — carte verre + Réessayer / Ouvrir le lien. */
@Composable
private fun VideoPlaybackError(
    sourceType: VideoSourceType,
    onRetry: () -> Unit,
    onOpenExternally: () -> Unit,
    showRetry: Boolean = true
) {
    val strings = LocalHubStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            shape = RoundedCornerShape(24.dp),
            variant = GlassVariant.Card,
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = when (sourceType) {
                        VideoSourceType.YOUTUBE -> strings.lot5.videoErrYoutubeTitle
                        VideoSourceType.DIRECT_MEDIA -> strings.lot5.videoErrDirectTitle
                        else -> strings.lot5.videoErrPlatformTitle
                    },
                    color = HubWhite,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )

                Text(
                    text = strings.lot5.videoErrActionDesc,
                    color = HubSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (showRetry) {
                    HubButton(
                        text = strings.lot3.retryLabel,
                        onClick = onRetry,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                HubButton(
                    text = strings.lot5.videoOpenLink,
                    onClick = onOpenExternally,
                    variant = HubButtonVariant.Secondary,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
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
