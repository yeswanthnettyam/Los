package com.kaleidofin.originator.presentation.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kaleidofin.originator.domain.model.CameraConfig
import com.kaleidofin.originator.domain.model.FormField
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DynamicCameraCaptureField(
    field: FormField,
    value: Any?,
    error: String?,
    onValueChange: (String) -> Unit, // URL or ID from upload API
    onBlur: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    uploadImage: suspend (ByteArray, String) -> String? // (imageBytes, mimeType) -> URL/ID
) {
    val context = LocalContext.current
    val cameraConfig = field.cameraConfig ?: return
    val coroutineScope = rememberCoroutineScope()
    
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    
    val imageCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri.value != null) {
            coroutineScope.launch {
                isUploading = true
                uploadError = null
                
                try {
                    val result = processCapturedImage(
                        context = context,
                        uri = imageUri.value!!,
                        cameraConfig = cameraConfig
                    )
                    
                    if (result != null) {
                        val (imageBytes, mimeType) = result
                        val url = uploadImage(imageBytes, mimeType)
                        
                        if (url != null) {
                            onValueChange(url)
                            onBlur()
                        } else {
                            uploadError = "Failed to upload image"
                        }
                    }
                } catch (e: Exception) {
                    uploadError = e.message ?: "Failed to process image"
                } finally {
                    isUploading = false
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
        
        // Current value display or capture button
        if (value != null && value.toString().isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Image captured",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = {
                        if (isEnabled) {
                            val uri = createImageUri(context)
                            imageUri.value = uri
                            imageCaptureLauncher.launch(uri)
                        }
                    }) {
                        Text("Retake")
                    }
                }
            }
        } else {
            Button(
                onClick = {
                    if (isEnabled) {
                        val uri = createImageUri(context)
                        imageUri.value = uri
                        imageCaptureLauncher.launch(uri)
                    }
                },
                enabled = isEnabled && !isUploading,
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
                    Text("Capture Image")
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
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = context.getExternalFilesDir(null)
    val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
    return Uri.fromFile(imageFile)
}

private suspend fun processCapturedImage(
    context: Context,
    uri: Uri,
    cameraConfig: CameraConfig
): Pair<ByteArray, String>? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val imageBytes = inputStream?.readBytes() ?: return null
        
        // Check resolution
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        if (bitmap == null) {
            throw Exception("Failed to decode image")
        }
        
        val width = bitmap.width
        val height = bitmap.height
        
        if (cameraConfig.minWidth != null && width < cameraConfig.minWidth) {
            throw Exception("Image width must be at least ${cameraConfig.minWidth}px")
        }
        
        if (cameraConfig.minHeight != null && height < cameraConfig.minHeight) {
            throw Exception("Image height must be at least ${cameraConfig.minHeight}px")
        }
        
        // Blur detection
        if (cameraConfig.enableBlurDetection) {
            val isBlurry = detectBlur(bitmap)
            if (isBlurry) {
                throw Exception("Image is blurry. Please retake.")
            }
        }
        
        Pair(imageBytes, "image/jpeg")
    } catch (e: Exception) {
        Log.e("CameraCapture", "Error processing image", e)
        throw e
    }
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
