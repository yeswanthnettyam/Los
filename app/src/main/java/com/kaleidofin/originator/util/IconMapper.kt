package com.kaleidofin.originator.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    private val iconMap: Map<String, ImageVector> = mapOf(
        "ic_applicant_onboarding" to Icons.Outlined.Badge,
        "ic_credit_check" to Icons.Outlined.CreditScore,
        "ic_group_creation" to Icons.Outlined.Group,
        "ic_kyc_capture" to Icons.Outlined.DocumentScanner,
        "ic_field_verification" to Icons.Outlined.PersonPinCircle,
        "ic_personal_discussion" to Icons.Outlined.Verified,
        "ic_eligibility" to Icons.Outlined.Verified,
        "ic_doc_signing" to Icons.Outlined.Description,
        "ic_payment_collection" to Icons.Outlined.Payment
    )
    
    fun getIcon(iconName: String): ImageVector {
        return iconMap[iconName] ?: Icons.Outlined.Info
    }
}

