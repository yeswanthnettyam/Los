package com.kaleidofin.originator.presentation.component

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kaleidofin.originator.domain.model.FormField
import kotlinx.coroutines.launch

@Composable
fun DynamicWebViewLaunchField(
    field: FormField,
    error: String?,
    onLaunchWebView: (String) -> Unit, // URL -> Unit
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    getUrlFromApi: suspend () -> String? // API call to get URL
) {
    val webViewConfig = field.webViewConfig ?: return
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var apiError by remember { mutableStateOf<String?>(null) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Label
        Text(
            text = field.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Launch button
        Button(
            onClick = {
                if (isEnabled && !isLoading) {
                    when (webViewConfig.urlSource) {
                        "STATIC" -> {
                            webViewConfig.staticUrl?.let { url ->
                                onLaunchWebView(url)
                            } ?: run {
                                apiError = "Static URL not configured"
                            }
                        }
                        "API" -> {
                            coroutineScope.launch {
                                isLoading = true
                                apiError = null
                                
                                try {
                                    val url = getUrlFromApi()
                                    if (url != null) {
                                        onLaunchWebView(url)
                                    } else {
                                        apiError = "Failed to get URL from API"
                                    }
                                } catch (e: Exception) {
                                    apiError = "Error: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                        else -> {
                            apiError = "Invalid urlSource: ${webViewConfig.urlSource}"
                        }
                    }
                }
            },
            enabled = isEnabled && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Loading...")
            } else {
                Text(field.placeholder ?: "Open ${field.label}")
            }
        }
        
        // Error messages
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        apiError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
