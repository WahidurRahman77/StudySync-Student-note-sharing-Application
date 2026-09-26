package com.example.applicationdemo.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import java.net.URLDecoder
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    rawUrl: String,
    rawTitle: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val decodedUrl = remember(rawUrl) {
        try { URLDecoder.decode(rawUrl, "UTF-8") } catch (e: Exception) { rawUrl }
    }
    val decodedTitle = remember(rawTitle) {
        try { URLDecoder.decode(rawTitle, "UTF-8") } catch (e: Exception) { rawTitle }
    }

    val isImage = remember(decodedUrl) {
        val lower = decodedUrl.lowercase()
        lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") || lower.contains(".webp")
    }

    var isLoading by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = decodedTitle.ifEmpty { "Document Viewer" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isImage) "Image File" else "PDF Document",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isImage) {
                        IconButton(onClick = { webViewInstance?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(decodedUrl))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open External")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            if (isImage) {
                // Render image natively using Coil
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = decodedUrl,
                        contentDescription = decodedTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onLoading = { isLoading = true },
                        onSuccess = { isLoading = false },
                        onError = { isLoading = false }
                    )
                }
            } else {
                // Render PDF inside Webview via Google Docs Viewer Engine
                val formattedViewerUrl = remember(decodedUrl) {
                    if (decodedUrl.contains("drive.google.com") || decodedUrl.contains("docs.google.com")) {
                        decodedUrl
                    } else {
                        "https://docs.google.com/gview?embedded=true&url=${URLEncoder.encode(decodedUrl, "UTF-8")}"
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    if (newProgress >= 90) {
                                        isLoading = false
                                    }
                                }
                            }

                            loadUrl(formattedViewerUrl)
                            webViewInstance = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isLoading) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (isImage) "Loading Image..." else "Loading Document...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
