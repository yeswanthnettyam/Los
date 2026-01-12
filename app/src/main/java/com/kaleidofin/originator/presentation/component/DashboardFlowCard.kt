package com.kaleidofin.originator.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaleidofin.originator.domain.model.DashboardFlow
import com.kaleidofin.originator.util.IconRegistry

/**
 * Dynamic Dashboard Flow Card
 * 
 * Renders a flow tile based on backend configuration:
 * - UI colors from dashboardMeta.ui
 * - Icon from IconRegistry mapping
 * - Title and description from flow metadata
 */
@Composable
fun DashboardFlowCard(
    flow: DashboardFlow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Get colors from flow UI metadata or use defaults
    val backgroundColor = flow.ui?.backgroundColor ?: MaterialTheme.colorScheme.primaryContainer
    val textColor = flow.ui?.textColor ?: MaterialTheme.colorScheme.onPrimaryContainer
    val iconColor = flow.ui?.iconColor ?: MaterialTheme.colorScheme.primary
    
    // Get icon resource from registry
    val iconRes = IconRegistry.getIconResource(flow.icon) ?: IconRegistry.getDefaultIcon()
    
    Card(
        onClick = if (flow.startable) onClick else {{}},
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        ),
        enabled = flow.startable
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = flow.title,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            
            // Title
            Text(
                text = flow.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Description (if available)
            flow.description?.let { description ->
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Disabled indicator
            if (!flow.startable) {
                Text(
                    text = "Not Available",
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
