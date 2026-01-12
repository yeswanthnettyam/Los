package com.kaleidofin.originator.util

/**
 * API Configuration
 * 
 * Update BASE_URL to point to your backend API
 * 
 * Development URLs:
 * - Android Emulator accessing localhost: "http://10.0.2.2:8080/"
 * - Physical device on same network: "http://YOUR_LOCAL_IP:8080/" (e.g., "http://192.168.1.100:8080/")
 * 
 * Production URLs:
 * - Staging: "https://api-staging.yourcompany.com/"
 * - Production: "https://api.yourcompany.com/"
 */
object ApiConfig {
    /**
     * Base URL for the API
     * 
     * IMPORTANT: Update this before running the app!
     * 
     * For local development:
     * - If using Android Emulator: use "http://10.0.2.2:8080/"
     * - If using physical device: use "http://YOUR_LOCAL_IP:8080/"
     * 
     * For production: use your actual backend URL
     */
    const val BASE_URL = "http://10.0.2.2:8080/"
    
    /**
     * Enable/disable API logging
     * Set to false in production for performance and security
     */
    const val ENABLE_LOGGING = true
    
    /**
     * API timeout in seconds
     */
    const val TIMEOUT_SECONDS = 30L
}
