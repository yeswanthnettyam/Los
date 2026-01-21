package com.kaleidofin.originator.presentation.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.kaleidofin.originator.domain.model.CameraConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "CameraCaptureScreen"

enum class CameraState {
    PREVIEW,
    CAPTURING,
    CONFIRMATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCaptureScreen(
    cameraConfig: CameraConfig,
    onImageCaptured: (Uri) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Use rememberSaveable to preserve state when navigating away and back
    // Save state as string because Uri and enum need custom serialization
    var cameraStateStr by rememberSaveable { mutableStateOf(CameraState.PREVIEW.name) }
    var cameraState by remember {
        mutableStateOf(
            try {
                CameraState.valueOf(cameraStateStr)
            } catch (e: Exception) {
                CameraState.PREVIEW
            }
        )
    }
    
    // Update cameraStateStr when cameraState changes
    LaunchedEffect(cameraState) {
        cameraStateStr = cameraState.name
    }
    
    var capturedImageUriStr by rememberSaveable { mutableStateOf<String?>(null) }
    var capturedImageUri by remember {
        mutableStateOf<Uri?>(
            capturedImageUriStr?.let { Uri.parse(it) }
        )
    }
    
    // Update capturedImageUriStr when capturedImageUri changes
    LaunchedEffect(capturedImageUri) {
        capturedImageUriStr = capturedImageUri?.toString()
    }
    
    // Reload bitmap when returning to CONFIRMATION state and URI exists
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(cameraState, capturedImageUri) {
        if (cameraState == CameraState.CONFIRMATION && capturedImageUri != null && capturedBitmap == null) {
            // Reload bitmap if we're in CONFIRMATION state but bitmap is missing
            capturedImageUri?.let { uri ->
                capturedBitmap = loadBitmapFromUri(context, uri)
            }
        }
    }
    var flashAlpha by remember { mutableStateOf(0f) }
    var isCheckingBlur by remember { mutableStateOf(false) }
    var showBlurDialog by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    // Entry animation
    var isVisible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "entry_alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.96f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "entry_scale"
    )

    // Shutter button breathing animation
    val breathingScale by rememberInfiniteTransition(label = "breathing_transition").animateFloat(
        initialValue = 0.98f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            onNavigateBack()
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            capturedImageUri = it
            cameraState = CameraState.CONFIRMATION
            coroutineScope.launch {
                capturedBitmap = loadBitmapFromUri(context, it)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            isVisible = true
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            isVisible = true
        }
    }

    // Setup camera
    LaunchedEffect(hasCameraPermission, previewView) {
        if (hasCameraPermission && previewView != null && cameraState == CameraState.PREVIEW) {
            val provider = cameraProviderFuture.get()
            cameraProvider = provider
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView!!.surfaceProvider)
            }

            val cameraSelector = when (cameraConfig.cameraType) {
                "FRONT" -> CameraSelector.DEFAULT_FRONT_CAMERA
                else -> CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera setup error", e)
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "DisposableEffect cleanup triggered")
            try {
                cameraProvider?.unbindAll()
                cameraProvider = null
                previewView = null
            } catch (e: Exception) {
                Log.e(TAG, "Cleanup error", e)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .scale(scale)
            .background(Color.Black)
    ) {
        when (cameraState) {
            CameraState.PREVIEW -> {
                // Camera Preview
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView = it }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Top overlay with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            Log.d(TAG, "Back button clicked")
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .alpha(alpha)
                            .padding(top = 40.dp, bottom = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Capture Image",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alpha(alpha)
                    )

                    if (false) {
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .alpha(alpha)
                                .padding(top = 40.dp, bottom = 40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                // Bottom controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Shutter button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(breathingScale)
                            .clip(CircleShape)
                            .background(Color.White)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        Log.d(TAG, "Shutter button tapped")
                                        coroutineScope.launch {
                                            captureImage(
                                                context = context,
                                                imageCapture = imageCapture,
                                                cameraConfig = cameraConfig,
                                                onSuccess = { uri ->
                                                    Log.d(TAG, "Image captured successfully: $uri")
                                                    coroutineScope.launch {
                                                        capturedImageUri = uri
                                                        cameraState = CameraState.CAPTURING

                                                        flashAlpha = 0.4f
                                                        delay(120)
                                                        flashAlpha = 0f
                                                        delay(100)

                                                        capturedBitmap = withContext(Dispatchers.IO) {
                                                            loadBitmapFromUri(context, uri)
                                                        }
                                                        Log.d(TAG, "Bitmap loaded, transitioning to CONFIRMATION")
                                                        cameraState = CameraState.CONFIRMATION
                                                    }
                                                },
                                                onError = { e ->
                                                    Log.e(TAG, "Image capture error", e)
                                                }
                                            )
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                // Flash overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = flashAlpha))
                )
            }

            CameraState.CAPTURING -> {
                Log.d(TAG, "State: CAPTURING")
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            CameraState.CONFIRMATION -> {
                Log.d(TAG, "State: CONFIRMATION")
                AnimatedVisibility(
                    visible = cameraState == CameraState.CONFIRMATION,
                    enter = fadeIn() + scaleIn(initialScale = 0.9f),
                    exit = fadeOut() + scaleOut(targetScale = 0.9f)
                ) {
                    ConfirmationView(
                        bitmap = capturedBitmap,
                        imageUri = capturedImageUri,
                        cameraConfig = cameraConfig,
                        isCheckingBlur = isCheckingBlur,
                        showBlurDialog = showBlurDialog,
                        onRetake = {
                            Log.d(TAG, "Retake clicked")
                            cameraState = CameraState.PREVIEW
                            capturedImageUri = null
                            capturedBitmap = null
                            showBlurDialog = false
                            isCheckingBlur = false
                        },
                        onUsePhoto = {
                            capturedImageUri?.let { uri ->
                                Log.d(TAG, "Use Photo clicked - URI: $uri")
                                
                                // Check if blur detection is enabled
                                if (cameraConfig.enableBlurDetection) {
                                    Log.d(TAG, "Starting blur detection")
                                    isCheckingBlur = true
                                    
                                    coroutineScope.launch {
                                        try {
                                            val isBlurry = detectBlur(context, uri)
                                            
                                            isCheckingBlur = false
                                            
                                            if (isBlurry) {
                                                Log.d(TAG, "Image is blurry - showing dialog")
                                                showBlurDialog = true
                                            } else {
                                                Log.d(TAG, "Image is clear - navigating")
                                                onImageCaptured(uri)
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Blur detection error", e)
                                            isCheckingBlur = false
                                            // On error, allow navigation
                                            onImageCaptured(uri)
                                        }
                                    }
                                } else {
                                    // No blur detection - navigate immediately
                                    Log.d(TAG, "Blur detection disabled - navigating immediately")
                                    onImageCaptured(uri)
                                }
                            } ?: run {
                                Log.e(TAG, "Use Photo clicked but URI is null!")
                            }
                        },
                        onDismissBlurDialog = {
                            showBlurDialog = false
                        },
                        onPreview = {
                            capturedImageUri?.let { uri ->
                                Log.d(TAG, "Preview clicked")
                                onNavigateToPreview(uri)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmationView(
    bitmap: Bitmap?,
    imageUri: Uri?,
    cameraConfig: CameraConfig,
    isCheckingBlur: Boolean,
    showBlurDialog: Boolean,
    onRetake: () -> Unit,
    onUsePhoto: () -> Unit,
    onDismissBlurDialog: () -> Unit,
    onPreview: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Image preview
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Captured image",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onPreview),
                contentScale = ContentScale.Fit
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Confirm Image",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Use Photo button
            Button(
                onClick = {
                    Log.d(TAG, "Use Photo button onClick triggered")
                    onUsePhoto()
                },
                enabled = !isCheckingBlur,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCheckingBlur) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking quality...")
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Photo")
                }
            }
            
            // Retake button
            OutlinedButton(
                onClick = onRetake,
                enabled = !isCheckingBlur,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Close, 
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retake", color = Color.White)
            }
        }
        
        // Blur Detection Dialog
        if (showBlurDialog) {
            AlertDialog(
                onDismissRequest = onDismissBlurDialog,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text("Image Quality Issue")
                },
                text = {
                    Text(
                        "The image appears to be blurry. Please retake the photo for better quality.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDismissBlurDialog()
                        onRetake()
                    }) {
                        Text("Retake Photo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissBlurDialog) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private suspend fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    cameraConfig: CameraConfig,
    onSuccess: (Uri) -> Unit,
    onError: (Exception) -> Unit
) {
    val photoFile = File(
        context.getExternalFilesDir(null),
        "JPEG_${System.currentTimeMillis()}_${System.nanoTime()}.jpg"
    )

    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputFileOptions,
        Executors.newSingleThreadExecutor(),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                onSuccess(uri)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)?.also {
                inputStream?.close()
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Optimized blur detection using downsampled image (< 1 second)
 */
private suspend fun detectBlur(context: Context, uri: Uri): Boolean = 
    withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Blur detection START")
            val startTime = System.currentTimeMillis()
            
            // Load downsampled image for blur detection (much faster)
            val bitmap = loadDownsampledBitmap(context, uri, maxSize = 512)
                ?: return@withContext false
            
            // Calculate Laplacian variance for blur detection
            val variance = calculateLaplacianVariance(bitmap)
            bitmap.recycle()
            
            val duration = System.currentTimeMillis() - startTime
            val isBlurry = variance < 100.0 // Threshold - adjust as needed
            
            Log.d(TAG, "Blur detection END: ${duration}ms, variance: $variance, isBlurry: $isBlurry")
            
            isBlurry
        } catch (e: Exception) {
            Log.e(TAG, "Blur detection error", e)
            false // Assume not blurry on error
        }
    }

/**
 * Load downsampled bitmap for fast processing
 */
private fun loadDownsampledBitmap(
    context: Context, 
    uri: Uri, 
    maxSize: Int
): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        // Get dimensions
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        
        // Calculate sample size
        val sampleSize = calculateSampleSize(
            options.outWidth,
            options.outHeight,
            maxSize,
            maxSize
        )
        
        // Load downsampled bitmap
        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize
        
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error loading downsampled bitmap", e)
        null
    }
}

/**
 * Calculate sample size for downsampling
 */
private fun calculateSampleSize(
    width: Int,
    height: Int,
    reqWidth: Int,
    reqHeight: Int
): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && 
               halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/**
 * Calculate Laplacian variance for blur detection (optimized)
 */
private fun calculateLaplacianVariance(bitmap: Bitmap): Double {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    
    var sum = 0.0
    var sumSq = 0.0
    var count = 0
    
    // Apply Laplacian operator
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val idx = y * width + x
            
            // Get grayscale values
            val center = getGrayValue(pixels[idx])
            val top = getGrayValue(pixels[(y - 1) * width + x])
            val bottom = getGrayValue(pixels[(y + 1) * width + x])
            val left = getGrayValue(pixels[y * width + (x - 1)])
            val right = getGrayValue(pixels[y * width + (x + 1)])
            
            // Laplacian: center * 4 - (top + bottom + left + right)
            val laplacian = kotlin.math.abs(
                4 * center - top - bottom - left - right
            ).toDouble()
            
            sum += laplacian
            sumSq += laplacian * laplacian
            count++
        }
    }
    
    // Calculate variance
    val mean = sum / count
    return (sumSq / count) - (mean * mean)
}

/**
 * Get grayscale value from pixel
 */
private fun getGrayValue(pixel: Int): Int {
    val r = (pixel shr 16) and 0xFF
    val g = (pixel shr 8) and 0xFF
    val b = pixel and 0xFF
    return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
}