package com.kaleidofin.originator.domain.model

data class FlowStep(
    val screenId: String,
    val flowId: String,
    val formDataSnapshot: Map<String, Any?>
)
