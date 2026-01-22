package com.kaleidofin.originator.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaleidofin.originator.domain.model.FormField
import com.kaleidofin.originator.domain.usecase.GetMasterDataUseCase
import com.kaleidofin.originator.presentation.ui.state.DynamicFormUiState
import com.kaleidofin.originator.presentation.ui.state.RuntimeSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.kaleidofin.originator.data.datasource.FormDataSource
import com.kaleidofin.originator.data.mapper.toDomain
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.kaleidofin.originator.util.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// FlowStep data class removed - navigation is now backend-driven
// Keeping for potential future use if needed, but flow stack management is removed

/**
 * Local navigation stack entry for managing back navigation
 * Android maintains this stack locally; backend manages flow snapshot
 */
data class NavigationStackEntry(
    val screenId: String,
    val screenConfig: com.kaleidofin.originator.data.dto.FormScreenDto,
    val formData: Map<String, Any>? = null // Store form data for back navigation restoration
)

@HiltViewModel
class DynamicFormViewModel @Inject constructor(
    private val getMasterDataUseCase: GetMasterDataUseCase,
    private val formDataSource: FormDataSource,
    private val savedStateHandle: SavedStateHandle,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    // Local navigation stack for back navigation
    // Backend manages flow snapshot; Android uses this for local back button handling
    private val _navigationStack = mutableListOf<NavigationStackEntry>()
    
    // Gson for serializing RuntimeSession to SavedStateHandle
    private val gson = Gson()
    
    /**
     * Get RuntimeSession from SavedStateHandle (backup) or return null
     */
    private fun getRuntimeSessionFromSavedState(): RuntimeSession? {
        val sessionJson = savedStateHandle.get<String>("runtimeSession")
        return if (sessionJson != null) {
            try {
                gson.fromJson(sessionJson, RuntimeSession::class.java)
            } catch (e: Exception) {
                android.util.Log.e("DynamicFormViewModel", "Failed to deserialize RuntimeSession from SavedStateHandle", e)
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Save RuntimeSession to SavedStateHandle (backup)
     */
    private fun saveRuntimeSessionToSavedState(session: RuntimeSession?) {
        if (session != null) {
            try {
                val sessionJson = gson.toJson(session)
                savedStateHandle["runtimeSession"] = sessionJson
            } catch (e: Exception) {
                android.util.Log.e("DynamicFormViewModel", "Failed to serialize RuntimeSession to SavedStateHandle", e)
            }
        } else {
            savedStateHandle.remove<String>("runtimeSession")
        }
    }
    
    /**
     * Normalize dropdown value: if it's a label from STATIC_JSON field, convert to value.
     * This ensures we always store/use values, not labels, for consistency.
     */
    private fun normalizeDropdownValue(fieldId: String, value: Any?, formScreen: com.kaleidofin.originator.domain.model.FormScreen?): Any? {
        if (value == null || formScreen == null) return value
        
        val valueStr = value.toString().trim()
        if (valueStr.isBlank()) return value
        
        // Find the field definition
        val field = formScreen.sections.flatMap { it.fields }
            .plus(formScreen.sections.flatMap { it.subSections.flatMap { sub -> sub.fields } })
            .find { it.id == fieldId }
        
        // If field has staticData and value matches a label, convert to value
        if (field?.dataSource?.staticData != null && field.dataSource.staticData.isNotEmpty()) {
            val matchingItem = field.dataSource.staticData.find { it.label == valueStr }
            if (matchingItem != null) {
                android.util.Log.d("DynamicFormViewModel", "Normalizing dropdown value: field='$fieldId', label='$valueStr' -> value='${matchingItem.value}'")
                return matchingItem.value
            }
        }
        
        return value
    }
    
    // Helper method to load screen config from DTO (from Flow API responses)
    // This method processes screenConfig from Flow Engine APIs without making additional API calls
    private suspend fun loadScreenFromDto(
        screenConfigDto: com.kaleidofin.originator.data.dto.FormScreenDto, 
        restoreData: Map<String, Any>? = null,
        nextScreenId: String? = null, // Optional: set nextScreen in same state update for atomicity
        applicationId: Int? = null, // Optional: set applicationId in same state update for atomicity
        isLoadingFromResponse: Boolean = false, // Flag to indicate loading from API response (prevents duplicate API calls)
        updatedRuntimeSession: RuntimeSession? = null // Optional: update RuntimeSession atomically with screen load
    ) {
        // Convert DTO to domain model using mapper
        val formScreen = screenConfigDto.toDomain()
        
        // Process screen config (same logic as loadFormConfiguration but without API call)
            val initialData = mutableMapOf<String, Any>()
            formScreen.hiddenFields.forEach { field ->
            val defaultValue = field.defaultValue ?: when (field.type) {
                    "BOOLEAN" -> false
                    "TEXT" -> ""
                    "NUMBER" -> 0
                    else -> ""
                }
            initialData[field.id] = mapOf("value" to defaultValue)
            }

            // Initialize repeatable section instances (including subsections)
            val sectionInstances = mutableMapOf<String, Int>()
            fun processSection(section: com.kaleidofin.originator.domain.model.FormSection) {
                if (section.repeatable) {
                    sectionInstances[section.sectionId] = section.minInstances
                }
                section.subSections.forEach { processSection(it) }
            }
            formScreen.sections.forEach { processSection(it) }

            // Collect all dataSource requirements and initialize field values from JSON (including subsections)
            val masterDataKeys = mutableSetOf<String>()
            val inlineDataMap = mutableMapOf<String, List<String>>()
            
            fun collectFields(section: com.kaleidofin.originator.domain.model.FormSection, sectionIndex: Int? = null) {
                section.fields.forEach { field ->
                // Initialize field value from JSON if present - wrap in { "value": ... }
                    val fieldKey = if (sectionIndex != null) "${field.id}_$sectionIndex" else field.id
                    if (field.value != null) {
                        // Normalize dropdown value (convert label to value if needed)
                        val normalizedValue = normalizeDropdownValue(field.id, field.value, formScreen)
                        initialData[fieldKey] = mapOf("value" to normalizedValue)
                    }
                    
                    field.dataSource?.let { dataSource ->
                        when (dataSource.type) {
                            "INLINE" -> {
                                // Store INLINE values
                                if (dataSource.values != null) {
                                    inlineDataMap[field.id] = dataSource.values
                                }
                            }
                            "MASTER" -> {
                                // Collect MASTER keys to load
                                if (dataSource.key != null) {
                                    masterDataKeys.add(dataSource.key)
                                }
                            }
                            "API" -> {
                                // API dataSource will be loaded on demand when field is accessed
                            }
                        }
                    }
                }
                // Process subsections recursively
                section.subSections.forEach { subSection ->
                    val subInstanceCount = if (subSection.repeatable) {
                        sectionInstances[subSection.sectionId] ?: subSection.minInstances
                    } else {
                        1
                    }
                    repeat(subInstanceCount) { subIndex ->
                        collectFields(subSection, if (subSection.repeatable) subIndex else null)
                    }
                }
            }
            
            // Process all sections and their instances
            formScreen.sections.forEach { section ->
                val instanceCount = if (section.repeatable) {
                    sectionInstances[section.sectionId] ?: section.minInstances
                } else {
                    1
                }
                repeat(instanceCount) { index ->
                    collectFields(section, if (section.repeatable) index else null)
                }
            }

            // Load master data for all MASTER dataSources
            val masterDataMap = mutableMapOf<String, List<String>>()
            masterDataKeys.forEach { key ->
                try {
                    val options = getMasterDataUseCase(key)
                    masterDataMap[key] = options
                } catch (e: Exception) {
                    // If master data fails to load, use empty list
                    masterDataMap[key] = emptyList()
                }
            }

            // If restoreData is provided, use it as the primary source (for back navigation)
            // Wrap restoreData values if they're not already wrapped, and normalize dropdown values
            val finalFormData = if (restoreData != null && restoreData.isNotEmpty()) {
                android.util.Log.d("DynamicFormViewModel", "Restoring form data - ${restoreData.size} fields")
                val restoredData = mutableMapOf<String, Any>()
                restoreData.forEach { (key, value) ->
                    // Extract fieldId from key (handle section indices like "fieldId_0")
                    val fieldId = key.substringBefore("_").takeIf { key.contains("_") && key.substringAfter("_").all { it.isDigit() } } ?: key
                    
                    // Unwrap value if needed
                    val unwrappedValue = if (value is Map<*, *> && value.containsKey("value")) {
                        value["value"] // Already wrapped, extract value
                    } else {
                        value // Not wrapped
                    }
                    
                    // Normalize dropdown value (convert label to value if needed)
                    val normalizedValue = normalizeDropdownValue(fieldId, unwrappedValue, formScreen)
                    
                    // Wrap normalized value
                    val wrappedValue = mapOf("value" to normalizedValue)
                    restoredData[key] = wrappedValue
                    android.util.Log.d("DynamicFormViewModel", "Restored field: $key = $wrappedValue (normalized from '$unwrappedValue')")
                }
                // Merge with initialData for fields that don't exist in restoreData
                initialData.forEach { (key, value) ->
                    if (!restoredData.containsKey(key)) {
                        restoredData[key] = value
                    }
                }
                restoredData
            } else {
                initialData
            }
            
            _uiState.update {
                it.copy(
                    formScreen = formScreen,
                    formData = finalFormData,
                    sectionInstances = sectionInstances,
                    masterData = masterDataMap,
                    inlineData = inlineDataMap,
                    isLoading = false,
                    fieldErrors = emptyMap(), // Clear errors when loading/restoring
                    nextScreen = nextScreenId ?: it.nextScreen, // Set nextScreen if provided, otherwise keep existing
                    applicationId = applicationId ?: it.applicationId, // Set applicationId if provided, otherwise keep existing
                    isLoadingFromResponse = isLoadingFromResponse, // Set flag to prevent duplicate API calls
                    runtimeSession = updatedRuntimeSession ?: it.runtimeSession // Update RuntimeSession atomically if provided
                )
            }
            
            // Save RuntimeSession to SavedStateHandle if updated
            if (updatedRuntimeSession != null) {
                saveRuntimeSessionToSavedState(updatedRuntimeSession)
            }
            
            // Clear the flag after a delay to allow LaunchedEffect to check it
            // Increased delay to ensure navigation completes and LaunchedEffect re-runs with updated state
            if (isLoadingFromResponse) {
                kotlinx.coroutines.delay(500)
                _uiState.update { it.copy(isLoadingFromResponse = false) }
            }
    }

    private val _uiState = MutableStateFlow(
        DynamicFormUiState(
            runtimeSession = getRuntimeSessionFromSavedState() // Restore from SavedStateHandle on init
        )
    )
    val uiState: StateFlow<DynamicFormUiState> = _uiState.asStateFlow()
    
    init {
        // Restore runtimeSession from SavedStateHandle if available
        val restoredSession = getRuntimeSessionFromSavedState()
        if (restoredSession != null) {
            _uiState.update { it.copy(runtimeSession = restoredSession) }
            android.util.Log.d("DynamicFormViewModel", "Restored RuntimeSession from SavedStateHandle: $restoredSession")
        }
    }

    // Flow stack removed - navigation is now backend-driven

    /* ---------------- LOAD ---------------- */

    // Start flow using Flow Engine /flow/start API
    // Returns flowId, currentScreenId, and full screenConfig in single response
    /**
     * Load screen using Runtime API (POST /api/v1/runtime/next-screen)
     * This is the ONLY way to get screen configurations - always uses Runtime API
     * 
     * @param currentScreenId Screen ID to load. If null, loads initial screen (flow start)
     * @param restoreData Optional form data to restore when loading screen
     * @param flowId Flow ID (required for initial load, optional for subsequent)
     * @param productCode Product code (optional)
     * @param partnerCode Partner code (optional, defaults to SAMASTA)
     * @param branchCode Branch code (optional)
     */
    /**
     * START FLOW - Called ONLY when user clicks a module card from dashboard
     * This is the ONLY place where flow start Runtime API is called
     * 
     * STRICT RULES:
     * - Must be called ONLY from dashboard navigation
     * - runtimeSession MUST be null (flow not started)
     * - Creates RuntimeSession and stores applicationId
     * 
     * @param flowId Flow ID (required)
     * @param productCode Product code (required)
     * @param partnerCode Partner code (optional, defaults to SAMASTA)
     * @param branchCode Branch code (optional)
     */
    fun startFlow(
        flowId: String,
        productCode: String,
        partnerCode: String? = null,
        branchCode: String? = null
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val runtimeSession = state.runtimeSession
            
            // CRITICAL INVARIANT: If runtimeSession exists, flow has already started
            // DO NOT call flow start API again - this would create a new application
            if (runtimeSession != null) {
                android.util.Log.e("DynamicFormViewModel", "🚨 VIOLATION: startFlow() called but runtimeSession exists! applicationId: ${runtimeSession.applicationId}, currentScreenId: ${runtimeSession.currentScreenId}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Flow already in progress. Current screen: ${runtimeSession.currentScreenId}"
                    )
                }
                return@launch
            }
            
            // Validate required parameters
            val finalPartnerCode = partnerCode?.takeIf { it.isNotBlank() } ?: "SAMASTA"
            
            android.util.Log.d("DynamicFormViewModel", "🚀 START FLOW - flowId: $flowId, productCode: $productCode, partnerCode: $finalPartnerCode")
            
            _uiState.update { 
                it.copy(
                    isLoading = true,
                    error = null
                )
            }
            
            try {
                // Clear navigation stack for new flow
                _navigationStack.clear()
                
                // Call Runtime API - FLOW START MODE
                // applicationId = null, currentScreenId = null
                android.util.Log.d("DynamicFormViewModel", "📡 Runtime API CALL (FLOW START) - applicationId: null, currentScreenId: null, flowId: $flowId, productCode: $productCode")
                
                val response = formDataSource.nextScreen(
                    applicationId = null, // Flow start - no applicationId yet
                    currentScreenId = null, // Flow start - no current screen
                    flowId = flowId,
                    productCode = productCode,
                    partnerCode = finalPartnerCode,
                    branchCode = branchCode,
                    formData = null // Flow start - no form data
                )
                
                // Extract applicationId from response
                val applicationId = response.applicationId
                val nextScreenId = response.nextScreenId
                
                android.util.Log.d("DynamicFormViewModel", "✅ Runtime API RESPONSE (FLOW START) - applicationId: $applicationId, nextScreenId: $nextScreenId")
                
                if (applicationId == null || nextScreenId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Invalid response: applicationId or nextScreenId is null"
                        )
                    }
                    return@launch
                }
                
                // Create RuntimeSession - flow has started
                val newRuntimeSession = RuntimeSession(
                    applicationId = applicationId.toLong(),
                    flowId = flowId,
                    currentScreenId = nextScreenId
                )
                
                // Save to SavedStateHandle (backup)
                saveRuntimeSessionToSavedState(newRuntimeSession)
                
                // Update state with RuntimeSession and flow context
                _uiState.update {
                    it.copy(
                        runtimeSession = newRuntimeSession,
                        productCode = productCode,
                        partnerCode = finalPartnerCode,
                        branchCode = branchCode,
                        // Legacy fields for backward compatibility
                        applicationId = applicationId,
                        flowId = flowId
                    )
                }
                
                // Push first screen to navigation stack
                _navigationStack.add(
                    NavigationStackEntry(
                        screenId = nextScreenId,
                        screenConfig = response.screenConfig,
                        formData = null
                    )
                )
                
                // Load screen config from response
                loadScreenFromDto(
                    response.screenConfig,
                    restoreData = null,
                    nextScreenId = null, // Don't set nextScreen - we're already on this screen
                    applicationId = applicationId,
                    isLoadingFromResponse = false, // This is flow start, not form submission
                    updatedRuntimeSession = newRuntimeSession // Set RuntimeSession atomically
                )
                
                android.util.Log.d("DynamicFormViewModel", "✅ FLOW STARTED - applicationId: $applicationId, firstScreen: $nextScreenId")
            } catch (e: Exception) {
                android.util.Log.e("DynamicFormViewModel", "❌ FLOW START FAILED", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to start flow"
                    )
                }
            }
        }
    }
    
    /**
     * Start flow using Runtime API (POST /api/v1/runtime/next-screen with currentScreenId = null)
     * Clears navigation stack and loads initial screen
     * 
     * @deprecated Use startFlow(flowId, productCode, ...) instead - this method is kept for backward compatibility
     */
    @Deprecated("Use startFlow(flowId, productCode, ...) instead")
    fun startFlowLegacy(applicationId: String, flowType: String? = null) {
        viewModelScope.launch {
            // Preserve flow context when updating state
            _uiState.update { 
                it.copy(
                    isLoading = true,
                    error = null
                )
            }
            
            try {
                // Clear navigation stack for new flow
                _navigationStack.clear()
                
                // Call Runtime API - POST /api/v1/runtime/next-screen with currentScreenId = null
                // Backend evaluates flow, resolves config, manages snapshot
                val response = formDataSource.nextScreen(
                    applicationId = null, // Not sent
                    currentScreenId = null, // null for first load
                    flowId = flowType, // Use flowType parameter if provided
                    productCode = null, // Will be extracted from screen config response
                    partnerCode = "SAMASTA", // Default to SAMASTA for testing
                    branchCode = null,
                    formData = emptyMap() // Empty object {} for first load
                )
                
                // Extract and store flow context from screen config response
                val screenConfig = response.screenConfig
                val extractedFlowId = screenConfig.flowId ?: flowType
                val extractedProductCode = screenConfig.scope?.productCode
                val extractedPartnerCode = screenConfig.scope?.partnerCode?.takeIf { it.isNotBlank() } ?: "SAMASTA"
                val extractedBranchCode = screenConfig.scope?.branchCode
                
                // Update state with flow context
                _uiState.update {
                    it.copy(
                        flowId = extractedFlowId,
                        productCode = extractedProductCode,
                        partnerCode = extractedPartnerCode,
                        branchCode = extractedBranchCode
                    )
                }
                
                // Push initial screen to navigation stack
                _navigationStack.add(
                    NavigationStackEntry(
                        screenId = response.nextScreenId,
                        screenConfig = response.screenConfig,
                        formData = null
                    )
                )
                
                // Load screen config directly from response - NO separate API call
                loadScreenFromDto(response.screenConfig, restoreData = null)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to start flow"
                    )
        }
    }
        }
    }
    
    // Legacy method removed - Always use loadScreenViaRuntimeApi() instead
    // This ensures we only use Runtime API (POST /api/v1/runtime/next-screen)

    /* ---------------- FIELD UPDATE ---------------- */

    fun updateFieldValue(fieldId: String, value: Any, sectionIndex: Int?) {
        val finalId = sectionIndex?.let { "${fieldId}_$it" } ?: fieldId
        val state = _uiState.value
        val formScreen = state.formScreen

        // Normalize dropdown value (convert label to value if needed) - safeguard in case UI didn't convert
        val normalizedValue = normalizeDropdownValue(fieldId, value, formScreen)

        android.util.Log.d("DynamicFormViewModel", "Updating field: $finalId (base fieldId: $fieldId, sectionIndex: $sectionIndex) with value: '$value' (normalized: '$normalizedValue')")

        // Check if this field has verification - if value changes, reset verification status
        // Find field in sections and subsections recursively (check all subsections, not just first)
        fun findFieldInAllSections(section: com.kaleidofin.originator.domain.model.FormSection): com.kaleidofin.originator.domain.model.FormField? {
            // First check fields in current section
            section.fields.find { it.id == fieldId }?.let { return it }
            
            // Then check all subsections recursively
            section.subSections.forEach { subSection ->
                findFieldInAllSections(subSection)?.let { return it }
            }
            
            return null
        }
        
        val field = formScreen?.sections?.firstNotNullOfOrNull { findFieldInAllSections(it) }
        val verificationStatusField = field?.verification?.statusField
        
        android.util.Log.d("DynamicFormViewModel", "Found field: ${field?.id}, type: ${field?.type}, isVerifiedInputField: ${field?.type == "VERIFIED_INPUT" || field?.type == "API_VERIFICATION"}")
        
        // Check if value actually changed - unwrap old value for comparison
        val oldWrappedValue = state.formData[finalId]
        val oldValue = if (oldWrappedValue is Map<*, *> && oldWrappedValue.containsKey("value")) {
            oldWrappedValue["value"]
        } else {
            oldWrappedValue
        }
        val valueChanged = oldValue?.toString() != normalizedValue.toString()
        
        android.util.Log.d("DynamicFormViewModel", "Value changed: $valueChanged (old: '$oldValue', new: '$value')")
        
        // Determine if this is a verified input field - apply same logic for VERIFIED_INPUT and API_VERIFICATION
        val isVerifiedInputField = field?.type == "VERIFIED_INPUT" || field?.type == "API_VERIFICATION"
        // Use finalId to handle section indices correctly (e.g., "fieldId_0_verified")
        // Both VERIFIED_INPUT and API_VERIFICATION use the same verification status key format
        val verifiedStatusField = if (isVerifiedInputField) "${finalId}_verified" else null
        
        android.util.Log.d("DynamicFormViewModel", "Field type check - isVerifiedInputField: $isVerifiedInputField, verifiedStatusField: $verifiedStatusField")

        _uiState.update { currentState ->
            val newFormData = currentState.formData.toMutableMap().apply {
                // Wrap normalized value in { "value": ... } object
                put(finalId, mapOf("value" to normalizedValue))
                
                // Reset verification status when field value changes
                // For VERIFIED_INPUT and API_VERIFICATION fields, reset {finalId}_verified
                if (valueChanged && isVerifiedInputField && verifiedStatusField != null) {
                    put(verifiedStatusField, false)
                    android.util.Log.d("DynamicFormViewModel", "✅ Reset verification status for: $verifiedStatusField because field $finalId changed (field type: ${field?.type})")
                } else if (valueChanged && isVerifiedInputField) {
                    android.util.Log.w("DynamicFormViewModel", "⚠️ Field $finalId is verified input but verifiedStatusField is null (field type: ${field?.type})")
                } else if (valueChanged && field != null) {
                    android.util.Log.d("DynamicFormViewModel", "ℹ️ Field $finalId changed but is not verified input (field type: ${field.type})")
                } else if (field == null) {
                    android.util.Log.w("DynamicFormViewModel", "⚠️ Field $fieldId not found in form configuration")
                }
                
                // For other verification fields, reset using verification.statusField
                if (valueChanged && verificationStatusField != null) {
                    put(verificationStatusField, false)
                    android.util.Log.d("DynamicFormViewModel", "Reset verification status for: $verificationStatusField because field $finalId changed")
                }
            }
            
            // Clear errors for this field and dependent fields that become disabled
            val newFieldErrors = currentState.fieldErrors.toMutableMap().apply {
                // For dropdowns and other fields, clear error when value changes
                // Validation will happen on blur with the updated value
                if (valueChanged) {
                    remove(finalId) // Clear error when value changes - will be re-validated on blur
                    
                    // Also clear verification error for verified input fields (VERIFIED_INPUT and API_VERIFICATION) when value changes
                    if (isVerifiedInputField) {
                        remove(finalId) // Clear any verification error
                        android.util.Log.d("DynamicFormViewModel", "✅ Cleared verification error for field: $finalId (type: ${field?.type})")
                    }
                }
                
                // Clear errors for dependent fields that become disabled (including subsections)
                fun processSection(section: com.kaleidofin.originator.domain.model.FormSection) {
                    val instanceCount = if (section.repeatable) {
                        currentState.sectionInstances[section.sectionId] ?: section.minInstances
                    } else {
                        1
                    }
                    
                    repeat(instanceCount) { index ->
                        section.fields.forEach { dependentField ->
                            dependentField.enabledWhen?.let { condition ->
                                val dependentFieldKey = if (section.repeatable) "${dependentField.id}_$index" else dependentField.id
                                
                                // Check if dependent field should be enabled with new value
                                val tempState = currentState.copy(formData = newFormData)
                                val isEnabled = tempState.evaluateDependencyCondition(condition, if (section.repeatable) index else null)
                                
                                // If field becomes disabled, clear its error and value
                                val wasEnabled = currentState.evaluateDependencyCondition(condition, if (section.repeatable) index else null)
                                
                                if (wasEnabled && !isEnabled) {
                                    // Field is being disabled - clear error and value
                                    remove(dependentFieldKey)
                                    newFormData.remove(dependentFieldKey) // Clear the entered value
                                    android.util.Log.d("DynamicFormViewModel", "Cleared error and value for disabled dependent field: $dependentFieldKey")
                                } else if (!isEnabled && containsKey(dependentFieldKey)) {
                                    // Field was already disabled but had error - clear error
                                    remove(dependentFieldKey)
                                }
                            }
                            
                            // Handle visibleWhen - clear value and error if field becomes invisible
                            dependentField.visibleWhen?.let { condition ->
                                val dependentFieldKey = if (section.repeatable) "${dependentField.id}_$index" else dependentField.id
                                val tempState = currentState.copy(formData = newFormData)
                                val isVisible = tempState.evaluateDependencyCondition(condition, if (section.repeatable) index else null)
                                val wasVisible = currentState.evaluateDependencyCondition(condition, if (section.repeatable) index else null)
                                
                                if (wasVisible && !isVisible) {
                                    // Field is being hidden - clear error, value, and verification state
                                    remove(dependentFieldKey)
                                    newFormData.remove(dependentFieldKey) // Clear the entered value
                                    
                                    // Clear verification state if it's a verified input field
                                    val verificationKey = "${dependentFieldKey}_verified"
                                    remove(verificationKey)
                                    newFormData.remove(verificationKey)
                                    
                                    android.util.Log.d("DynamicFormViewModel", "Cleared error, value, and verification for hidden dependent field: $dependentFieldKey")
                                } else if (!isVisible && containsKey(dependentFieldKey)) {
                                    // Field was already hidden but had error - clear error
                                    remove(dependentFieldKey)
                                }
                            }
                        }
                    }
                    
                    // Process subsections recursively
                    section.subSections.forEach { processSection(it) }
                }
                
                formScreen?.sections?.forEach { processSection(it) }
            }
            
            currentState.copy(
                formData = newFormData,
                fieldErrors = newFieldErrors
            )
        }
        
        android.util.Log.d("DynamicFormViewModel", "Field updated. New value in state: '${_uiState.value.formData[finalId]}'")
    }

    /* ---------------- BLUR VALIDATION ---------------- */

    fun validateFieldOnBlur(fieldId: String, value: Any?, sectionIndex: Int?) {
        val finalId = sectionIndex?.let { "${fieldId}_$it" } ?: fieldId
        
        _uiState.update { state ->
            val formScreen = state.formScreen ?: return@update state
            
            // Find field in sections and subsections recursively
            fun findFieldInAllSections(section: com.kaleidofin.originator.domain.model.FormSection): com.kaleidofin.originator.domain.model.FormField? {
                return section.fields.find { it.id == fieldId } 
                    ?: section.subSections.firstOrNull()?.let { findFieldInAllSections(it) }
            }
            
            val field = formScreen.sections.firstNotNullOfOrNull { findFieldInAllSections(it) } ?: return@update state

            // Skip validation for DATE fields on blur - only validate on submit
            if (field.type == "DATE") {
                return@update state
            }

            // Use the current value from state (which should be updated by updateFieldValue)
            // Unwrap value from { "value": ... } object
            val wrappedValue = state.formData[finalId]
            val currentValue = if (wrappedValue is Map<*, *> && wrappedValue.containsKey("value")) {
                wrappedValue["value"]
            } else {
                wrappedValue ?: value
            }
            
            // Validate using current state
            val isFieldEnabled = field.enabledWhen?.let { condition ->
                state.evaluateDependencyCondition(condition, sectionIndex)
            } ?: true
            
            val error = if (!isFieldEnabled) {
                null // No error for disabled fields
            } else {
                // Required validation
                if (field.required && (currentValue == null || (currentValue is String && currentValue.isBlank()))) {
                    "${field.label} is required"
                } else if (field.validation != null && currentValue is String && currentValue.isNotBlank()) {
                    // Regex validation
                    if (!field.validation.regex.toRegex().matches(currentValue)) {
                        field.validation.errorMessage
                    } else {
                        null
                    }
                } else if (field.type == "NUMBER" && currentValue is String && currentValue.isNotBlank()) {
                    // Check if it's a valid number (only digits)
                    if (!currentValue.all { it.isDigit() }) {
                        "${field.label} must be a number"
                    } else {
                        // Convert to number for numeric value validation
                    val num = currentValue.toIntOrNull()
                        if (num == null) {
                            "${field.label} must be a valid number"
                        } else {
                            // For NUMBER fields, validate numeric value constraints using min/max
                    when {
                                field.min != null && num < field.min -> "${field.label} must be at least ${field.min}"
                                field.max != null && num > field.max -> "${field.label} must be at most ${field.max}"
                                // Also validate length constraints for NUMBER fields
                                field.maxLength != null && currentValue.length > field.maxLength -> 
                                    "${field.label} must be at most ${field.maxLength} characters"
                                else -> null
                            }
                        }
                    }
                } else if (field.type == "DROPDOWN" && field.selectionMode == "MULTIPLE" && currentValue is String) {
                    // Validate multi-select dropdown constraints (minSelections, maxSelections)
                    val selectedValues = if (currentValue.isBlank()) {
                        emptyList<String>()
                    } else {
                        currentValue.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    }
                    
                    when {
                        field.minSelections != null && selectedValues.size < field.minSelections -> 
                            "${field.label} must select at least ${field.minSelections} ${if (field.minSelections == 1) "option" else "options"}"
                        field.maxSelections != null && selectedValues.size > field.maxSelections -> 
                            "${field.label} must select at most ${field.maxSelections} ${if (field.maxSelections == 1) "option" else "options"}"
                        else -> null
                    }
                } else if (currentValue is String && currentValue.isNotBlank()) {
                    // Validate min/max length for all text-based input fields (TEXT, TEXTAREA, VERIFIED_INPUT, API_VERIFICATION, etc.)
                    when {
                        field.maxLength != null && currentValue.length > field.maxLength -> 
                            "${field.label} must be at most ${field.maxLength} characters"
                        field.min != null && currentValue.length < field.min -> 
                            "${field.label} must be at least ${field.min} characters"
                        field.max != null && currentValue.length > field.max -> 
                            "${field.label} must be at most ${field.max} characters"
                        else -> null
                    }
                } else {
                    null
                }
            }

            val errors = state.fieldErrors.toMutableMap()
            if (error != null) {
                errors[finalId] = error
            } else {
                errors.remove(finalId) // Clear error if validation passes
            }
            state.copy(fieldErrors = errors)
        }
    }

    /* ---------------- PURE VALIDATION ---------------- */

    fun validateSingleField(field: FormField, value: Any?, sectionIndex: Int? = null): String? {
        val state = _uiState.value
        
        // Check if field is enabled based on enabledWhen conditions
        val isFieldEnabled = field.enabledWhen?.let { condition ->
            state.evaluateDependencyCondition(condition, sectionIndex)
        } ?: true // Field is always enabled if no enabledWhen conditions
        
        // Skip validation if field is disabled
        if (!isFieldEnabled) {
            return null // No error for disabled fields
        }

        // Required validation - check both field.required and requiredWhen conditions
        val isRequired = field.required || (field.requiredWhen?.let { condition ->
            state.evaluateDependencyCondition(condition, sectionIndex)
        } ?: false)
        
        if (isRequired && (value == null || (value is String && value.isBlank()))) {
            return "${field.label} is required"
        }

        if (field.validation != null && value is String && value.isNotBlank()) {
            if (!field.validation.regex.toRegex().matches(value))
                return field.validation.errorMessage
        }

        if (field.type == "NUMBER" && value is String && value.isNotBlank()) {
            // Check if it's a valid number (only digits)
            if (!value.all { it.isDigit() }) {
                return "${field.label} must be a number"
            }
            
            // Convert to number for numeric value validation
            val num = value.toIntOrNull()
            if (num == null && value.isNotEmpty()) {
                return "${field.label} must be a valid number"
            }
            
            // For NUMBER fields, validate numeric value constraints using min/max
            if (num != null) {
                field.min?.let { minValue ->
                    if (num < minValue) {
                        return "${field.label} must be at least $minValue"
                    }
                }
                field.max?.let { maxValue ->
                    if (num > maxValue) {
                        return "${field.label} must be at most $maxValue"
                    }
                }
            }
            
            // For NUMBER fields, also validate length constraints
            // maxLength validates maximum character count
            field.maxLength?.let { maxLength ->
                if (value.length > maxLength) {
                    return "${field.label} must be at most $maxLength characters"
                }
            }
            
            // Note: For NUMBER fields, min/max are used for numeric values, not length
            // If length validation is needed, use maxLength for maximum length
            // For minimum length, we could add a separate minLength field in the future
        }
        
        // Validate multi-select dropdown constraints (minSelections, maxSelections)
        if (field.type == "DROPDOWN" && field.selectionMode == "MULTIPLE" && value is String) {
            // Parse comma-separated value into list
            val selectedValues = if (value.isBlank()) {
                emptyList<String>()
            } else {
                value.split(",").map { it.trim() }.filter { it.isNotBlank() }
            }
            
            // Validate minSelections
            field.minSelections?.let { minSelections ->
                if (selectedValues.size < minSelections) {
                    return "${field.label} must select at least $minSelections ${if (minSelections == 1) "option" else "options"}"
                }
            }
            
            // Validate maxSelections
            field.maxSelections?.let { maxSelections ->
                if (selectedValues.size > maxSelections) {
                    return "${field.label} must select at most $maxSelections ${if (maxSelections == 1) "option" else "options"}"
                }
            }
        }
        
        // Validate min/max length for all text-based input fields
        // This applies to: TEXT, TEXTAREA, VERIFIED_INPUT, API_VERIFICATION, and any other text input types
        if (value is String && value.isNotBlank()) {
            // Validate maxLength for all text input fields
            field.maxLength?.let { maxLength ->
                if (value.length > maxLength) {
                    return "${field.label} must be at most $maxLength characters"
                }
            }
            
            // Validate min length for all text input fields
            field.min?.let { minLength ->
                if (value.length < minLength) {
                    return "${field.label} must be at least $minLength characters"
                }
            }
            
            // Validate max length for all text input fields (alternative to maxLength)
            field.max?.let { maxLength ->
                if (value.length > maxLength) {
                    return "${field.label} must be at most $maxLength characters"
                }
            }
        }

        return null
    }

    /* ---------------- FORM-LEVEL VALIDATION ---------------- */

    /**
     * Validate a form-level validation rule
     * @return Error message if validation fails, null if passes
     */
    private fun validateFormLevelRule(
        rule: com.kaleidofin.originator.domain.model.FormValidationRule,
        formData: Map<String, Any?>,
        screen: com.kaleidofin.originator.domain.model.FormScreen
    ): String? {
        val fieldId = rule.fieldId ?: return null
        
        return when (rule.type) {
            "REQUIRES_VERIFICATION" -> {
                // Check if the field is verified
                // For VERIFIED_INPUT and API_VERIFICATION fields, check for {fieldId}_verified or {fieldId}_{index}_verified
                // Check base field first
                var verifiedStatus = formData["${fieldId}_verified"] as? Boolean ?: false
                
                // If not verified, check all possible instances in repeatable sections
                if (!verifiedStatus) {
                    // Check all keys that match the pattern {fieldId}_*_verified
                    verifiedStatus = formData.keys.any { key ->
                        key.startsWith("${fieldId}_") && key.endsWith("_verified") && 
                        (formData[key] as? Boolean ?: false)
                    }
                }
                
                if (!verifiedStatus) {
                    // Find the field to get its label
                    val field = findFieldById(fieldId, screen)
                    val errorMessage = rule.message ?: "${field?.label ?: fieldId} must be verified"
                    errorMessage
                } else {
                    null
                }
            }
            // Add more validation types here as needed
            else -> {
                // Unknown validation type - log and return null (pass)
                android.util.Log.w("DynamicFormViewModel", "Unknown form-level validation type: ${rule.type}")
                null
            }
        }
    }

    /**
     * Find a field by ID in sections and subsections recursively
     */
    private fun findFieldById(
        fieldId: String,
        screen: com.kaleidofin.originator.domain.model.FormScreen
    ): com.kaleidofin.originator.domain.model.FormField? {
        fun searchInSection(section: com.kaleidofin.originator.domain.model.FormSection): com.kaleidofin.originator.domain.model.FormField? {
            // Check fields in this section
            section.fields.forEach { field ->
                if (field.id == fieldId) return field
            }
            // Check subsections recursively
            section.subSections.forEach { subSection ->
                searchInSection(subSection)?.let { return it }
            }
            return null
        }
        
        screen.sections.forEach { section ->
            searchInSection(section)?.let { return it }
        }
        return null
    }

    /* ---------------- SUBMIT ---------------- */

    fun submitForm() {
        val state = _uiState.value
        val screen = state.formScreen ?: return

        val errors = mutableMapOf<String, String>()
        var firstError: String? = null

        fun validateSection(section: com.kaleidofin.originator.domain.model.FormSection) {
            val count =
                if (section.repeatable)
                    state.sectionInstances[section.sectionId] ?: section.minInstances
                else 1

            repeat(count) { index ->
                section.fields.forEach { field ->
                    val key =
                        if (section.repeatable) "${field.id}_$index" else field.id
                    val wrappedValue = state.formData[key]
                    // Unwrap value from { "value": ... } object
                    val value = if (wrappedValue is Map<*, *> && wrappedValue.containsKey("value")) {
                        wrappedValue["value"]
                    } else {
                        wrappedValue
                    }

                    val error = validateSingleField(field, value, if (section.repeatable) index else null)
                    if (error != null) {
                        errors[key] = error
                        if (firstError == null) firstError = key
                    }
                }
            }
            
            // Validate subsections recursively
            section.subSections.forEach { validateSection(it) }
        }
        
        screen.sections.forEach { validateSection(it) }

        // Step 1: Field-level validation - if errors exist, stop here
        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    fieldErrors = errors,
                    firstErrorFieldId = firstError,
                    error = "Please correct highlighted fields"
                )
            }
            return
        }

        // Step 2: Form-level validation (only if field-level validation passes)
        val formValidationErrors = mutableMapOf<String, String>()
        var firstFormError: String? = null
        
        screen.validations?.rules?.forEach { rule ->
            // Only execute validations with executionTarget = "FRONTEND"
            if (rule.executionTarget == "FRONTEND") {
                val validationError = validateFormLevelRule(rule, state.formData, screen)
                if (validationError != null && rule.fieldId != null) {
                    formValidationErrors[rule.fieldId] = validationError
                    if (firstFormError == null) firstFormError = rule.fieldId
                }
            }
        }

        // If form-level validation fails, show errors and stop
        if (formValidationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    fieldErrors = it.fieldErrors.toMutableMap().apply {
                        putAll(formValidationErrors)
                    },
                    firstErrorFieldId = firstFormError ?: it.firstErrorFieldId,
                    error = "Please correct highlighted fields"
                )
            }
            return
        }

        // ✅ success flow - call Runtime API (POST /api/v1/runtime/next-screen)
        // Backend evaluates flow conditions and returns next screen with full screenConfig
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            
            try {
                // Get RuntimeSession - REQUIRED for screen progression
                val currentState = _uiState.value
                val runtimeSession = currentState.runtimeSession
                
                if (runtimeSession == null) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = "Flow not started. Please restart the flow."
                        )
                    }
                    return@launch
                }
                
                // Unwrap values from { "value": ... } objects before sending to backend
                // Also normalize dropdown values (convert labels to values) to ensure consistency
                val unwrappedFormData = state.formData.filterValues { it != null }.mapNotNull { entry ->
                    val wrapped = entry.value!!
                    val unwrapped = if (wrapped is Map<*, *> && wrapped.containsKey("value")) {
                        wrapped["value"]!!
                    } else {
                        wrapped
                    }
                    
                    // Extract fieldId from key (handle section indices like "fieldId_0")
                    val fieldId = entry.key.substringBefore("_").takeIf { 
                        entry.key.contains("_") && entry.key.substringAfter("_").all { it.isDigit() } 
                    } ?: entry.key
                    
                    // Normalize dropdown value (convert label to value if needed)
                    val normalizedValue = normalizeDropdownValue(fieldId, unwrapped, screen)
                    
                    // Only include non-null values - filter out any nulls from normalization
                    normalizedValue?.let { entry.key to it }
                }.toMap()
            
                // Update dummy JSON with form data for testing (if needed)
                formDataSource.updateFormData(screen.screenId, unwrappedFormData)
                
                // Get flow context from RuntimeSession and state
                val partnerCode = currentState.partnerCode?.takeIf { it.isNotBlank() } ?: "SAMASTA"
                val branchCode = currentState.branchCode
                
                android.util.Log.d("DynamicFormViewModel", "📤 SCREEN SUBMIT - applicationId: ${runtimeSession.applicationId}, currentScreenId: ${runtimeSession.currentScreenId}")
            
                // Call Runtime API - SCREEN PROGRESSION MODE
                // applicationId and currentScreenId from RuntimeSession
                android.util.Log.d("DynamicFormViewModel", "📡 Runtime API CALL (SCREEN PROGRESSION) - applicationId: ${runtimeSession.applicationId}, currentScreenId: ${runtimeSession.currentScreenId}, flowId: ${runtimeSession.flowId}")
                
                val response = formDataSource.nextScreen(
                    applicationId = runtimeSession.applicationId.toString(), // From RuntimeSession
                    currentScreenId = runtimeSession.currentScreenId, // From RuntimeSession
                    flowId = runtimeSession.flowId, // From RuntimeSession
                    productCode = currentState.productCode ?: throw IllegalStateException("productCode is null"),
                    partnerCode = partnerCode,
                    branchCode = branchCode,
                    formData = unwrappedFormData // Send ONLY formData values
                )
                
                android.util.Log.d("DynamicFormViewModel", "✅ Runtime API RESPONSE (SCREEN PROGRESSION) - applicationId: ${response.applicationId}, nextScreenId: ${response.nextScreenId}, status: ${response.status}")
                
                // Extract response data
                val screenConfig = response.screenConfig
                val nextScreenId = response.nextScreenId
                val responseApplicationId = response.applicationId
                val responseStatus = response.status
                
                // FLOW COMPLETION HANDLING
                // If nextScreenId is null and status is COMPLETED, flow has ended
                if (nextScreenId == null && responseStatus == "COMPLETED") {
                    android.util.Log.d("DynamicFormViewModel", "Flow completed - applicationId: ${runtimeSession.applicationId}")
                    
                    // Clear RuntimeSession - flow is complete
                    saveRuntimeSessionToSavedState(null)
            
            _uiState.update {
                        it.copy(
                            runtimeSession = null,
                            isSubmitting = false,
                            isFlowCompleted = true, // Mark flow as completed - prevents restart
                            // Legacy fields
                            applicationId = null,
                            flowId = null
                        )
                    }
                    
                    // TODO: Navigate to dashboard or success screen
                    // For now, just log - UI should handle navigation
                    android.util.Log.d("DynamicFormViewModel", "Flow completed. Navigate to dashboard or success screen.")
                    return@launch
                }
                
                // Validate response - if nextScreenId is null, flow has ended
                if (nextScreenId == null) {
                    android.util.Log.d("DynamicFormViewModel", "Flow ended - nextScreenId is null (applicationId: ${runtimeSession.applicationId})")
                    
                    // Clear RuntimeSession - flow is complete
                    saveRuntimeSessionToSavedState(null)
                    
                    _uiState.update {
                        it.copy(
                            runtimeSession = null,
                            isSubmitting = false,
                            isFlowCompleted = true, // Mark flow as completed - triggers navigation back to Home
                            // Legacy fields
                            applicationId = null,
                            flowId = null
                        )
                    }
                    
                    android.util.Log.d("DynamicFormViewModel", "Flow ended. Navigating back to Home.")
                    return@launch
                }
                
                // Verify applicationId matches (should always match, but check for safety)
                if (responseApplicationId != null && responseApplicationId.toLong() != runtimeSession.applicationId) {
                    android.util.Log.w("DynamicFormViewModel", "ApplicationId mismatch: expected ${runtimeSession.applicationId}, got $responseApplicationId")
                }
                
                // Update RuntimeSession with new currentScreenId
                val updatedRuntimeSession = runtimeSession.copy(currentScreenId = nextScreenId)
                
                // Extract flow context from screen config response (if available)
                val extractedProductCode = screenConfig.scope?.productCode ?: currentState.productCode
                val extractedPartnerCode = screenConfig.scope?.partnerCode?.takeIf { it.isNotBlank() } 
                    ?: partnerCode?.takeIf { it.isNotBlank() } 
                    ?: currentState.partnerCode?.takeIf { it.isNotBlank() } 
                    ?: "SAMASTA"
                val extractedBranchCode = screenConfig.scope?.branchCode ?: branchCode ?: currentState.branchCode
                
                // Store CURRENT screen's formData BEFORE moving to next screen
                // Update CURRENT screen's entry in stack with its formData (for back navigation)
                val currentScreenFormData = state.formData.toMap() // Create a copy to avoid reference issues
                android.util.Log.d("DynamicFormViewModel", "Storing formData for current screen: ${runtimeSession.currentScreenId}, fields: ${currentScreenFormData.keys}")
                
                // Find and update CURRENT screen's entry in stack with its formData
                val currentScreenIndex = _navigationStack.indexOfFirst { it.screenId == runtimeSession.currentScreenId }
                if (currentScreenIndex >= 0) {
                    // Update existing entry with formData
                    val currentEntry = _navigationStack[currentScreenIndex]
                    _navigationStack[currentScreenIndex] = currentEntry.copy(formData = currentScreenFormData)
                    android.util.Log.d("DynamicFormViewModel", "Updated stack entry for screen: ${runtimeSession.currentScreenId} with ${currentScreenFormData.size} fields")
                } else {
                    android.util.Log.w("DynamicFormViewModel", "Current screen ${runtimeSession.currentScreenId} not found in stack - this should not happen")
                }
                
                // Push next screen to navigation stack (for local back navigation)
                _navigationStack.add(
                    NavigationStackEntry(
                        screenId = nextScreenId,
                        screenConfig = screenConfig,
                        formData = null // Next screen starts with empty formData
                    )
                )
                
                // Load next screen config directly from response
                // Update RuntimeSession atomically with screen load
                // NO navigation needed - UI will recompose automatically when state updates
                loadScreenFromDto(
                    screenConfig, 
                    restoreData = null,
                    nextScreenId = null, // Don't set nextScreen - no navigation needed
                    applicationId = responseApplicationId ?: runtimeSession.applicationId.toInt(), // Use response applicationId or fallback to session
                    isLoadingFromResponse = false, // Not needed - no navigation to prevent
                    updatedRuntimeSession = updatedRuntimeSession // Update RuntimeSession atomically
                )
                
                // Update state with flow context (RuntimeSession already updated by loadScreenFromDto)
                _uiState.update { state ->
                    state.copy(
                        productCode = extractedProductCode ?: state.productCode,
                        partnerCode = extractedPartnerCode,
                        branchCode = extractedBranchCode ?: state.branchCode,
                        isSubmitting = false,
                        // Legacy fields for backward compatibility
                        applicationId = responseApplicationId ?: runtimeSession.applicationId.toInt(),
                        flowId = runtimeSession.flowId
                    )
                }
                
                android.util.Log.d("DynamicFormViewModel", "✅ SCREEN SUBMITTED - nextScreenId: $nextScreenId, applicationId: ${updatedRuntimeSession.applicationId}")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = e.message ?: "Failed to submit form"
                    )
                }
            }
        }
    }

    fun clearFirstErrorField() {
        _uiState.update { it.copy(firstErrorFieldId = null) }
    }
    
    fun clearNextScreen() {
        _uiState.update { it.copy(nextScreen = null) }
    }
    
    /**
     * Reset flow completion flag - called when navigating back to dashboard after flow completion
     * This allows starting a new flow
     */
    fun resetFlowCompletion() {
        _uiState.update { it.copy(isFlowCompleted = false) }
    }
    
    /* ---------------- MODAL HANDLING ---------------- */
    
    fun openModal(modalId: String) {
        _uiState.update { it.copy(openModalId = modalId) }
    }
    
    fun closeModal() {
        _uiState.update { it.copy(openModalId = null) }
    }
    
    fun updateHiddenField(fieldId: String, value: Any) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.toMutableMap().apply {
                    // Wrap value in { "value": ... } object
                    put(fieldId, mapOf("value" to value))
                }
            )
        }
    }
    
    fun checkAndTriggerVerification(fieldId: String, value: Any, sectionIndex: Int?) {
        val state = _uiState.value
        val formScreen = state.formScreen ?: return
        
        // Find field in sections and subsections recursively
        fun findFieldInAllSections(section: com.kaleidofin.originator.domain.model.FormSection): com.kaleidofin.originator.domain.model.FormField? {
            return section.fields.find { it.id == fieldId } 
                ?: section.subSections.firstOrNull()?.let { findFieldInAllSections(it) }
        }
        
        val field = formScreen.sections.firstNotNullOfOrNull { findFieldInAllSections(it) } ?: return
        val verification = field.verification ?: return
        
        // Check if verification should be triggered
        val shouldTrigger = when (verification.trigger) {
            "ON_COMPLETE" -> {
                // Trigger when field reaches maxLength or is complete
                if (field.maxLength != null && value is String) {
                    value.length == field.maxLength
                } else {
                    value is String && value.isNotBlank()
                }
            }
            "ON_BLUR" -> {
                // Trigger on blur - handled separately
                false
            }
            else -> false
        }
        
        if (shouldTrigger && verification.enabled) {
            openModal(verification.modalId)
        }
    }
    
    /* ---------------- REPEATABLE SECTIONS ---------------- */
    
    fun addSectionInstance(sectionId: String) {
        _uiState.update { currentState ->
            val currentCount = currentState.sectionInstances[sectionId] ?: 0
            val section = currentState.formScreen?.sections?.find { it.sectionId == sectionId }
                ?: currentState.formScreen?.sections?.flatMap { it.subSections }?.find { it.sectionId == sectionId }
            
            if (section != null && (section.maxInstances == null || currentCount < section.maxInstances)) {
                currentState.copy(
                    sectionInstances = currentState.sectionInstances.toMutableMap().apply {
                        put(sectionId, currentCount + 1)
                    }
                )
            } else {
                currentState
            }
        }
    }
    
    fun removeSectionInstance(sectionId: String) {
        _uiState.update { currentState ->
            val currentCount = currentState.sectionInstances[sectionId] ?: 0
            val section = currentState.formScreen?.sections?.find { it.sectionId == sectionId }
                ?: currentState.formScreen?.sections?.flatMap { it.subSections }?.find { it.sectionId == sectionId }
            
            if (section != null && currentCount > section.minInstances) {
                val instanceToRemove = currentCount - 1
                val fieldsToRemove = section.fields.map { "${it.id}_$instanceToRemove" }
                
                currentState.copy(
                    sectionInstances = currentState.sectionInstances.toMutableMap().apply {
                        put(sectionId, currentCount - 1)
                    },
                    formData = currentState.formData.toMutableMap().apply {
                        keys.removeAll(fieldsToRemove)
                    },
                    fieldErrors = currentState.fieldErrors.toMutableMap().apply {
                        keys.removeAll(fieldsToRemove)
                    }
                )
            } else {
                currentState
            }
        }
    }

    /* ---------------- FLOW STACK MANAGEMENT ---------------- */
    // Flow stack management removed - navigation is now backend-driven via /flow/back API

    
    // Store restore data temporarily for LaunchedEffect to pick up
    private var pendingRestoreData: Map<String, Any>? = null
    
    fun getPendingRestoreData(): Map<String, Any>? {
        val data = pendingRestoreData
        pendingRestoreData = null // Clear after reading
        return data
    }
    
    /**
     * Handle back navigation using local stack
     * Android manages local navigation; backend manages flow snapshot
     * 
     * Rules:
     * 1. If stack has previous screen -> pop and restore
     * 2. If at start screen -> exit flow (handled by UI)
     * 3. Backend snapshot allows editing previous screens
     */
    /**
     * Handle back navigation - restores previous screen from local navigation stack
     * NO navigation needed - screen updates via state and UI recomposes
     */
    fun handleBackNavigation(
        onExitFlow: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                // Check if we can go back (need at least 2 items in stack: previous + current)
                if (_navigationStack.size <= 1) {
                    // At start screen - exit flow
                    _navigationStack.clear()
                    _uiState.update { it.copy(isLoading = false) }
                    onExitFlow()
                    return@launch
                }
                
                // Pop current screen from stack
                _navigationStack.removeAt(_navigationStack.size - 1)
                
                // Get previous screen from stack
                val previousEntry = _navigationStack.last()
                
                android.util.Log.d("DynamicFormViewModel", "Navigating back to screen: ${previousEntry.screenId}, stored formData: ${previousEntry.formData?.keys ?: "null"}")
                
                // Update RuntimeSession with previous screen (if exists)
                val currentState = _uiState.value
                val updatedRuntimeSession = currentState.runtimeSession?.copy(currentScreenId = previousEntry.screenId)
                if (updatedRuntimeSession != null) {
                    saveRuntimeSessionToSavedState(updatedRuntimeSession)
                }
                
                // Load previous screen config (already resolved by backend during forward navigation)
                // Backend snapshot allows editing this screen again
                // Set flag to prevent LaunchedEffect from calling API
                _uiState.update { 
                    it.copy(
                        isRestoringFromBack = true,
                        runtimeSession = updatedRuntimeSession // Update RuntimeSession
                    )
                }
                
                // Restore previous screen with its formData - UI will recompose automatically
                loadScreenFromDto(
                    previousEntry.screenConfig, 
                    restoreData = previousEntry.formData, // Restore formData from navigation stack
                    updatedRuntimeSession = updatedRuntimeSession // Update RuntimeSession atomically
                )
                
                // Reset flag after a small delay
                kotlinx.coroutines.delay(100)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isRestoringFromBack = false // Reset flag
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to navigate back"
                    )
                }
                onError(e.message ?: "Failed to navigate back")
            }
        }
    }
    
    /**
     * Check if back navigation is possible
     * Returns true if there are previous screens in the stack
     */
    fun canNavigateBack(): Boolean {
        return _navigationStack.size > 1
    }
    
    /* ---------------- OTP VERIFICATION ---------------- */
    
    fun sendOtp(
        endpoint: String,
        method: String,
        phoneNumber: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // TODO: Replace with actual API call using endpoint and method
                // For now, simulate OTP send
                kotlinx.coroutines.delay(500) // Simulate network delay
                
                // Simulate success - in real implementation, make HTTP request
                // val response = httpClient.post(endpoint) { body = json { put("phoneNumber", phoneNumber) } }
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.message ?: "Failed to send OTP")
            }
        }
    }
    
    fun verifyOtp(
        endpoint: String,
        method: String,
        phoneNumber: String,
        otp: String,
        fieldId: String,
        fieldKey: String, // Field key with section index if applicable
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // TODO: Replace with actual API call using endpoint and method
                // For now, simulate OTP verification
                kotlinx.coroutines.delay(500) // Simulate network delay
                
                // Simulate success - in real implementation, make HTTP request
                // val response = httpClient.get(endpoint) { url { parameters.append("phoneNumber", phoneNumber); parameters.append("otp", otp) } }
                // if (response.status.isSuccess()) {
                //     updateFieldValue("${fieldId}_verified", true, null)
                //     onSuccess()
                // } else {
                //     onFailure("Invalid OTP")
                // }
                
                // For now, mark as verified on success
                updateFieldValue("${fieldKey}_verified", true, null)
                // Clear any verification error on success
                clearFieldError(fieldKey)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.message ?: "Failed to verify OTP")
            }
        }
    }
    
    /* ---------------- CAMERA CAPTURE ---------------- */
    
    suspend fun uploadImage(
        endpoint: String,
        method: String,
        imageBytes: ByteArray,
        mimeType: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("DynamicFormViewModel", "Uploading image to: $endpoint, method: $method, size: ${imageBytes.size} bytes")
            
            // Build full URL (endpoint might be absolute or relative)
            val fullUrl = if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
                endpoint
        } else {
                // Remove leading slash if present and combine with base URL
                val cleanEndpoint = endpoint.removePrefix("/")
                "${ApiConfig.BASE_URL.removeSuffix("/")}/$cleanEndpoint"
            }
            
            android.util.Log.d("DynamicFormViewModel", "Full upload URL: $fullUrl")
            
            // Create multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "image.jpg",
                    imageBytes.toRequestBody(mimeType.toMediaType())
                )
                .build()
            
            // Build request
            val requestBuilder = Request.Builder()
                .url(fullUrl)
                .post(requestBody)
            
            // Execute request on IO thread (already in withContext(Dispatchers.IO))
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            
            android.util.Log.d("DynamicFormViewModel", "Upload response code: ${response.code}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                android.util.Log.d("DynamicFormViewModel", "Upload response: $responseBody")
                
                // Parse response - expect JSON with fileId or url field
                // Common response formats:
                // {"fileId": "abc123"} or {"url": "https://..."} or {"data": {"fileId": "abc123"}}
                try {
                    val json = Gson().fromJson(responseBody, Map::class.java) as? Map<*, *>
                    val fileId = json?.get("fileId") as? String
                        ?: json?.get("url") as? String
                        ?: (json?.get("data") as? Map<*, *>)?.get("fileId") as? String
                        ?: (json?.get("data") as? Map<*, *>)?.get("url") as? String
                        ?: responseBody // Fallback to raw response
                    
                    android.util.Log.d("DynamicFormViewModel", "Upload successful, fileId: $fileId")
                    fileId?.toString()
                } catch (e: Exception) {
                    android.util.Log.e("DynamicFormViewModel", "Failed to parse upload response", e)
                    // If response is not JSON, treat entire response as fileId/URL
                    responseBody
                }
            } else {
                val errorBody = response.body?.string()
                android.util.Log.e("DynamicFormViewModel", "Upload failed: ${response.code}, $errorBody")
                throw Exception("Upload failed: ${response.code} - $errorBody")
            }
        } catch (e: Exception) {
            android.util.Log.e("DynamicFormViewModel", "Image upload error", e)
            null
        }
    }

    /* ---------------- WEBVIEW LAUNCH ---------------- */
    
    suspend fun getWebViewUrl(
        endpoint: String,
        method: String,
        responseUrlField: String? = null // Field name in response that contains URL (default: "url")
    ): String? = withContext(Dispatchers.IO) {
        // Fallback URL for testing purposes
        val fallbackUrl = "https://www.kaleidofin.com/"
        
        try {
            android.util.Log.d("DynamicFormViewModel", "Getting WebView URL from: $endpoint, method: $method")
            
            // Build full URL (endpoint might be absolute or relative)
            val fullUrl = if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
                endpoint
            } else {
                // Remove leading slash if present and combine with base URL
                val cleanEndpoint = endpoint.removePrefix("/")
                "${ApiConfig.BASE_URL.removeSuffix("/")}/$cleanEndpoint"
            }
            
            android.util.Log.d("DynamicFormViewModel", "Full WebView URL API: $fullUrl")
            
            // Get current form data for the request body
            val currentState = _uiState.value
            val formData = currentState.formData ?: emptyMap()
            
            // Build request body (JSON)
            val requestBody = gson.toJson(formData).toRequestBody("application/json".toMediaType())
            
            // Build request
            val requestBuilder = when (method.uppercase()) {
                "POST" -> Request.Builder().url(fullUrl).post(requestBody)
                "GET" -> Request.Builder().url(fullUrl).get()
                else -> Request.Builder().url(fullUrl).post(requestBody) // Default to POST
            }
            
            // Execute request
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            
            android.util.Log.d("DynamicFormViewModel", "WebView URL API response code: ${response.code}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                android.util.Log.d("DynamicFormViewModel", "WebView URL API response: $responseBody")
                
                // Parse response - expect JSON with url field
                // Use responseUrlField if provided, otherwise default to "url"
                // Common response formats:
                // {"url": "https://..."} or {"data": {"url": "https://..."}} or just a string URL
                val urlFieldName = responseUrlField ?: "url"
                try {
                    val json = gson.fromJson(responseBody, Map::class.java) as? Map<*, *>
                    val url = json?.get(urlFieldName) as? String
                        ?: (json?.get("data") as? Map<*, *>)?.get(urlFieldName) as? String
                        ?: json?.get("url") as? String // Fallback to "url" if responseUrlField not found
                        ?: (json?.get("data") as? Map<*, *>)?.get("url") as? String // Fallback to "data.url"
                        ?: responseBody?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                        ?: responseBody?.trim()?.takeIf { it.isNotBlank() }
                    
                    if (url != null && url.isNotBlank()) {
                        android.util.Log.d("DynamicFormViewModel", "WebView URL extracted: $url")
                        url
        } else {
                        android.util.Log.w("DynamicFormViewModel", "No URL found in response: $responseBody, using fallback URL")
                        fallbackUrl
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DynamicFormViewModel", "Failed to parse WebView URL response", e)
                    // If response is not JSON, treat entire response as URL if it looks like one
                    val url = responseBody?.takeIf { 
                        it.startsWith("http://") || it.startsWith("https://") 
                    }?.trim()
                    
                    if (url != null && url.isNotBlank()) {
                        url
                    } else {
                        android.util.Log.w("DynamicFormViewModel", "Response is not a valid URL, using fallback URL")
                        fallbackUrl
                    }
                }
            } else {
                val errorBody = response.body?.string()
                android.util.Log.w("DynamicFormViewModel", "WebView URL API failed: ${response.code}, $errorBody. Using fallback URL for testing.")
                // Return fallback URL instead of throwing exception
                fallbackUrl
            }
        } catch (e: Exception) {
            android.util.Log.w("DynamicFormViewModel", "WebView URL API call error: ${e.message}. Using fallback URL for testing.", e)
            // Return fallback URL instead of null for testing purposes
            fallbackUrl
        }
    }
    
    /* ---------------- API VERIFICATION ---------------- */
    
    fun verifyApi(
        endpoint: String,
        method: String,
        requestMapping: String?,
        fieldValue: String,
        fieldId: String,
        fieldKey: String, // Field key with section index if applicable
        successCondition: com.kaleidofin.originator.domain.model.ApiVerificationSuccessCondition?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // TODO: Replace with actual API call using endpoint and method
                // For now, simulate API verification
                kotlinx.coroutines.delay(500) // Simulate network delay
                
                // Build request body from requestMapping if provided
                // requestMapping format: {"pan":"{{panValue}}"}
                // Replace {{fieldId}} or {{fieldValue}} with actual value
                val requestBody = requestMapping?.replace("{{${fieldId}Value}}", fieldValue)
                    ?.replace("{{${fieldId}}}", fieldValue)
                    ?: "{}"
                
                // Simulate API response - in real implementation, make HTTP request
                // val response = when (method.uppercase()) {
                //     "GET" -> httpClient.get(endpoint) { url { parameters.append(fieldId, fieldValue) } }
                //     "POST" -> httpClient.post(endpoint) { body = requestBody }
                //     else -> throw IllegalArgumentException("Unsupported method: $method")
                // }
                // 
                // val responseData = response.bodyAsText()
                // val jsonResponse = Json.parseToJsonElement(responseData).jsonObject
                // 
                // Check success condition
                // val isSuccess = successCondition?.let { condition ->
                //     val fieldValue = jsonResponse[condition.field]?.asString
                //     fieldValue == condition.equals
                // } ?: response.status.isSuccess()
                
                // For now, simulate success - in real implementation, check actual response
                // if (isSuccess) {
                //     updateFieldValue("${fieldId}_verified", true, null)
                //     onSuccess()
                // } else {
                //     onFailure("Verification failed")
                // }
                
                // Simulate success for now
                updateFieldValue("${fieldKey}_verified", true, null)
                // Clear any verification error on success
                clearFieldError(fieldKey)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.message ?: "Failed to verify")
            }
        }
    }
    
    /* ---------------- FIELD ERROR MANAGEMENT ---------------- */
    
    fun setFieldError(fieldKey: String, errorMessage: String) {
        _uiState.update { state ->
            val newErrors = state.fieldErrors.toMutableMap()
            newErrors[fieldKey] = errorMessage
            state.copy(fieldErrors = newErrors)
        }
    }
    
    fun clearFieldError(fieldKey: String) {
        _uiState.update { state ->
            val newErrors = state.fieldErrors.toMutableMap()
            newErrors.remove(fieldKey)
            state.copy(fieldErrors = newErrors)
        }
    }
    
    /**
     * Decode Aadhaar QR using backend API
     * 
     * @param qrDataString Numeric QR string (Base10)
     * @return Result containing decoded Aadhaar data or error
     */
    suspend fun decodeAadhaarQR(qrDataString: String): Result<com.kaleidofin.originator.data.dto.AadhaarDecodeResponseDto> {
        return try {
            android.util.Log.d("DynamicFormViewModel", "Sending Aadhaar QR payload to backend, length = ${qrDataString.length}")
            val result = formDataSource.decodeAadhaarQR(qrDataString)
            
            result.onSuccess { response ->
                android.util.Log.d("DynamicFormViewModel", "Aadhaar decode API success: name=${response.name != null}, gender=${response.gender != null}, dob=${response.dob != null}, aadhaarLast4=${response.aadhaarLast4 != null}")
            }.onFailure { error ->
                android.util.Log.e("DynamicFormViewModel", "Aadhaar decode API failed: ${error.message}", error)
            }
            
            result
        } catch (e: Exception) {
            android.util.Log.e("DynamicFormViewModel", "Aadhaar decode API exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
