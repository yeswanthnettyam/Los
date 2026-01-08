package com.kaleidofin.originator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaleidofin.originator.domain.model.FormField
import com.kaleidofin.originator.domain.usecase.GetFormConfigurationUseCase
import com.kaleidofin.originator.domain.usecase.GetMasterDataUseCase
import com.kaleidofin.originator.presentation.ui.state.DynamicFormUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.kaleidofin.originator.data.datasource.FormDataSource

data class FlowStep(
    val screenId: String,
    val flowId: String,
    val formDataSnapshot: Map<String, Any>
)

@HiltViewModel
class DynamicFormViewModel @Inject constructor(
    private val getFormConfigurationUseCase: GetFormConfigurationUseCase,
    private val getMasterDataUseCase: GetMasterDataUseCase,
    private val formDataSource: FormDataSource // For testing: Update dummy JSON
) : ViewModel() {

    private val _uiState = MutableStateFlow(DynamicFormUiState())
    val uiState: StateFlow<DynamicFormUiState> = _uiState.asStateFlow()

    private val _flowStack = MutableStateFlow<List<FlowStep>>(emptyList())
    val flowStack: StateFlow<List<FlowStep>> = _flowStack.asStateFlow()

    /* ---------------- LOAD ---------------- */

    fun loadFormConfiguration(target: String, isInitialLoad: Boolean = false, restoreData: Map<String, Any>? = null) {
        viewModelScope.launch {
            // Reset state when loading a new configuration
            _uiState.update { 
                DynamicFormUiState(
                    isLoading = true,
                    error = null
                )
            }

            val formScreen = getFormConfigurationUseCase(target)
            
            // Push current screen to flow stack if not initial load and navigating forward (not restoring)
            // Check if target is different from current screen to avoid pushing when restoring
            val currentScreenId = _uiState.value.formScreen?.screenId
            if (!isInitialLoad && currentScreenId != null && target != currentScreenId && restoreData == null) {
                val currentScreen = _uiState.value.formScreen!!
                pushScreen(
                    screenId = currentScreen.screenId,
                    flowId = currentScreen.flowId,
                    formData = _uiState.value.formData
                )
            }

            val initialData = mutableMapOf<String, Any>()
            formScreen.hiddenFields.forEach { field ->
                initialData[field.id] = field.defaultValue ?: when (field.type) {
                    "BOOLEAN" -> false
                    "TEXT" -> ""
                    "NUMBER" -> 0
                    else -> ""
                }
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
                    // Initialize field value from JSON if present
                    val fieldKey = if (sectionIndex != null) "${field.id}_$sectionIndex" else field.id
                    if (field.value != null) {
                        initialData[fieldKey] = field.value
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
                                // For now, we can preload if needed
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

            // If restoreData is provided, merge it with initial data
            val finalFormData = if (restoreData != null) {
                val mergedData = initialData.toMutableMap()
                mergedData.putAll(restoreData)
                mergedData
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
                    fieldErrors = emptyMap() // Clear errors when loading/restoring
                )
            }
            
            // Initialize flow stack with first screen if empty
            if (_flowStack.value.isEmpty()) {
                pushScreen(
                    screenId = formScreen.screenId,
                    flowId = formScreen.flowId,
                    formData = finalFormData
                )
            }
        }
    }

    /* ---------------- FIELD UPDATE ---------------- */

    fun updateFieldValue(fieldId: String, value: Any, sectionIndex: Int?) {
        val finalId = sectionIndex?.let { "${fieldId}_$it" } ?: fieldId
        val state = _uiState.value
        val formScreen = state.formScreen

        android.util.Log.d("DynamicFormViewModel", "Updating field: $finalId (base fieldId: $fieldId, sectionIndex: $sectionIndex) with value: '$value'")

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
        
        // Check if value actually changed
        val oldValue = state.formData[finalId]
        val valueChanged = oldValue?.toString() != value.toString()
        
        android.util.Log.d("DynamicFormViewModel", "Value changed: $valueChanged (old: '$oldValue', new: '$value')")
        
        // Determine if this is a verified input field - apply same logic for VERIFIED_INPUT and API_VERIFICATION
        val isVerifiedInputField = field?.type == "VERIFIED_INPUT" || field?.type == "API_VERIFICATION"
        // Use finalId to handle section indices correctly (e.g., "fieldId_0_verified")
        // Both VERIFIED_INPUT and API_VERIFICATION use the same verification status key format
        val verifiedStatusField = if (isVerifiedInputField) "${finalId}_verified" else null
        
        android.util.Log.d("DynamicFormViewModel", "Field type check - isVerifiedInputField: $isVerifiedInputField, verifiedStatusField: $verifiedStatusField")

        _uiState.update { currentState ->
            val newFormData = currentState.formData.toMutableMap().apply {
                put(finalId, value)
                
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
                            if (dependentField.enabledWhen.isNotEmpty()) {
                                val dependentFieldKey = if (section.repeatable) "${dependentField.id}_$index" else dependentField.id
                                
                                // Check if dependent field should be enabled with new value
                                val tempState = currentState.copy(formData = newFormData)
                                val isEnabled = dependentField.enabledWhen.all { condition ->
                                    tempState.evaluateEnabledCondition(condition, if (section.repeatable) index else null)
                                }
                                
                                // If field becomes disabled, clear its error and value
                                val wasEnabled = dependentField.enabledWhen.all { condition ->
                                    currentState.evaluateEnabledCondition(condition, if (section.repeatable) index else null)
                                }
                                
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
            val currentValue = state.formData[finalId] ?: value
            
            // Validate using current state
            val isFieldEnabled = if (field.enabledWhen.isNotEmpty()) {
                field.enabledWhen.all { condition ->
                    state.evaluateEnabledCondition(condition, sectionIndex)
                }
            } else {
                true
            }
            
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
                    // Check length constraints first
                    when {
                        field.min != null && currentValue.length < field.min -> "${field.label} must be at least ${field.min} characters"
                        field.max != null && currentValue.length > field.max -> "${field.label} must be at most ${field.max} characters"
                        !currentValue.all { it.isDigit() } -> "${field.label} must be a number"
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
        val isFieldEnabled = if (field.enabledWhen.isNotEmpty()) {
            field.enabledWhen.all { condition ->
                state.evaluateEnabledCondition(condition, sectionIndex)
            }
        } else {
            true // Field is always enabled if no enabledWhen conditions
        }
        
        // Skip validation if field is disabled
        if (!isFieldEnabled) {
            return null // No error for disabled fields
        }

        // Required validation - only check if field is enabled
        if (field.required && (value == null || (value is String && value.isBlank())))
            return "${field.label} is required"

        if (field.validation != null && value is String && value.isNotBlank()) {
            if (!field.validation.regex.toRegex().matches(value))
                return field.validation.errorMessage
        }

        if (field.type == "NUMBER" && value is String && value.isNotBlank()) {
            // For NUMBER fields, min/max are used for length constraints
            // Check length constraints first
            field.min?.let { minLength ->
                if (value.length < minLength) {
                    return "${field.label} must be at least $minLength characters"
                }
            }
            field.max?.let { maxLength ->
                if (value.length > maxLength) {
                    return "${field.label} must be at most $maxLength characters"
                }
            }
            
            // Then check if it's a valid number (only digits)
            if (!value.all { it.isDigit() }) {
                return "${field.label} must be a number"
            }
            
            // Convert to number for potential numeric value validation
            val num = value.toIntOrNull()
            if (num == null && value.isNotEmpty()) {
                return "${field.label} must be a valid number"
            }
        }
        
        // Validate maxLength for text fields (including VERIFIED_INPUT and API_VERIFICATION)
        if (value is String && value.isNotBlank() && field.maxLength != null) {
            if (value.length > field.maxLength) {
                return "${field.label} must be at most ${field.maxLength} characters"
            }
        }
        
        // Validate min/max length for string fields (non-NUMBER types)
        if (field.type != "NUMBER" && value is String && value.isNotBlank()) {
            field.min?.let { minLength ->
                if (value.length < minLength) {
                    return "${field.label} must be at least $minLength characters"
                }
            }
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
                    val value = state.formData[key]

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

        // ✅ success flow - determine next screen based on form data (conditional navigation)
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            kotlinx.coroutines.delay(800)
            
            // Update dummy JSON with form data for testing
            formDataSource.updateFormData(screen.screenId, state.formData.filterValues { it != null }.mapValues { it.value!! })
            
            // Determine next screen based on form data for conditional navigation
            // Example: Identity → Business (if salaried) or Identity → Employment (if self-employed)
            val nextScreen = determineNextScreen(screen, state.formData)
            
            _uiState.update {
                it.copy(isSubmitting = false, nextScreen = nextScreen)
            }
        }
    }
    
    private fun determineNextScreen(screen: com.kaleidofin.originator.domain.model.FormScreen, formData: Map<String, Any?>): String? {
        // Get the default next screen from actions
        val defaultNextScreen = screen.actions.firstOrNull()?.nextScreen
        
            return defaultNextScreen;
    }

    fun clearFirstErrorField() {
        _uiState.update { it.copy(firstErrorFieldId = null) }
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
                    put(fieldId, value)
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

    fun pushScreen(
        screenId: String,
        flowId: String,
        formData: Map<String, Any>
    ) {
        _flowStack.update { currentStack ->
            currentStack + FlowStep(
                screenId = screenId,
                flowId = flowId,
                formDataSnapshot = formData.toMap()
            )
        }
    }

    fun popScreen(): FlowStep? {
        val currentStack = _flowStack.value
        return if (currentStack.size > 1) {
            // Remove the last item (current screen) and return the previous one
            val newStack = currentStack.dropLast(1)
            val previous = currentStack[currentStack.size - 2]
            _flowStack.value = newStack
            previous
        } else {
            null
        }
    }

    
    // Store restore data temporarily for LaunchedEffect to pick up
    private var pendingRestoreData: Map<String, Any>? = null
    
    fun getPendingRestoreData(): Map<String, Any>? {
        val data = pendingRestoreData
        pendingRestoreData = null // Clear after reading
        return data
    }
    
    fun handleBackNavigation(onNavigateToScreen: (String) -> Unit, onExitFlow: () -> Unit) {
        val previous = popScreen()
        if (previous != null) {
            // Store restore data for LaunchedEffect to use
            pendingRestoreData = previous.formDataSnapshot
            // Navigate to previous screen's route - this will trigger LaunchedEffect(target) to load configuration
            onNavigateToScreen(previous.screenId)
        } else {
            // Exit flow - navigate back using navigation controller
            onExitFlow()
        }
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
}
