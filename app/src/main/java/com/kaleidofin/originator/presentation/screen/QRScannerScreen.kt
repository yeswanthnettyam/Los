package com.kaleidofin.originator.presentation.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kaleidofin.originator.domain.model.QRConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.util.Base64
import com.kaleidofin.originator.data.dto.AadhaarDecodeResponseDto

@Composable
fun QRScannerScreen(
    qrConfig: QRConfig,
    onQRScanned: (Map<String, String>) -> Unit,
    onAadhaarQRDetected: ((ByteArray) -> Unit)? = null,
    onNavigateBack: () -> Unit,
    onDecodeAadhaarQR: ((String) -> kotlinx.coroutines.Deferred<Result<AadhaarDecodeResponseDto>>)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    var showDecodeProgress by remember { mutableStateOf(false) }
    var decodeError by remember { mutableStateOf<String?>(null) }
    
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageAnalysisRef by remember { mutableStateOf<ImageAnalysis?>(null) }
    val cameraRef = remember { mutableStateOf<Camera?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var torchEnabled by remember { mutableStateOf(false) }
    val lastProcessTime = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val isQrProcessed = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val lastQrDetectionTime = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    
    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            // Process the selected image
            processImageFromGallery(
                context = context,
                imageUri = imageUri,
                qrConfig = qrConfig,
                onSuccess = { prefillData ->
                    onQRScanned(prefillData)
                    onNavigateBack()
                },
                onAadhaarQRDetected = { rawBytes ->
                    // Mark as processed to prevent duplicate detection during API call
                    isQrProcessed.set(true)
                    
                    // Call backend Aadhaar decode API
                    if (onDecodeAadhaarQR != null) {
                        coroutineScope.launch {
                            try {
                                showDecodeProgress = true
                                decodeError = null
                                
                                // Convert raw bytes to base64
                                val qrDataBase64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                                
                                // Call decode API
                                val result = onDecodeAadhaarQR(qrDataBase64).await()
                                
                                showDecodeProgress = false
                                
                                result.onSuccess { decodedData ->
                                    // API call successful - pass decoded data via callback and navigate back
                                    onAadhaarQRDetected?.invoke(rawBytes)
                                    onNavigateBack()
                                }.onFailure { error ->
                                    // API call failed - show error but keep scanning active
                                    decodeError = error.message ?: "Unable to decode Aadhaar QR"
                                    // Reset processed flag to allow rescanning
                                    isQrProcessed.set(false)
                                }
                            } catch (e: Exception) {
                                showDecodeProgress = false
                                decodeError = e.message ?: "Unable to decode Aadhaar QR"
                                // Reset processed flag to allow rescanning
                                isQrProcessed.set(false)
                            }
                        }
                    } else {
                        // No decode function provided - use callback directly
                        onAadhaarQRDetected?.invoke(rawBytes)
                        onNavigateBack()
                    }
                },
                onDecodeAadhaarQR = onDecodeAadhaarQR,
                coroutineScope = coroutineScope,
                onShowDecodeProgress = { showDecodeProgress = it },
                onSetDecodeError = { decodeError = it }
            )
        }
    }
    
    // Scanning indicator animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val scanningAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanning_alpha"
    )
    
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
            Toast.makeText(context, "Failed to initialize camera", Toast.LENGTH_LONG).show()
            onNavigateBack()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            imageAnalysisRef?.clearAnalyzer()
            cameraProvider?.unbindAll()
        }
    }
    
    BackHandler {
        imageAnalysisRef?.clearAnalyzer()
        cameraProvider?.unbindAll()
        onNavigateBack()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Preview
        if (cameraProvider != null && isScanning) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    // High-resolution selector for better QR detection accuracy
                    // Try highest available resolution (up to 4K) for maximum accuracy like PhonePe/Google Pay
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                android.util.Size(2560, 1440), // Target 1440p for best balance of quality and performance
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                    
                    // Use BLOCK_PRODUCER strategy for better quality (like PhonePe/Google Pay)
                    // This ensures we process every frame with maximum quality, but may be slower
                    // For faster detection, we'll use KEEP_ONLY_LATEST but with higher resolution
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Faster processing
                        .setResolutionSelector(resolutionSelector)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) // Better quality format
                        .build()
                    
                    imageAnalysisRef = imageAnalysis
                    // Use multi-threaded executor for faster processing (like PhonePe/Google Pay)
                    // This allows parallel processing of frames for better performance
                    val analyzerExecutor = Executors.newFixedThreadPool(2)
                    
                    imageAnalysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                        // Prevent processing if scanning is stopped, QR already processed, or API call in progress
                        if (!isScanning || isQrProcessed.get() || showDecodeProgress) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        
                        val currentTime = System.currentTimeMillis()
                        val lastTime = lastProcessTime.get()
                        // Reduced throttling from 300ms to 100ms for faster detection (like PhonePe/Google Pay)
                        if (currentTime - lastTime >= 100) {
                            lastProcessTime.set(currentTime)
                            processQRImage(
                                imageProxy = imageProxy,
                                qrConfig = qrConfig,
                                context = ctx,
                                isQrProcessed = isQrProcessed,
                                lastQrDetectionTime = lastQrDetectionTime,
                                onSuccess = { prefillData ->
                                    isScanning = false
                                    imageAnalysis.clearAnalyzer()
                                    analyzerExecutor.shutdown()
                                    onQRScanned(prefillData)
                                    onNavigateBack()
                                },
                                onAadhaarQRDetected = { rawBytes ->
                                    // Mark as processed IMMEDIATELY to prevent duplicate detection
                                    isQrProcessed.set(true)
                                    
                                    // Call backend Aadhaar decode API
                                    if (onDecodeAadhaarQR != null) {
                                        coroutineScope.launch {
                                            try {
                                                showDecodeProgress = true
                                                decodeError = null
                                                
                                                // Convert raw bytes to base64
                                                val qrDataBase64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                                                
                                                // Call decode API
                                                val result = onDecodeAadhaarQR(qrDataBase64).await()
                                                
                                                showDecodeProgress = false
                                                
                                                result.onSuccess { decodedData ->
                                                    // API call successful - stop scanning and navigate back
                                                    isScanning = false
                                                    imageAnalysis.clearAnalyzer()
                                                    analyzerExecutor.shutdown()
                                                    onAadhaarQRDetected?.invoke(rawBytes)
                                                    onNavigateBack()
                                                }.onFailure { error ->
                                                    // API call failed - show error but keep scanning active
                                                    decodeError = error.message ?: "Unable to decode Aadhaar QR"
                                                    // Reset processed flag to allow rescanning after a delay
                                                    kotlinx.coroutines.delay(500) // Small delay to prevent immediate re-detection
                                                    isQrProcessed.set(false)
                                                }
                                            } catch (e: Exception) {
                                                showDecodeProgress = false
                                                decodeError = e.message ?: "Unable to decode Aadhaar QR"
                                                // Reset processed flag to allow rescanning after a delay
                                                kotlinx.coroutines.delay(500) // Small delay to prevent immediate re-detection
                                                isQrProcessed.set(false)
                                            }
                                        }
                                    } else {
                                        // No decode function provided - stop scanning and navigate back
                                        isScanning = false
                                        imageAnalysis.clearAnalyzer()
                                        analyzerExecutor.shutdown()
                                        onAadhaarQRDetected?.invoke(rawBytes)
                                        onNavigateBack()
                                    }
                                }
                            )
                        } else {
                            imageProxy.close()
                        }
                    }
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider?.unbindAll()
                        val cameraInstance = cameraProvider?.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        
                        // Store camera instance for torch control
                        cameraRef.value = cameraInstance
                        
                        // Enable continuous auto-focus for better QR detection (like PhonePe/Google Pay)
                        // Auto-focus is enabled by default, but we ensure optimal settings
                        cameraInstance?.cameraControl?.setLinearZoom(0f) // Full zoom range for maximum field of view
                    } catch (e: Exception) {
                        android.util.Log.e("QRScanner", "Failed to bind camera", e)
                        Toast.makeText(ctx, "Failed to start camera", Toast.LENGTH_LONG).show()
                        onNavigateBack()
                    }
                    
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Full-screen blurred overlay (will be cut out at scanning frame area)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 25.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }
        
        // Overlay with instructions and frame
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top section with instructions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .padding(top = 48.dp)
            ) {
                Text(
                    text = "Place the QR code inside the frame and move nearer for a clear scan...!",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value + 6).sp
                    ),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Scanning frame overlay - cut out the frame area from blurred overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                // Cut out the frame area from the blurred overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameSize = size.minDimension * 0.8f
                    val frameLeft = (size.width - frameSize) / 2
                    val frameTop = (size.height - frameSize) / 2
                    val frameRight = frameLeft + frameSize
                    val frameBottom = frameTop + frameSize
                    
                    // Draw overlay path that cuts out the frame area
                    val overlayPath = Path().apply {
                        // Outer rectangle (full screen)
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                        
                        // Inner rectangle (frame area) - subtract this
                        moveTo(frameLeft, frameTop)
                        lineTo(frameRight, frameTop)
                        lineTo(frameRight, frameBottom)
                        lineTo(frameLeft, frameBottom)
                        close()
                    }
                    
                    drawPath(
                        path = overlayPath,
                        color = Color.Black.copy(alpha = 0.8f),
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstOut
                    )
                }
                
                // Primary color frame with rounded corners
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameSize = size.minDimension * 0.8f
                    val cornerLength = 40.dp.toPx()
                    val strokeWidth = 4.dp.toPx()
                    val frameLeft = (size.width - frameSize) / 2
                    val frameTop = (size.height - frameSize) / 2
                    val frameRight = frameLeft + frameSize
                    val frameBottom = frameTop + frameSize
                    
                    // Top-left corner
                    drawLine(
                        color = primaryColor,
                        start = Offset(frameLeft, frameTop + cornerLength),
                        end = Offset(frameLeft, frameTop),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(frameLeft, frameTop),
                        end = Offset(frameLeft + cornerLength, frameTop),
                        strokeWidth = strokeWidth
                    )
                    
                    // Top-right corner
                    drawLine(
                        color = primaryColor,
                        start = Offset(frameRight - cornerLength, frameTop),
                        end = Offset(frameRight, frameTop),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(frameRight, frameTop),
                        end = Offset(frameRight, frameTop + cornerLength),
                        strokeWidth = strokeWidth
                    )
                    
                    // Bottom-left corner
                    drawLine(
                        color = primaryColor,
                        start = Offset(frameLeft, frameBottom - cornerLength),
                        end = Offset(frameLeft, frameBottom),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(frameLeft, frameBottom),
                        end = Offset(frameLeft + cornerLength, frameBottom),
                        strokeWidth = strokeWidth
                    )
                    
                    // Bottom-right corner
                    drawLine(
                        color = primaryColor,
                        start = Offset(frameRight - cornerLength, frameBottom),
                        end = Offset(frameRight, frameBottom),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(frameRight, frameBottom),
                        end = Offset(frameRight, frameBottom - cornerLength),
                        strokeWidth = strokeWidth
                    )
                }
                
                // Scanning indicator (red dot)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                Color.Red.copy(alpha = scanningAlpha),
                                CircleShape
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 60.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close button
                    Button(
                        onClick = {
                            imageAnalysisRef?.clearAnalyzer()
                            cameraProvider?.unbindAll()
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray.copy(alpha = 0.7f),
                            contentColor = Color.White
                        ),
                        shape = CircleShape,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(62.dp)
                        )
                    }
                    
                    // Upload QR button
                    Button(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            contentColor = Color.White
                        ),
                        shape = CircleShape,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Upload QR",
                            modifier = Modifier.size(62.dp)
                        )
                    }
                    
                    // Torch button
                    Button(
                        onClick = {
                            torchEnabled = !torchEnabled
                            cameraRef.value?.cameraControl?.enableTorch(torchEnabled)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            contentColor = Color.White
                        ),
                        shape = CircleShape,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Torch",
                            modifier = Modifier.size(62.dp)
                        )
                    }
                }
            }
        }
        
        // Aadhaar Decode Progress Dialog
        if (showDecodeProgress) {
            AlertDialog(
                onDismissRequest = { /* Non-dismissible */ },
                title = { Text("Decoding Aadhaar QR") },
                text = {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                        Text("Please wait while we decode the Aadhaar QR code...")
                    }
                },
                confirmButton = {}
            )
        }
        
        // Aadhaar Decode Error Dialog
        decodeError?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { 
                    decodeError = null
                    // Reset flags and add delay to prevent immediate re-detection
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(500) // Small delay to prevent immediate re-detection
                        isQrProcessed.set(false)
                        showDecodeProgress = false
                    }
                },
                title = { Text("Decode Failed") },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(onClick = { 
                        decodeError = null
                        // Reset flags and add delay to prevent immediate re-detection
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(500) // Small delay to prevent immediate re-detection
                            isQrProcessed.set(false)
                            showDecodeProgress = false
                        }
                    }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

private fun processQRImage(
    imageProxy: androidx.camera.core.ImageProxy,
    qrConfig: QRConfig,
    context: Context,
    isQrProcessed: java.util.concurrent.atomic.AtomicBoolean,
    onSuccess: (Map<String, String>) -> Unit,
    onAadhaarQRDetected: ((ByteArray) -> Unit)? = null,
    lastQrDetectionTime: java.util.concurrent.atomic.AtomicLong? = null
) {
    if (isQrProcessed.get()) {
        imageProxy.close()
        return
    }
    
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    
    val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .enableAllPotentialBarcodes()
        .build()
    
    val scanner = BarcodeScanning.getClient(scannerOptions)
    val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
    
    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            if (isQrProcessed.get()) {
                imageProxy.close()
                return@addOnSuccessListener
            }
            
            if (barcodes.isNotEmpty()) {
                // Check for Aadhaar QR
                var validAadhaarBarcode: Barcode? = null
                for (barcode in barcodes) {
                    val rawBytes = barcode.rawBytes
                    if (barcode.format == Barcode.FORMAT_QR_CODE &&
                        rawBytes != null &&
                        rawBytes.size > 1000) {
                        validAadhaarBarcode = barcode
                        break
                    }
                }
                
                if (validAadhaarBarcode != null) {
                    // Double-check flag to prevent race condition
                    if (isQrProcessed.get()) {
                        imageProxy.close()
                        return@addOnSuccessListener
                    }
                    
                    // Debounce check to prevent duplicate detections
                    val currentTime = System.currentTimeMillis()
                    val lastDetectionTime = lastQrDetectionTime?.get() ?: 0L
                    // Only apply debounce if we've had a previous detection (lastDetectionTime > 0)
                    if (lastDetectionTime > 0 && currentTime - lastDetectionTime < 2000) {
                        imageProxy.close()
                        return@addOnSuccessListener
                    }
                    // Update detection time
                    lastQrDetectionTime?.set(currentTime)
                    
                    // Set flag IMMEDIATELY before any callback to prevent duplicate detection
                    isQrProcessed.set(true)
                    
                    val rawBytes = validAadhaarBarcode.rawBytes!!
                    imageProxy.close()
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context, "Aadhaar QR detected", Toast.LENGTH_LONG).show()
                    }
                    onAadhaarQRDetected?.invoke(rawBytes)
                    return@addOnSuccessListener
                }
                
                // Standard JSON QR
                var standardQrBarcode: Barcode? = null
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (rawValue != null && rawValue.isNotBlank()) {
                        standardQrBarcode = barcode
                        break
                    }
                }
                
                if (standardQrBarcode != null) {
                    val rawValue = standardQrBarcode.rawValue!!
                    imageProxy.close()
                    
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
                                isQrProcessed.set(true)
                                onSuccess(prefillData)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("QRScanner", "JSON parsing failed", e)
                        }
                    }
                } else {
                    imageProxy.close()
                }
            } else {
                imageProxy.close()
            }
        }
        .addOnFailureListener { e ->
            android.util.Log.e("QRScanner", "ML Kit scanning failed", e)
            imageProxy.close()
        }
}

private fun processImageFromGallery(
    context: Context,
    imageUri: Uri,
    qrConfig: QRConfig,
    onSuccess: (Map<String, String>) -> Unit,
    onAadhaarQRDetected: ((ByteArray) -> Unit)? = null,
    onDecodeAadhaarQR: ((String) -> kotlinx.coroutines.Deferred<Result<AadhaarDecodeResponseDto>>)? = null,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    onShowDecodeProgress: ((Boolean) -> Unit)? = null,
    onSetDecodeError: ((String?) -> Unit)? = null
) {
    try {
        val inputImage = InputImage.fromFilePath(context, imageUri)
        
        val scannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAllPotentialBarcodes()
            .build()
        
        val scanner = BarcodeScanning.getClient(scannerOptions)
        
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    // Check for Aadhaar QR
                    var validAadhaarBarcode: Barcode? = null
                    for (barcode in barcodes) {
                        val rawBytes = barcode.rawBytes
                        if (barcode.format == Barcode.FORMAT_QR_CODE &&
                            rawBytes != null &&
                            rawBytes.size > 1000) {
                            validAadhaarBarcode = barcode
                            break
                        }
                    }
                    
                    if (validAadhaarBarcode != null) {
                        val rawBytes = validAadhaarBarcode.rawBytes!!
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(context, "Aadhaar QR detected", Toast.LENGTH_LONG).show()
                        }
                        onAadhaarQRDetected?.invoke(rawBytes)
                        return@addOnSuccessListener
                    }
                    
                    // Standard JSON QR
                    var standardQrBarcode: Barcode? = null
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null && rawValue.isNotBlank()) {
                            standardQrBarcode = barcode
                            break
                        }
                    }
                    
                    if (standardQrBarcode != null) {
                        val rawValue = standardQrBarcode.rawValue!!
                        
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
                                    onSuccess(prefillData)
                                } else {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        Toast.makeText(context, "No QR code found in image", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    Toast.makeText(context, "Failed to parse QR code", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(context, "No QR code found in image", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context, "No QR code found in image", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "Failed to scan QR code: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    } catch (e: Exception) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
