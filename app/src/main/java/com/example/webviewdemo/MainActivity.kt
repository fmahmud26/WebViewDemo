package com.example.webviewdemo

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.webviewdemo.ui.theme.WebViewDemoTheme

class MainActivity : ComponentActivity() {
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WebViewDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    WebViewScreen { webViewInstance ->
                        webView = webViewInstance
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

@Composable
fun WebViewScreen(onWebViewCreated: (WebView) -> Unit) {
    val context = LocalContext.current
    val websiteUrl = "https://github.com/fmahmud26"
    val errorPageHtml = "file:///android_asset/offline.html"

    var loadURL by remember { mutableStateOf(websiteUrl) }
    val isLoading = remember { mutableStateOf(false) }

    MonitorNetworkState(context = context,
        onNetworkAvailable = { loadURL = websiteUrl },
        onNetworkLost = { loadURL = errorPageHtml })

    WebViewWithLoadingIndicator(loadURL = loadURL,
        isLoading = isLoading.value,
        onWebViewCreated = onWebViewCreated,
        onPageStarted = { isLoading.value = true },
        onPageFinished = { isLoading.value = false },
        onPageError = { loadURL = errorPageHtml })
}

@Composable
fun MonitorNetworkState(
    context: Context, onNetworkAvailable: () -> Unit, onNetworkLost: () -> Unit
) {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    DisposableEffect(Unit) {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onNetworkAvailable()
            override fun onLost(network: Network) = onNetworkLost()
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}

@Composable
fun WebViewWithLoadingIndicator(
    loadURL: String,
    isLoading: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
    onPageError: () -> Unit
) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.loadWithOverviewMode = true

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    onPageStarted()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    onPageFinished()
                }

                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    onPageError()
                }
            }

            loadUrl(loadURL)
            onWebViewCreated(this)
        }
    }, update = { it.loadUrl(loadURL) })

    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        )
    }
}
