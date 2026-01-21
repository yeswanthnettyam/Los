package com.kaleidofin.originator.presentation.component

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.collect
import com.kaleidofin.originator.domain.model.CameraConfig
import com.kaleidofin.originator.domain.model.FormField
import com.kaleidofin.originator.presentation.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Data class to hold image info
data class CapturedImage(
    val uri: String, // URL or local file path
    val localUri: Uri? = null // Local URI for display (if not uploaded yet)
)

@Composable
fun DynamicCameraCaptureField(
    field: FormField,
    value: Any?,
    error: String?,
    onValueChange: (String) -> Unit, // JSON array string of image URLs/IDs
    onBlur: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    uploadImage: suspend (ByteArray, String) -> String?, // (imageBytes, mimeType) -> URL/ID
    navController: NavController? = null
) {
    val context = LocalContext.current
    val cameraConfig = field.cameraConfig ?: return
    val coroutineScope = rememberCoroutineScope()
    val gson = remember { Gson() }
    
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var pendingImages by remember { mutableStateOf<List<CapturedImage>>(emptyList()) }
    
    // Store mapping of uploaded URLs to their local URIs (for thumbnail display)
    // This persists across value changes so thumbnails don't disappear after upload
    var uploadedUrlToLocalUri by remember { mutableStateOf<Map<String, Uri>>(emptyMap()) }
    
    // Parse current value to list of images
    val currentImages = remember(value, uploadedUrlToLocalUri) {
        if (value == null || value.toString().isBlank()) {
            emptyList<CapturedImage>()
        } else {
            try {
                val valueStr = value.toString()
                // Try parsing as JSON array first
                if (valueStr.startsWith("[")) {
                    val type = object : TypeToken<List<String>>() {}.type
                    val urls: List<String> = gson.fromJson(valueStr, type)
                    urls.map { url ->
                        // Check if this URL has a localUri mapping (for uploaded images)
                        val localUri = uploadedUrlToLocalUri[url]
                        CapturedImage(
                            uri = url,
                            localUri = localUri
                        )
                    }
                } else {
                    // Fallback: treat as comma-separated string
                    valueStr.split(",").mapNotNull { url ->
                        url.trim().takeIf { it.isNotBlank() }?.let { trimmedUrl ->
                            val localUri = uploadedUrlToLocalUri[trimmedUrl]
                            CapturedImage(
                                uri = trimmedUrl,
                                localUri = localUri
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    // Combine current images with pending images
    val allImages = remember(currentImages, pendingImages) {
        (currentImages + pendingImages).distinctBy { it.uri }
    }
    
    val minImages = cameraConfig.minImages ?: 0
    val maxImages = cameraConfig.maxImages
    
    val canCaptureMore = maxImages == null || allImages.size < maxImages
    val hasMinImages = allImages.size >= minImages
    
    // Check if upload API is configured (needed for thumbnail status indicators)
    val hasUploadApi = cameraConfig.uploadApi?.endpoint != null
    
    // Function to launch camera screen
    val launchCamera: () -> Unit = {
        if (navController != null) {
            val configJson = gson.toJson(cameraConfig)
            navController.navigate(Screen.CameraCapture.createRoute(configJson))
        }
    }
    
    // Listen for captured image from camera screen (must be after allImages is defined)
    LaunchedEffect(navController, allImages) {
        navController?.let { nav ->
            nav.currentBackStackEntry?.savedStateHandle?.getStateFlow<String?>("capturedImageUri", null)
                ?.collect { uriString ->
                    uriString?.let {
                        val uri = Uri.parse(it)
                        coroutineScope.launch {
                            try {
                                // Process image (quality checks) - MUST be on IO thread
                                val result = withContext(Dispatchers.IO) {
                                    processCapturedImage(
                                        context = context,
                                        uri = uri,
                                        cameraConfig = cameraConfig
                                    )
                                }
                                
                                if (result != null) {
                                    // Store locally (don't upload immediately)
                                    // uploadOnCapture=true means we'll upload on form submit
                                    val capturedImage = CapturedImage(
                                        uri = uri.toString(), // Store local URI
                                        localUri = uri
                                    )
                                    
                                    // Add to pending images temporarily
                                    pendingImages = pendingImages + capturedImage
                                    
                                    // Update value with all images (including local URIs)
                                    val updatedImages = allImages + capturedImage
                                    val imageUrls = updatedImages.map { it.uri }
                                    val jsonValue = gson.toJson(imageUrls)
                                    onValueChange(jsonValue)
                                    
                                    // Clear pending after a short delay
                                    kotlinx.coroutines.delay(100)
                                    pendingImages = emptyList()
                                    
                                    onBlur()
                                }
                            } catch (e: Exception) {
                                uploadError = e.message ?: "Failed to process image"
                            }
                        }
                        // Clear the saved state
                        nav.currentBackStackEntry?.savedStateHandle?.remove<String>("capturedImageUri")
                    }
                }
        }
    }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Label
        Text(
            text = field.label + if (field.required) " *" else "",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Capture button
        Button(
            onClick = {
                if (isEnabled && canCaptureMore && navController != null) {
                    uploadError = null
                    launchCamera()
                }
            },
            enabled = isEnabled && canCaptureMore && navController != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            val buttonText = when {
                maxImages != null -> "Capture Image (${allImages.size}/$maxImages)"
                else -> "Capture Image"
            }
            Text(buttonText)
        }
        
        // Min/Max images info
        if (minImages > 0 || maxImages != null) {
            Text(
                text = when {
                    minImages > 0 && maxImages != null -> "Required: $minImages-$maxImages images (${allImages.size} captured)"
                    minImages > 0 -> "Required: at least $minImages images (${allImages.size} captured)"
                    maxImages != null -> "Maximum: $maxImages images (${allImages.size} captured)"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    minImages > 0 && allImages.size < minImages -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        // Display captured images
        if (allImages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(allImages) { index, image ->
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                enabled = navController != null,
                                onClick = {
                                    // Navigate to image preview screen
                                    navController?.let { nav ->
                                        val imageUriToShow = image.localUri ?: Uri.parse(image.uri)
                                        nav.navigate(Screen.ImagePreview.createRoute(imageUriToShow.toString()))
                                    }
                                }
                            )
                    ) {
                        // Image thumbnail
                        val imageUriToShow = image.localUri ?: Uri.parse(image.uri)
                        val bitmap = remember(imageUriToShow) {
                            mutableStateOf<Bitmap?>(null)
                        }
                        
                        LaunchedEffect(imageUriToShow) {
                            bitmap.value = withContext(Dispatchers.IO) {
                                loadBitmapFromUri(context, imageUriToShow, 100)
                            }
                        }
                        
                        bitmap.value?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Captured image ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } ?: Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                        
                        // Upload status indicator (top-left corner)
                        val isUploaded = !image.uri.startsWith("content://") && 
                                        !image.uri.startsWith("file://") &&
                                        image.localUri == null
                        val needsUpload = image.uri.startsWith("content://") || 
                                         image.uri.startsWith("file://") ||
                                         image.localUri != null
                        
                        if (isUploaded) {
                            // Green checkmark for uploaded images
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .background(
                                        Color(0xFF4CAF50), // Green
                                        RoundedCornerShape(50)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Uploaded",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else if (needsUpload && hasUploadApi) {
                            // Orange upload icon for images that need upload
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .background(
                                        Color(0xFFFF9800), // Orange
                                        RoundedCornerShape(50)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = "Needs upload",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        
                        // Blue cross/delete button overlay
                        if (isEnabled) {
                            IconButton(
                                onClick = {
                                    // Stop propagation to prevent navigation when clicking delete
                                    val updatedImages = allImages.toMutableList().apply {
                                        removeAt(index)
                                    }
                                    val imageUrls = updatedImages.map { it.uri }
                                    val jsonValue = gson.toJson(imageUrls)
                                    onValueChange(jsonValue)
                                    pendingImages = emptyList()
                                    onBlur()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            Color(0xFF2196F3), // Blue color
                                            RoundedCornerShape(50)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete image",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Upload button (only show when min images are captured and upload API is configured)
        // Check if images are local (content:// or file:// URIs)
        // Note: We don't check localUri here because it's kept for thumbnail display even after upload
        // Only check the URI itself - if it's a URL (http/https), it's already uploaded
        val hasLocalImages = allImages.any { image ->
            image.uri.startsWith("content://") || 
            image.uri.startsWith("file://")
            // Don't check localUri - it's preserved for thumbnails even after upload
        }
        
        // Upload button should only be enabled when:
        // 1. Upload API is configured
        // 2. There are local images to upload (content://, file://, or localUri != null)
        // 3. Min images requirement is satisfied (if specified) - at least minImages captured
        // 4. Not currently uploading
        val canUpload = hasUploadApi && 
                       hasLocalImages && 
                       hasMinImages &&
                       !isUploading
        
        // Show upload button if:
        // 1. Upload API is configured
        // 2. Min images requirement is satisfied (if specified)
        // 3. There are images captured
        // This ensures button shows when minImages is satisfied, even if some images are already uploaded
        val shouldShowUploadButton = hasUploadApi && 
                                    hasMinImages && 
                                    allImages.isNotEmpty()
        
        if (shouldShowUploadButton) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    // Upload all images that haven't been uploaded yet
                    coroutineScope.launch {
                        isUploading = true
                        uploadError = null
                        
                        try {
                            // Filter for local images (content:// or file:// URIs only)
                            // Note: We only check the URI itself, not localUri, because localUri is kept
                            // for thumbnail display even after upload
                            val imagesToUpload = allImages.filter { image ->
                                image.uri.startsWith("content://") || 
                                image.uri.startsWith("file://")
                            }
                            
                            if (imagesToUpload.isNotEmpty()) {
                                val uploadedUrls = mutableListOf<String>()
                                val uploadMapping = mutableMapOf<String, Uri>() // Map uploaded URL -> localUri
                                var uploadSuccessCount = 0
                                var uploadFailureCount = 0
                                
                                for (image in imagesToUpload) {
                                    val uri = image.localUri ?: Uri.parse(image.uri)
                                    
                                    val result = processCapturedImage(
                                        context = context,
                                        uri = uri,
                                        cameraConfig = cameraConfig
                                    )
                                    
                                    if (result != null) {
                                        val (imageBytes, mimeType) = result
                                        val url = uploadImage(imageBytes, mimeType)
                                        if (url != null) {
                                            uploadedUrls.add(url)
                                            // Store mapping: uploaded URL -> localUri (for thumbnail display)
                                            uploadMapping[url] = uri
                                            uploadSuccessCount++
                                        } else {
                                            uploadFailureCount++
                                        }
                                    }
                                }
                                
                                // Build the final list of URLs to save
                                // Always preserve ALL images: already uploaded + newly uploaded + failed uploads (local URIs)
                                val existingUploadedUrls = allImages.mapNotNull { img ->
                                    if (!img.uri.startsWith("content://") && !img.uri.startsWith("file://")) {
                                        img.uri // Already uploaded URLs
                                    } else null
                                }
                                
                                // Keep local images that failed to upload (their URIs stay as local URIs)
                                val failedUploadLocalUris = imagesToUpload
                                    .filter { image ->
                                        val uri = image.localUri ?: Uri.parse(image.uri)
                                        !uploadMapping.values.contains(uri) // Not successfully uploaded
                                    }
                                    .map { it.uri } // Keep original local URI
                                
                                // Combine: existing uploaded URLs + newly uploaded URLs + failed upload local URIs
                                val allUrls = existingUploadedUrls + uploadedUrls + failedUploadLocalUris
                                
                                
                                // Only update value if we have URLs to save (should always be true, but safety check)
                                if (allUrls.isNotEmpty()) {
                                    // Update the mapping to preserve thumbnails after upload
                                    if (uploadMapping.isNotEmpty()) {
                                        uploadedUrlToLocalUri = uploadedUrlToLocalUri + uploadMapping
                                    }
                                    
                                    val jsonValue = gson.toJson(allUrls)
                                    
                                    // Update the value - thumbnails will be preserved via uploadedUrlToLocalUri mapping
                                    onValueChange(jsonValue)
                                    
                                    // Show error if some uploads failed
                                    if (uploadFailureCount > 0) {
                                        uploadError = if (uploadSuccessCount > 0) {
                                            "Some images failed to upload. $uploadSuccessCount succeeded, $uploadFailureCount failed."
                                        } else {
                                            "Failed to upload images. Please try again."
                                        }
                                    }
                                } else {
                                    // This should never happen, but safety check
                                    uploadError = "Failed to upload images. Please try again."
                                }
                                
                                pendingImages = emptyList()
                                onBlur()
                            } else {
                                uploadError = "No images to upload. All images are already uploaded."
                            }
                        } catch (e: Exception) {
                            uploadError = e.message ?: "Failed to upload images"
                        } finally {
                            isUploading = false
                        }
                    }
                },
                enabled = isEnabled && !isUploading && canUpload,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading...")
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Upload",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Images")
                }
            }
        }
        
        // Error message
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        uploadError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun createImageUri(context: Context): Uri {
    // Create a unique file name using timestamp and nano time for better uniqueness
    val photoFile = File(
        context.getExternalFilesDir(null),
        "JPEG_${System.currentTimeMillis()}_${System.nanoTime()}.jpg"
    )
    
    // Use FileProvider to create a content:// URI instead of file:// URI
    // This prevents FileUriExposedException on Android 7.0+ (API 24+)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
}

private suspend fun processCapturedImage(
    context: Context,
    uri: Uri,
    cameraConfig: CameraConfig
): Pair<ByteArray, String>? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val imageBytes = inputStream?.readBytes() ?: return@withContext null
        inputStream?.close()
        
        // Check resolution - decode with options to avoid OOM
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        
        val width = options.outWidth
        val height = options.outHeight
        
        if (cameraConfig.minWidth != null && width < cameraConfig.minWidth) {
            throw Exception("Image width must be at least ${cameraConfig.minWidth}px")
        }
        
        if (cameraConfig.minHeight != null && height < cameraConfig.minHeight) {
            throw Exception("Image height must be at least ${cameraConfig.minHeight}px")
        }
        
        // Note: Blur detection is now done on "Use Photo" button click, not during background processing
        
        Pair(imageBytes, "image/jpeg")
    } catch (e: Exception) {
        throw e
    }
}

private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

private fun detectBlur(bitmap: Bitmap): Boolean {
    // Simple blur detection using variance of Laplacian
    // Lower variance indicates blur
    val laplacianKernel = arrayOf(
        floatArrayOf(0f, -1f, 0f),
        floatArrayOf(-1f, 4f, -1f),
        floatArrayOf(0f, -1f, 0f)
    )
    
    var variance = 0.0
    var mean = 0.0
    val width = bitmap.width
    val height = bitmap.height
    
    // Convert to grayscale and apply Laplacian
    val grayValues = Array(height) { DoubleArray(width) }
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            var sum = 0.0
            for (ky in -1..1) {
                for (kx in -1..1) {
                    val pixel = bitmap.getPixel(x + kx, y + ky)
                    val gray = (0.299 * android.graphics.Color.red(pixel) +
                            0.587 * android.graphics.Color.green(pixel) +
                            0.114 * android.graphics.Color.blue(pixel))
                    sum += gray * laplacianKernel[ky + 1][kx + 1]
                }
            }
            grayValues[y][x] = sum * sum
            mean += grayValues[y][x]
        }
    }
    
    mean /= ((width - 2) * (height - 2))
    
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            variance += (grayValues[y][x] - mean) * (grayValues[y][x] - mean)
        }
    }
    
    variance /= ((width - 2) * (height - 2))
    
    // Threshold: variance < 100 indicates blur
    return variance < 100.0
}

private fun loadBitmapFromUri(context: Context, uri: Uri, maxSize: Int): Bitmap? {
    return try {
        
        // First, get image dimensions without decoding full image
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        
        val width = options.outWidth
        val height = options.outHeight
        
        // Calculate sample size to load at maxSize
        val sampleSize = if (maxSize > 0) {
            calculateInSampleSize(width, height, maxSize, maxSize)
        } else {
            1
        }
        
        // Now decode with downsampling
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inJustDecodeBounds = false
            // Use RGB_565 for thumbnails to save memory and speed up
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        }
        
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
        
        bitmap
    } catch (e: Exception) {
        null
    }
}
