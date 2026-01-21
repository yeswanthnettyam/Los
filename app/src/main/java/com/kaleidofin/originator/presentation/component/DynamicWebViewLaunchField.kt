package com.kaleidofin.originator.presentation.component

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    val webViewConfig = field.webViewConfig
    if (webViewConfig == null) {
        // Render error state instead of returning early
        return@DynamicWebViewLaunchField Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = field.label.takeIf { it.isNotBlank() } ?: "External Process",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Error: WebView configuration is missing",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var apiError by remember { mutableStateOf<String?>(null) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Label - use field.label
        Text(
            text = field.label + if (field.required) " *" else "",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Launch button - Simple button style matching QR scanner
        Button(
            onClick = {
                if (isEnabled && !isLoading) {
                    when (webViewConfig.urlSource) {
                        "STATIC" -> {
                            val url = webViewConfig.staticUrl
                            if (url != null && url.isNotBlank()) {
                                onLaunchWebView(url)
                            } else {
                                apiError = "Static URL not configured"
                            }
                        }
                        "API" -> {
                            // Validate that launchApi is configured
                            if (webViewConfig.launchApi == null) {
                                apiError = "API configuration is missing. Please contact support."
                                return@Button
                            }
                            
                            coroutineScope.launch {
                                isLoading = true
                                showProgressDialog = true
                                apiError = null
                                
                                try {
                                    val url = getUrlFromApi()
                                    
                                    if (url != null && url.isNotBlank()) {
                                        showProgressDialog = false
                                        isLoading = false
                                        onLaunchWebView(url)
                                    } else {
                                        showProgressDialog = false
                                        apiError = "Unable to open link. Please try again."
                                        isLoading = false
                                    }
                                } catch (e: Exception) {
                                    showProgressDialog = false
                                    apiError = "Unable to open link. Please try again."
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
            Text("Proceed")
        }
        
        // Progress Dialog
        if (showProgressDialog) {
            AlertDialog(
                onDismissRequest = {
                    // Don't allow dismissing during API call
                },
                title = {
                    Text("Loading...")
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text("Please wait while we prepare the link...")
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }
        
        // Error messages
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        apiError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
