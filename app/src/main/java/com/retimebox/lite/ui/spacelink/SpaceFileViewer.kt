package com.retimebox.lite.ui.spacelink

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.retimebox.lite.data.local.entity.SpaceFileItem
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.data.local.entity.SpaceType
import com.retimebox.lite.util.FileHelper
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.IOException

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private class SpaceFileHttpServer(
    hostname: String,
    port: Int,
    private val context: Context
) : NanoHTTPD(hostname, port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        return if (uri.startsWith("/api/file")) {
            val params = session.parameters
            val pathList = params["path"]
            if (pathList.isNullOrEmpty()) {
                newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Bad Request")
            } else {
                val filePath = pathList[0]
                handleFileRequest(filePath)
            }
        } else if (uri.startsWith("/assets/")) {
            handleAssetRequest(uri.removePrefix("/assets/"))
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
    }

    private fun handleFileRequest(filePath: String): Response {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
        val mimeType = getMimeType(filePath)
        val inputStream = FileInputStream(file)
        val response = newChunkedResponse(Response.Status.OK, mimeType, inputStream)
        response.addHeader("Access-Control-Allow-Origin", "*")
        return response
    }

    private fun handleAssetRequest(assetPath: String): Response {
        return try {
            val inputStream = context.assets.open(assetPath)
            val mimeType = getMimeType(assetPath)
            val response = newChunkedResponse(Response.Status.OK, mimeType, inputStream)
            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
            response
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found: ${e.message}")
        }
    }

    private fun getMimeType(path: String): String {
        return when {
            path.endsWith(".html") -> "text/html; charset=utf-8"
            path.endsWith(".js") -> "application/javascript; charset=utf-8"
            path.endsWith(".css") -> "text/css; charset=utf-8"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".mp4") -> "video/mp4"
            path.endsWith(".ply") -> "application/octet-stream"
            path.endsWith(".sog") -> "application/octet-stream"
            path.endsWith(".json") -> "application/json; charset=utf-8"
            else -> "application/octet-stream"
        }
    }
}

private fun findFreePort(): Int {
    val server = java.net.ServerSocket()
    server.bind(null)
    val port = server.localPort
    server.close()
    return port
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SpaceFileViewer(
    spaceFileId: Long,
    onBack: () -> Unit,
    onOpenRecord: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as RetimeboxApplication
    val spaceFileRepository = app.spaceFileRepository

    var fileItem by remember { mutableStateOf<SpaceFileItem?>(null) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var pageProgress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var serverPort by remember { mutableStateOf<Int?>(null) }
    var httpServer by remember { mutableStateOf<SpaceFileHttpServer?>(null) }
    var viewerUrl by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(spaceFileId) {
        fileItem = spaceFileRepository.findById(spaceFileId)
        if (fileItem != null) {
            val port = findFreePort()
            serverPort = port
            val server = SpaceFileHttpServer("127.0.0.1", port, context.applicationContext)
            try {
                server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                httpServer = server
                val absPath = FileHelper.getSpaceFileAbsolutePath(context, fileItem!!.filePath)
                val fileName = File(absPath).name
                val fileUrl = "http://127.0.0.1:$port/api/file/$fileName?path=${Uri.encode(absPath)}"
                viewerUrl = if (fileItem!!.spaceType == SpaceType.GSPLAT) {
                    "http://127.0.0.1:$port/assets/gsviewer.html?content=${Uri.encode(fileUrl)}"
                } else {
                    "http://127.0.0.1:$port/assets/playviewer.html?type=${fileItem!!.spaceType.name}&url=$fileUrl"
                }
            } catch (e: IOException) {
                hasError = true
            }
        } else {
            hasError = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            httpServer?.stop()
            httpServer = null
            serverPort = null
            webViewRef.value?.apply {
                stopLoading()
                clearHistory()
                (parent as? ViewGroup)?.removeView(this)
                destroy()
            }
            webViewRef.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileItem?.name ?: "空间文件",
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val current = fileItem
                        if (current != null && current.sourceType == SourceType.FROM_RECORD_INDEX && current.bindRecordId != null && current.bindRecordId > 0) {
                            onOpenRecord(current.bindRecordId)
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("非索引条目没有笔记来源") }
                        }
                    }) {
                        Text("来源")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        text = "加载失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "无法启动本地服务器",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            if (viewerUrl.isNotEmpty() && !hasError) {
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
                                setMixedContentMode(2)
                                mediaPlaybackRequiresUserGesture = false
                                javaScriptCanOpenWindowsAutomatically = true
                                allowFileAccess = true
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
                        if (webViewInstance.url != viewerUrl && viewerUrl.isNotEmpty()) {
                            isLoading = true
                            hasError = false
                            pageProgress = 0
                            webViewInstance.loadUrl(viewerUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isLoading && !hasError && viewerUrl.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { pageProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = Color.White
                )
            }
        }
    }
}
