package com.kaleidofin.originator.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Request DTO for Aadhaar QR decode API
 * POST /api/v1/qr/aadhaar/decode
 */
data class AadhaarDecodeRequestDto(
    @SerializedName("qrPayload")
    val qrPayload: String // Numeric Aadhaar QR string
)

/**
 * Response DTO for Aadhaar QR decode API
 */
data class AadhaarDecodeResponseDto(
    @SerializedName("name")
    val name: String?,
    
    @SerializedName("gender")
    val gender: String?,
    
    @SerializedName("dob")
    val dob: String?,
    
    @SerializedName("aadhaarLast4")
    val aadhaarLast4: String?
)
