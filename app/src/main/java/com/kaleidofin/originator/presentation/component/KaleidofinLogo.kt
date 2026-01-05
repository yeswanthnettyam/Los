package com.kaleidofin.originator.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kaleidofin.originator.R

@Composable
fun KaleidofinLogo(
    modifier: Modifier = Modifier,
    painter: Painter = painterResource(id = R.drawable.kaleidofin_logo),
    tint: Color? = null,
    maxWidthFraction: Float = 0.7f,
    minHeight: androidx.compose.ui.unit.Dp = 120.dp,
    maxHeight: androidx.compose.ui.unit.Dp = 200.dp,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String = "Kaleidofin logo"
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        
        // Calculate dynamic size based on container dimensions
        val logoWidth = containerWidth * maxWidthFraction
        val calculatedHeight = (containerHeight * 0.25f).coerceIn(minHeight, maxHeight)
        
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier
                .widthIn(max = logoWidth)
                .heightIn(min = minHeight, max = calculatedHeight)
                .aspectRatio(ratio = 16f / 9f, matchHeightConstraintsFirst = false),
            contentScale = contentScale,
            colorFilter = if (tint == null) null else ColorFilter.tint(tint)
        )
    }
}

