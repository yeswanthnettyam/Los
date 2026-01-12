package com.kaleidofin.originator.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Dashboard API Response
 * GET /api/v1/dashboard/flows
 */
data class DashboardResponseDto(
    @SerializedName("flows") val flows: List<DashboardFlowDto>
)

/**
 * Dashboard Flow Configuration
 * Contains flow metadata and UI customization from FlowConfig.dashboardMeta
 */
data class DashboardFlowDto(
    @SerializedName("flowId") val flowId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("icon") val icon: String? = null, // Icon key (e.g., "APPLICANT_ONBOARDING")
    @SerializedName("ui") val ui: DashboardUiMetaDto? = null,
    @SerializedName("startable") val startable: Boolean = true,
    
    // Context for starting flow
    @SerializedName("productCode") val productCode: String? = null,
    @SerializedName("partnerCode") val partnerCode: String? = null,
    @SerializedName("branchCode") val branchCode: String? = null
)

/**
 * UI customization metadata for dashboard tiles
 */
data class DashboardUiMetaDto(
    @SerializedName("backgroundColor") val backgroundColor: String? = null, // Hex color
    @SerializedName("textColor") val textColor: String? = null, // Hex color
    @SerializedName("iconColor") val iconColor: String? = null // Hex color
)
