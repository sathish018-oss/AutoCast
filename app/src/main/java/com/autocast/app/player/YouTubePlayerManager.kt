package com.autocast.app.player

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class YouTubePlayerManager(private val context: Context) {

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun createYouTubePlayerWebView(): WebView {
        val wv = WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                allowFileAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                }
            }
            webChromeClient = WebChromeClient()
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; overflow: hidden; font-family: sans-serif; }
                    #player { width: 100%; height: 100%; position: absolute; top: 0; left: 0; }
                    .controls-overlay {
                        position: absolute; bottom: 10px; left: 50%; transform: translateX(-50%);
                        display: flex; gap: 12px; background: rgba(0,0,0,0.7); padding: 8px 16px; border-radius: 24px; z-index: 10;
                    }
                    .btn { color: #fff; background: #e50914; border: none; padding: 8px 14px; border-radius: 16px; font-weight: bold; cursor: pointer; }
                </style>
            </head>
            <body>
                <div id="player"></div>
                <script src="https://www.youtube.com/iframe_api"></script>
                <script>
                    var player;
                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            videoId: 'dQw4w9WgXcQ',
                            playerVars: {
                                'playsinline': 1,
                                'autoplay': 1,
                                'controls': 1,
                                'rel': 0,
                                'modestbranding': 1
                            }
                        });
                    }
                    function loadVideo(videoId) {
                        if (player && player.loadVideoById) {
                            player.loadVideoById(videoId);
                        }
                    }
                    function playVideo() { if (player && player.playVideo) player.playVideo(); }
                    function pauseVideo() { if (player && player.pauseVideo) player.pauseVideo(); }
                    function seekRelative(seconds) {
                        if (player && player.getCurrentTime) {
                            var curr = player.getCurrentTime();
                            player.seekTo(curr + seconds, true);
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        wv.loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
        this.webView = wv
        return wv
    }

    fun loadVideo(videoId: String) {
        webView?.evaluateJavascript("loadVideo('$videoId');", null)
    }

    fun play() {
        webView?.evaluateJavascript("playVideo();", null)
    }

    fun pause() {
        webView?.evaluateJavascript("pauseVideo();", null)
    }

    fun seekForward() {
        webView?.evaluateJavascript("seekRelative(10);", null)
    }

    fun seekRewind() {
        webView?.evaluateJavascript("seekRelative(-10);", null)
    }
}
