package com.kaleidofin.originator.presentation.ui.state

/**
 * Runtime Session - Single source of truth for flow state
 * 
 * This holds the critical flow state that MUST persist across screens:
 * - applicationId: The backend-managed application ID (created on flow start)
 * - flowId: The flow identifier
 * - currentScreenId: The current screen being displayed
 * 
 * Rules:
 * - Created ONLY when flow starts (user clicks module card)
 * - Updated on every screen submit (currentScreenId = nextScreenId)
 * - Cleared when flow completes
 * - MUST NOT be null once flow has started
 */
data class RuntimeSession(
    val applicationId: Long,
    val flowId: String,
    val currentScreenId: String
)
