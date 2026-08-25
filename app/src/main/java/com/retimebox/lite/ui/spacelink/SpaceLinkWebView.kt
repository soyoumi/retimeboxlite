package com.retimebox.lite.ui.spacelink

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.retimebox.lite.R
import com.retimebox.lite.RetimeboxApplication
import com.retimebox.lite.data.local.entity.SpaceLinkItem
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SpaceLinkWebView(
    spaceLinkId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val app = context.applicationContext as RetimeboxApplication
    val spaceLinkRepository = app.spaceLinkRepository

    var linkItem by remember { mutableStateOf<SpaceLinkItem?>(null) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(spaceLinkId) {
        linkItem = spaceLinkRepository.findById(spaceLinkId)
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.let {
                if (isFullscreen) {
                    it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    @Suppress("DEPRECATION")
                    it.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
            webViewRef.value?.apply {
                stopLoading()
                clearHistory()
                (parent as? ViewGroup)?.removeView(this)
                destroy()
            }
            webViewRef.value = null
        }
    }

    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        activity?.let {
            if (isFullscreen) {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                @Suppress("DEPRECATION")
                it.window?.decorView?.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            } else {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                @Suppress("DEPRECATION")
                it.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    fun reloadPage() {
        hasError = false
        isLoading = true
        val url = linkItem?.webUrl
        val wv = webViewRef.value
        if (wv != null && url != null) {
            wv.post {
                wv.loadUrl(url)
            }
        }
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(linkItem?.name ?: stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        TextButton(onClick = { reloadPage() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else innerPadding)
                .background(Color.Black)
        ) {
            if (hasError) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.webview_load_error),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = linkItem?.webUrl ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    TextButton(onClick = { reloadPage() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(AndroidColor.BLACK)
                        isFocusable = true
                        isFocusableInTouchMode = true
                        requestFocus()

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            setMixedContentMode(0)
                            mediaPlaybackRequiresUserGesture = false
                            javaScriptCanOpenWindowsAutomatically = true
                            allowFileAccess = false
                            allowContentAccess = true
                            setSupportMultipleWindows(true)
                            cacheMode = WebSettings.LOAD_DEFAULT
                            databaseEnabled = true
                            setInitialScale(100)
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageProgress = newProgress
                                isLoading = newProgress < 100
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                hasError = false
                                super.onPageFinished(view, url)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                if (errorCode != 0) {
                                    hasError = true
                                    isLoading = false
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                url: String?
                            ): Boolean {
                                return false
                            }
                        }

                        webViewRef.value = this
                    }
                },
                update = { webViewInstance ->
                    webViewRef.value = webViewInstance
                    val url = linkItem?.webUrl
                    if (url != null && webViewInstance.url != url) {
                        isLoading = true
                        hasError = false
                        pageProgress = 0
                        webViewInstance.loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading && !hasError) {
                LinearProgressIndicator(
                    progress = { pageProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = Color.White
                )
            }

            if (isFullscreen) {
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = { toggleFullscreen() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 48.dp, end = 16.dp)
                    ) {
                        Icon(
                            Icons.Filled.FullscreenExit,
                            contentDescription = "退出全屏",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 48.dp, start = 16.dp)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
