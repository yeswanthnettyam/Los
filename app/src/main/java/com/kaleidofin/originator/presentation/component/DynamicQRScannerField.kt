package com.kaleidofin.originator.presentation.component

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import android.content.Context
import android.widget.Toast
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import com.kaleidofin.originator.domain.model.FormField
import com.kaleidofin.originator.domain.model.QRConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

import androidx.navigation.NavController
import com.kaleidofin.originator.presentation.navigation.Screen
import com.google.gson.Gson

@Composable
fun DynamicQRScannerField(
    field: FormField,
    error: String?,
    onQRScanned: (Map<String, String>) -> Unit, // Map<targetFieldId, value> -> Unit
    onAadhaarQRDetected: ((ByteArray) -> Unit)? = null, // Callback for Aadhaar QR binary payload
    navController: NavController,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val context = LocalContext.current
    val qrConfig = field.qrConfig ?: return
    
    var scanError by remember { mutableStateOf<String?>(null) }
    
    // Listen for QR scan results from SavedStateHandle
    LaunchedEffect(Unit) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        savedStateHandle?.get<Map<String, String>>("qrScannedData")?.let { prefillData ->
            savedStateHandle.remove<Map<String, String>>("qrScannedData")
            scanError = null
            onQRScanned(prefillData)
        }
        
        savedStateHandle?.get<ByteArray>("aadhaarQRData")?.let { rawBytes ->
            savedStateHandle.remove<ByteArray>("aadhaarQRData")
            scanError = null
            onAadhaarQRDetected?.invoke(rawBytes)
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Navigate to QR Scanner screen
            val gson = Gson()
            val configJson = gson.toJson(qrConfig)
            navController.navigate(Screen.QRScanner.createRoute(configJson))
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
                            // Navigate to QR Scanner screen
                            val gson = Gson()
                            val configJson = gson.toJson(qrConfig)
                            navController.navigate(Screen.QRScanner.createRoute(configJson))
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
    }
}

@Composable
private fun QRScannerDialog(
    qrConfig: QRConfig,
    onQRScanned: (Map<String, String>) -> Unit,
    onAadhaarQRDetected: ((ByteArray) -> Unit)? = null,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageAnalysisRef by remember { mutableStateOf<ImageAnalysis?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    val lastProcessTime = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val isQrProcessed = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    
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
            onError("Failed to initialize camera: ${e.message}")
        }
    }
    
    // Cleanup on dismiss
    LaunchedEffect(Unit) {
        // This will run when the composable is disposed
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Clean up camera resources when dialog is dismissed
            imageAnalysisRef?.clearAnalyzer()
            cameraProvider?.unbindAll()
        }
    }
    
    AlertDialog(
        onDismissRequest = {
            imageAnalysisRef?.clearAnalyzer()
            cameraProvider?.unbindAll()
            onDismiss()
        },
        title = { Text("Scan QR Code") },
        text = {
            Column {
                if (cameraProvider != null && isScanning) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            
                            // ============================================================
                            // HIGH-RESOLUTION ANALYSIS FOR AADHAAR SECURE QR
                            // ============================================================
                            // WHY: Aadhaar Secure QR codes are extremely dense and small.
                            //      Require ≥1080p (1920x1080) resolution for reliable detection.
                            //      Lower resolutions (720p/480p) lack sufficient pixel density.
                            // ============================================================
                            val resolutionSelector = ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        android.util.Size(1920, 1080), // Target 1080p minimum
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                    )
                                )
                                .build()
                            
                            // Configure ImageAnalysis with high resolution for dense Aadhaar QR codes
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setResolutionSelector(resolutionSelector)
                                .build()
                            
                            android.util.Log.d("QRScanner", "ImageAnalysis configured: target=1920x1080 (1080p), strategy=KEEP_ONLY_LATEST")
                            
                            imageAnalysisRef = imageAnalysis
                            
                            // Use background executor for analyzer to avoid blocking main thread
                            // ML Kit processing is CPU-intensive and should run off main thread
                            val analyzerExecutor = Executors.newSingleThreadExecutor()
                            
                            imageAnalysis.setAnalyzer(
                                analyzerExecutor
                            ) { imageProxy ->
                                // Check if QR has already been processed (prevent duplicate scans)
                                if (!isScanning || isQrProcessed.get()) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                
                                // Throttle processing: process every ~300ms to avoid overwhelming ML Kit
                                // Critical for high-density Aadhaar QR codes that require careful analysis
                                val currentTime = System.currentTimeMillis()
                                val lastTime = lastProcessTime.get()
                                if (currentTime - lastTime >= 300) {
                                    lastProcessTime.set(currentTime)
                                    processQRImage(
                                        imageProxy = imageProxy,
                                        qrConfig = qrConfig,
                                        context = ctx,
                                        isQrProcessed = isQrProcessed,
                                        onSuccess = { prefillData ->
                                            // Stop scanning after successful detection
                                            isScanning = false
                                            imageAnalysis.clearAnalyzer()
                                            analyzerExecutor.shutdown()
                                            onQRScanned(prefillData)
                                        },
                                        onAadhaarQRDetected = { rawBytes ->
                                            // Mark as processed and stop analysis immediately
                                            isQrProcessed.set(true)
                                            isScanning = false
                                            imageAnalysis.clearAnalyzer()
                                            analyzerExecutor.shutdown()
                                            onAadhaarQRDetected?.invoke(rawBytes)
                                        }
                                    )
                                } else {
                                    // Skip frame if processing too frequently
                                    imageProxy.close()
                                }
                            }
                            
                            // Explicitly use BACK camera for consistent Aadhaar QR scanning
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            
                            android.util.Log.d("QRScanner", "Camera selector: BACK camera")
                            
                            try {
                                cameraProvider?.unbindAll()
                                cameraProvider?.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("QRScanner", "Failed to bind camera", e)
                                onError("Failed to start camera: ${e.message}")
                            }
                            
                            previewView
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                } else if (!isScanning) {
                    Text("QR Code scanned successfully!")
                } else {
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                imageAnalysisRef?.clearAnalyzer()
                cameraProvider?.unbindAll()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

private fun processQRImage(
    imageProxy: androidx.camera.core.ImageProxy,
    qrConfig: QRConfig,
    context: Context,
    isQrProcessed: java.util.concurrent.atomic.AtomicBoolean,
    onSuccess: (Map<String, String>) -> Unit,
    onAadhaarQRDetected: ((ByteArray) -> Unit)? = null
) {
    // Early return if QR has already been processed
    if (isQrProcessed.get()) {
        imageProxy.close()
        return
    }
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        android.util.Log.w("QRScanner", "Frame received but mediaImage is null")
        imageProxy.close()
        return
    }
    
    // Extract frame metadata for diagnostic logging
    val width = mediaImage.width
    val height = mediaImage.height
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    
    // Production-safe diagnostic log: actual frame resolution received
    // This confirms if ResolutionSelector successfully obtained ≥1080p resolution
    val resolutionLabel = if (width >= 1920 && height >= 1080) {
        "≥1080p"
    } else {
        "${String.format("%.1f", width * height / 1_000_000.0)}MP"
    }
    android.util.Log.d("QRScanner", "Frame received: actual=${width}x${height} ($resolutionLabel), rotation=${rotationDegrees}°")
    
    // ============================================================
    // ML KIT QR-ONLY CONFIGURATION WITH DENSE BARCODE SUPPORT
    // ============================================================
    // WHY: Force QR-only detection to improve accuracy for dense Aadhaar QR codes.
    //      Scanning all barcode formats reduces detection reliability.
    //      Dense barcode support improves detection of high-density QR codes.
    // ============================================================
    val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .enableAllPotentialBarcodes() // Enable dense barcode detection for Aadhaar Secure QR
        .build()
    
    val scanner = BarcodeScanning.getClient(scannerOptions)
    
    // ============================================================
    // INPUT IMAGE CREATION
    // ============================================================
    // WHY: Use ImageProxy.image directly (fromMediaImage) to preserve maximum quality.
    //      DO NOT convert to Bitmap, resize, compress, or crop - this degrades
    //      detection accuracy for high-density Aadhaar QR codes.
    // ============================================================
    // Rotation: imageProxy.imageInfo.rotationDegrees is already correct for ML Kit.
    // CameraX calculates this relative to device orientation automatically.
    val inputImage = InputImage.fromMediaImage(
        mediaImage,
        rotationDegrees
    )
    
    // Process image with ML Kit (runs asynchronously)
    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            // Early return if QR has already been processed
            if (isQrProcessed.get()) {
                imageProxy.close()
                return@addOnSuccessListener
            }
            
            // Log total barcodes detected for diagnostics
            val barcodeCount = barcodes.size
            android.util.Log.d("QRScanner", "ML Kit result: ${barcodeCount} barcode(s) detected")
            
            if (barcodes.isNotEmpty()) {
                // ============================================================
                // FIND FIRST VALID AADHAAR SECURE QR
                // ============================================================
                // Requirements:
                // 1. format == Barcode.FORMAT_QR_CODE
                // 2. rawBytes != null
                // 3. rawBytes.size > 1000 (Aadhaar Secure QR is ~3KB)
                // ============================================================
                var validAadhaarBarcode: com.google.mlkit.vision.barcode.common.Barcode? = null
                
                for (barcode in barcodes) {
                    val rawBytes = barcode.rawBytes
                    if (barcode.format == Barcode.FORMAT_QR_CODE &&
                        rawBytes != null &&
                        rawBytes.size > 1000) {
                        validAadhaarBarcode = barcode
                        android.util.Log.d("QRScanner", "Valid Aadhaar QR found: format=QR_CODE, rawBytes size=${rawBytes.size} bytes")
                        break // Use FIRST valid barcode only
                    }
                }
                
                // Process valid Aadhaar Secure QR if found
                if (validAadhaarBarcode != null) {
                    val rawBytes = validAadhaarBarcode.rawBytes!!
                    
                    // CRITICAL: Close imageProxy ONLY after ML Kit processing completes
                    imageProxy.close()
                    
                    // Mark as processed to prevent duplicate scans
                    isQrProcessed.set(true)
                    
                    // Show single toast notification
                    showToast(context, "Aadhaar QR detected")
                    
                    // Call Aadhaar QR handler if provided
                    if (onAadhaarQRDetected != null) {
                        android.util.Log.d("QRScanner", "Selected barcode rawBytes size=${rawBytes.size} bytes")
                        onAadhaarQRDetected.invoke(rawBytes)
                    } else {
                        android.util.Log.w("QRScanner", "Aadhaar QR detected but no handler provided")
                    }
                    return@addOnSuccessListener
                }
                
                // ============================================================
                // STANDARD JSON QR CODE HANDLING
                // ============================================================
                // Only process if no valid Aadhaar QR was found
                // Find first barcode with valid rawValue
                var standardQrBarcode: com.google.mlkit.vision.barcode.common.Barcode? = null
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (rawValue != null && rawValue.isNotBlank()) {
                        standardQrBarcode = barcode
                        break
                    }
                }
                
                if (standardQrBarcode != null) {
                    val rawValue = standardQrBarcode.rawValue!!
                    
                    // CRITICAL: Close imageProxy ONLY after ML Kit processing completes
                    imageProxy.close()
                    
                    // Production-safe log: truncate QR payload to first 20 chars only
                    val preview = if (rawValue.length > 20) rawValue.take(20) + "..." else rawValue
                    android.util.Log.d("QRScanner", "Standard QR detected: rawValue length=${rawValue.length}, preview=${preview}")
                    
                    // Parse and prefill fields for standard JSON QR codes
                    if (qrConfig.format == "JSON") {
                        try {
                            val jsonObject = JSONObject(rawValue)
                            val prefillData = mutableMapOf<String, String>()
                            
                            qrConfig.prefillMapping.forEach { mapping ->
                                if (jsonObject.has(mapping.qrKey)) {
                                    var value = jsonObject.optString(mapping.qrKey, "")
                                    value = value.trim()
                                    
                                    if (value.isNotBlank()) {
                                        prefillData[mapping.targetFieldId] = value
                                    }
                                }
                            }
                            
                            if (prefillData.isNotEmpty()) {
                                android.util.Log.d("QRScanner", "QR scan successful: prefilling ${prefillData.size} field(s)")
                                onSuccess(prefillData)
                            } else {
                                android.util.Log.w("QRScanner", "QR detected but no matching keys found in payload")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("QRScanner", "JSON parsing failed: ${e.message}", e)
                        }
                    } else {
                        android.util.Log.w("QRScanner", "Unsupported QR format: ${qrConfig.format}")
                    }
                } else {
                    // No valid barcode found - ignore silently (no toast, no API call)
                    imageProxy.close()
                    // Skip logging for invalid barcodes to reduce noise
                }
            } else {
                // No QR code detected in this frame - continue scanning
                imageProxy.close()
                android.util.Log.d("QRScanner", "No QR code detected in frame")
            }
        }
        .addOnFailureListener { e ->
            // Log ML Kit failure with stacktrace for diagnostics
            android.util.Log.e("QRScanner", "ML Kit scanning failed: ${e.message}", e)
            imageProxy.close()
        }
}

/**
 * Helper function to show Toast on main thread
 */
private fun showToast(context: Context, message: String) {
    // Use Handler to ensure Toast runs on main thread
    android.os.Handler(android.os.Looper.getMainLooper()).post {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
