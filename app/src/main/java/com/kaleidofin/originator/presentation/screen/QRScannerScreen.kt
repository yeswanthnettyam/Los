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
import com.kaleidofin.originator.data.dto.AadhaarDecodeResponseDto
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QRScannerScreen(
    qrConfig: QRConfig,
    onQRScanned: (Map<String, String>) -> Unit,
    onAadhaarQRDetected: ((String) -> Unit)? = null,
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
                onAadhaarQRDetected = { qrDataString ->
                    // Mark as processed to prevent duplicate detection during API call
                    isQrProcessed.set(true)
                    
                    // Call backend Aadhaar decode API
                    if (onDecodeAadhaarQR != null) {
                        coroutineScope.launch {
                            try {
                                showDecodeProgress = true
                                decodeError = null
                                
                                // Send numeric QR string directly (no Base64 encoding)
                                val result = onDecodeAadhaarQR(qrDataString).await()
                                
                                showDecodeProgress = false
                                
                                result.onSuccess { decodedData ->
                                    // API call successful - pass numeric string via callback (navigation handled by callback)
                                    onAadhaarQRDetected?.invoke(qrDataString)
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
                        onAadhaarQRDetected?.invoke(qrDataString)
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
                    
                    // Higher resolution for Aadhaar QR detection (1080p)
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                android.util.Size(1920, 1080),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
                            )
                        )
                        .build()
                    
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setResolutionSelector(resolutionSelector)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                    
                    imageAnalysisRef = imageAnalysis
                    // Single-thread executor for sequential frame processing
                    val analyzerExecutor = Executors.newSingleThreadExecutor()
                    
                    imageAnalysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                        // Prevent processing if scanning is stopped, QR already processed, or API call in progress
                        if (!isScanning || isQrProcessed.get() || showDecodeProgress) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        
                        val currentTime = System.currentTimeMillis()
                        val lastTime = lastProcessTime.get()
                        // Frame throttling: 60-80ms for Aadhaar QR detection quality
                        if (currentTime - lastTime >= 70) {
                            lastProcessTime.set(currentTime)
                            processQRImage(
                                imageProxy = imageProxy,
                                context = ctx,
                                qrConfig = qrConfig,
                                isQrProcessed = isQrProcessed,
                                onSuccess = { prefillData ->
                                    isScanning = false
                                    imageAnalysis.clearAnalyzer()
                                    analyzerExecutor.shutdown()
                                    onQRScanned(prefillData)
                                    onNavigateBack()
                                },
                                onAadhaarQRDetected = { qrDataString ->
                                    // Mark as processed IMMEDIATELY to prevent duplicate detection
                                    isQrProcessed.set(true)
                                    
                                    // Call backend Aadhaar decode API
                                    if (onDecodeAadhaarQR != null) {
                                        coroutineScope.launch {
                                            try {
                                                showDecodeProgress = true
                                                decodeError = null
                                                
                                                // Send numeric QR string directly (no Base64 encoding)
                                                val result = onDecodeAadhaarQR(qrDataString).await()
                                                
                                                showDecodeProgress = false
                                                
                                                result.onSuccess { decodedData ->
                                                    // API call successful - stop scanning (navigation handled by callback)
                                                    isScanning = false
                                                    imageAnalysis.clearAnalyzer()
                                                    analyzerExecutor.shutdown()
                                                    onAadhaarQRDetected?.invoke(qrDataString)
                                                }.onFailure { error ->
                                                    // API call failed - show error but keep scanning active
                                                    decodeError = error.message ?: "Unable to decode Aadhaar QR"
                                                    // Reset processed flag to allow rescanning after a delay
                                                    kotlinx.coroutines.delay(500)
                                                    isQrProcessed.set(false)
                                                }
                                            } catch (e: Exception) {
                                                showDecodeProgress = false
                                                decodeError = e.message ?: "Unable to decode Aadhaar QR"
                                                // Reset processed flag to allow rescanning after a delay
                                                kotlinx.coroutines.delay(500)
                                                isQrProcessed.set(false)
                                            }
                                        }
                                    } else {
                                        // No decode function provided - stop scanning and navigate back
                                        isScanning = false
                                        imageAnalysis.clearAnalyzer()
                                        analyzerExecutor.shutdown()
                                        onAadhaarQRDetected?.invoke(qrDataString)
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
                        
                        // Enable continuous auto-focus for better QR detection
                        cameraInstance?.cameraControl?.setLinearZoom(0f)
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

// Singleton QR scanner instance for better performance
private val qrScanner by lazy {
    BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )
}

private fun processQRImage(
    imageProxy: ImageProxy,
    context: Context,
    qrConfig: QRConfig,
    isQrProcessed: AtomicBoolean,
    onSuccess: (Map<String, String>) -> Unit,
    onAadhaarQRDetected: ((String) -> Unit)? = null
) {
    val mediaImage = imageProxy.image ?: run {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(
        mediaImage,
        imageProxy.imageInfo.rotationDegrees
    )

    qrScanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            // Ensure imageProxy.close() is called exactly once
            imageProxy.close()

            if (isQrProcessed.get()) return@addOnSuccessListener
            if (barcodes.isEmpty()) return@addOnSuccessListener

            // Aadhaar QR FIRST (numeric string, 3000-4000 chars)
            val aadhaarQr = barcodes.firstOrNull {
                val rawValue = it.rawValue
                rawValue != null && 
                rawValue.length > 1000 && 
                rawValue.all { char -> char.isDigit() }
            }

            if (aadhaarQr != null) {
                isQrProcessed.set(true)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "Aadhaar QR detected", Toast.LENGTH_LONG).show()
                }
                // Pass numeric string directly to callback
                onAadhaarQRDetected?.invoke(aadhaarQr.rawValue!!)
                return@addOnSuccessListener
            }

            // JSON QR
            val jsonQr = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                ?: return@addOnSuccessListener

            if (qrConfig.format == "JSON") {
                try {
                    val json = JSONObject(jsonQr.rawValue!!)
                    val result = mutableMapOf<String, String>()

                    qrConfig.prefillMapping.forEach {
                        if (json.has(it.qrKey)) {
                            val value = json.optString(it.qrKey).trim()
                            if (value.isNotEmpty()) {
                                result[it.targetFieldId] = value
                            }
                        }
                    }

                    if (result.isNotEmpty()) {
                        isQrProcessed.set(true)
                        onSuccess(result)
                    }
                } catch (_: Exception) {
                    // Silent fail - invalid JSON
                }
            }
        }
        .addOnFailureListener {
            imageProxy.close()
        }
}

private fun processImageFromGallery(
    context: Context,
    imageUri: Uri,
    qrConfig: QRConfig,
    onSuccess: (Map<String, String>) -> Unit,
    onAadhaarQRDetected: ((String) -> Unit)? = null,
    onDecodeAadhaarQR: ((String) -> kotlinx.coroutines.Deferred<Result<AadhaarDecodeResponseDto>>)? = null,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    onShowDecodeProgress: ((Boolean) -> Unit)? = null,
    onSetDecodeError: ((String?) -> Unit)? = null
) {
    try {
        val inputImage = InputImage.fromFilePath(context, imageUri)
        
        qrScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isEmpty()) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context, "No QR code found in image", Toast.LENGTH_LONG).show()
                    }
                    return@addOnSuccessListener
                }

                // Aadhaar QR FIRST (numeric string, 3000-4000 chars)
                val aadhaarQr = barcodes.firstOrNull {
                    val rawValue = it.rawValue
                    rawValue != null && 
                    rawValue.length > 1000 && 
                    rawValue.all { char -> char.isDigit() }
                }

                if (aadhaarQr != null) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context, "Aadhaar QR detected", Toast.LENGTH_LONG).show()
                    }
                    // Pass numeric string to callback (will be converted to ByteArray at call site)
                    onAadhaarQRDetected?.invoke(aadhaarQr.rawValue!!)
                    return@addOnSuccessListener
                }

                // JSON QR
                val jsonQr = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                
                if (jsonQr == null) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context, "No QR code found in image", Toast.LENGTH_LONG).show()
                    }
                    return@addOnSuccessListener
                }

                if (qrConfig.format == "JSON") {
                    try {
                        val json = JSONObject(jsonQr.rawValue!!)
                        val result = mutableMapOf<String, String>()

                        qrConfig.prefillMapping.forEach {
                            if (json.has(it.qrKey)) {
                                val value = json.optString(it.qrKey).trim()
                                if (value.isNotEmpty()) {
                                    result[it.targetFieldId] = value
                                }
                            }
                        }

                        if (result.isNotEmpty()) {
                            onSuccess(result)
                        } else {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                Toast.makeText(context, "No QR code found in image", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (_: Exception) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(context, "Failed to parse QR code", Toast.LENGTH_LONG).show()
                        }
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
