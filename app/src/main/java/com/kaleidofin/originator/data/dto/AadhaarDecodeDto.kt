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
    
    @SerializedName("yob")
    val yob: String?,
    
    @SerializedName("aadhaarLast4")
    val aadhaarLast4: String?,
    
    @SerializedName("careOf")
    val careOf: String?,
    
    @SerializedName("house")
    val house: String?,
    
    @SerializedName("landmark")
    val landmark: String?,
    
    @SerializedName("location")
    val location: String?,
    
    @SerializedName("street")
    val street: String?,
    
    @SerializedName("subDistrict")
    val subDistrict: String?,
    
    @SerializedName("district")
    val district: String?,
    
    @SerializedName("state")
    val state: String?,
    
    @SerializedName("pinCode")
    val pinCode: String?,
    
    @SerializedName("postOffice")
    val postOffice: String?,
    
    @SerializedName("vtc")
    val vtc: String?,
    
    @SerializedName("address")
    val address: String?,
    
    @SerializedName("emailHash")
    val emailHash: String?,
    
    @SerializedName("mobileHash")
    val mobileHash: String?,
    
    @SerializedName("signature")
    val signature: String?
)
