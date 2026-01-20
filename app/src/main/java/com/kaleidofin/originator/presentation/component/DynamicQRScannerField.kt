package com.kaleidofin.originator.presentation.component

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kaleidofin.originator.domain.model.FormField
import com.kaleidofin.originator.domain.model.QRConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun DynamicQRScannerField(
    field: FormField,
    error: String?,
    onQRScanned: (Map<String, String>) -> Unit, // Map<targetFieldId, value> -> Unit
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val qrConfig = field.qrConfig ?: return
    
    var showScanner by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScanner = true
        } else {
            scanError = "Camera permission denied"
        }
    }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Label
        Text(
            text = field.label + if (field.required) " *" else "",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Scan button
        Button(
            onClick = {
                if (isEnabled) {
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            showScanner = true
                        }
                        else -> {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
            },
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan QR Code")
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
        
        scanError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        // QR Scanner Dialog
        if (showScanner) {
            QRScannerDialog(
                qrConfig = qrConfig,
                onQRScanned = { prefillData ->
                    showScanner = false
                    scanError = null
                    onQRScanned(prefillData)
                },
                onDismiss = {
                    showScanner = false
                },
                onError = { errorMsg ->
                    scanError = errorMsg
                    showScanner = false
                }
            )
        }
    }
}

@Composable
private fun QRScannerDialog(
    qrConfig: QRConfig,
    onQRScanned: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    
    LaunchedEffect(Unit) {
        try {
            cameraProvider = suspendCancellableCoroutine { continuation ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    try {
                        continuation.resume(future.get())
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        } catch (e: Exception) {
            android.util.Log.e("QRScanner", "Failed to get camera provider", e)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan QR Code") },
        text = {
            Column {
                if (cameraProvider != null) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            
                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(ctx)
                            ) { imageProxy ->
                                processQRImage(imageProxy, qrConfig) { prefillData ->
                                    imageProxy.close()
                                    onQRScanned(prefillData)
                                }
                            }
                            
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            
                            try {
                                cameraProvider?.unbindAll()
                                cameraProvider?.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                onError("Failed to start camera: ${e.message}")
                            }
                            
                            previewView
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun processQRImage(
    imageProxy: androidx.camera.core.ImageProxy,
    qrConfig: QRConfig,
    onSuccess: (Map<String, String>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    
    val image = InputImage.fromMediaImage(
        mediaImage,
        imageProxy.imageInfo.rotationDegrees
    )
    
    val scanner = BarcodeScanning.getClient()
    
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            if (barcodes.isNotEmpty()) {
                val barcode = barcodes[0]
                val rawValue = barcode.rawValue ?: ""
                
                if (qrConfig.format == "JSON") {
                    try {
                        val jsonObject = JSONObject(rawValue)
                        val prefillData = mutableMapOf<String, String>()
                        
                        qrConfig.prefillMapping.forEach { mapping ->
                            if (jsonObject.has(mapping.qrKey)) {
                                val value = jsonObject.optString(mapping.qrKey, "")
                                if (value.isNotBlank()) {
                                    prefillData[mapping.targetFieldId] = value
                                }
                            }
                        }
                        
                        if (prefillData.isNotEmpty()) {
                            onSuccess(prefillData)
                        } else {
                            // No matching keys found, but parsing succeeded
                            imageProxy.close()
                        }
                    } catch (e: Exception) {
                        // JSON parsing failed
                        imageProxy.close()
                    }
                } else {
                    // Unsupported format
                    imageProxy.close()
                }
            } else {
                imageProxy.close()
            }
        }
        .addOnFailureListener {
            imageProxy.close()
        }
}
