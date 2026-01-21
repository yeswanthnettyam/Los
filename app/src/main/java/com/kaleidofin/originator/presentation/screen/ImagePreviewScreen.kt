package com.kaleidofin.originator.presentation.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewScreen(
    imageUri: Uri,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Zoom and pan state
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    // Entry animation
    var isVisible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "preview_alpha"
    )
    val scaleAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "preview_scale"
    )
    
    // Handle system back button
    BackHandler(onBack = onNavigateBack)
    
    LaunchedEffect(Unit) {
        isVisible = true
        coroutineScope.launch {
            bitmap = loadBitmapFromUri(context, imageUri)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Image with zoom and pan (rendered first, so TopAppBar will be on top)
        bitmap?.let { bmp ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
                    .scale(scaleAnimation)
            ) {
                var lastScale by remember { mutableStateOf(1f) }
                
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (lastScale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                
                                val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                                
                                offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                
                                lastScale = newScale
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    val targetScale = if (scale > 1f) 1f else 2.5f
                                    scale = targetScale
                                    lastScale = targetScale
                                    
                                    if (targetScale == 1f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            )
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        
        // Top app bar (rendered last, so it's on top and clickable)
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private suspend fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
