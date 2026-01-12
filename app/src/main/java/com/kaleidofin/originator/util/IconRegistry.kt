package com.kaleidofin.originator.util

import com.kaleidofin.originator.R

/**
 * Icon Registry - Maps backend icon keys to Android drawable resources
 * 
 * Usage:
 * - Backend sends icon key (e.g., "APPLICANT_ONBOARDING")
 * - Android maps to drawable resource (e.g., R.drawable.ic_applicant)
 * 
 * Add new mappings here when backend adds new flow types
 */
object IconRegistry {
    
    /**
     * Map icon key to drawable resource ID
     * Returns null if icon key is not recognized (fallback to default icon)
     * 
     * TODO: Add actual icon drawables to res/drawable/ and uncomment mappings
     */
    fun getIconResource(iconKey: String?): Int? {
        if (iconKey == null) return null
        
        // NOTE: These mappings are placeholders
        // Add actual drawable resources before uncommenting
        return when (iconKey.uppercase()) {
            // TODO: Add icon files to res/drawable/ and update mappings
            // "APPLICANT_ONBOARDING" -> R.drawable.ic_applicant
            // "KYC_CAPTURE" -> R.drawable.ic_kyc
            // "CREDIT_CHECK" -> R.drawable.ic_credit
            // "PAYMENT_COLLECTION" -> R.drawable.ic_payment
            // ... add more as needed
            
            else -> null // Return null for unknown icons (UI will show default)
        }
    }
    
    /**
     * Get default icon resource for fallback
     * Uses Material Icons Outlined placeholder
     */
    fun getDefaultIcon(): Int {
        // Use a Material Icon as fallback until custom icons are added
        return android.R.drawable.ic_menu_info_details
    }
}
