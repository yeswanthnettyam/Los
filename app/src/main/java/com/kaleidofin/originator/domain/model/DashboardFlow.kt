package com.kaleidofin.originator.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Dashboard Flow - Domain model
 * Represents a flow that can be started from the dashboard
 */
data class DashboardFlow(
    val flowId: String,
    val title: String,
    val description: String?,
    val icon: String?, // Icon key for IconRegistry
    val ui: DashboardUiMeta?,
    val startable: Boolean,
    val productCode: String?,
    val partnerCode: String?,
    val branchCode: String?
)

/**
 * Dashboard UI customization
 */
data class DashboardUiMeta(
    val backgroundColor: Color?,
    val textColor: Color?,
    val iconColor: Color?
)
