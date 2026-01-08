package com.kaleidofin.originator.presentation.ui.state

import com.kaleidofin.originator.domain.model.EnabledCondition
import com.kaleidofin.originator.domain.model.FormScreen
import com.kaleidofin.originator.domain.model.FormSection
import com.kaleidofin.originator.domain.model.SubmitCondition

data class DynamicFormUiState(
    val formScreen: FormScreen? = null,
    val formData: Map<String, Any> = emptyMap(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val sectionInstances: Map<String, Int> = emptyMap(), // sectionId -> instance count
    val masterData: Map<String, List<String>> = emptyMap(), // dataSource key -> list of options
    val inlineData: Map<String, List<String>> = emptyMap(), // For INLINE dataSource
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val openModalId: String? = null,
    val nextScreen: String? = null,
    val firstErrorFieldId: String? = null // Field ID to scroll to on validation error
) {
    /**
     * Helper function to wrap a value in { "value": ... } object
     */
    private fun wrapValue(value: Any?): Any {
        return if (value == null) {
            mapOf("value" to null)
        } else {
            mapOf("value" to value)
        }
    }
    
    /**
     * Helper function to unwrap a value from { "value": ... } object
     * Supports both wrapped format { "value": ... } and direct value for backward compatibility
     */
    private fun unwrapValue(wrapped: Any?): Any? {
        return when (wrapped) {
            null -> null
            is Map<*, *> -> {
                // Check if it's a wrapped value object
                if (wrapped.containsKey("value")) {
                    wrapped["value"]
                } else {
                    // Not wrapped, return as-is (backward compatibility)
                    wrapped
                }
            }
            else -> wrapped // Direct value (backward compatibility)
        }
    }
    
    fun getFieldValue(fieldId: String): Any? {
        val rawValue = formData[fieldId]
        return unwrapValue(rawValue)
    }
    
    fun getFieldError(fieldId: String): String? = fieldErrors[fieldId]
    
    fun hasError(fieldId: String): Boolean = fieldErrors.containsKey(fieldId)
    
    fun isFormValid(formScreen: FormScreen?): Boolean {
        if (formScreen == null) return false
        
        var isValid = true
        
        fun validateSection(section: FormSection) {
            if (!isValid) return // Early exit if already invalid
            
            val instanceCount = if (section.repeatable) {
                sectionInstances[section.sectionId] ?: section.minInstances
            } else {
                1
            }
            
            // Check all instances of this section
            repeat(instanceCount) { index ->
                section.fields.forEach { field ->
                    if (!isValid) return@forEach // Early exit if already invalid
                    
                    // Check if field is enabled based on enabledWhen conditions
                    val isFieldEnabled = if (field.enabledWhen.isNotEmpty()) {
                        field.enabledWhen.all { condition ->
                            evaluateEnabledCondition(condition, if (section.repeatable) index else null)
                        }
                    } else {
                        true // Field is always enabled if no enabledWhen conditions
                    }
                    
                    // Only validate if field is enabled
                    if (isFieldEnabled && field.required) {
                        val fieldKey = if (section.repeatable) "${field.id}_$index" else field.id
                        val value = unwrapValue(formData[fieldKey])
                        if (value == null || (value is String && value.isBlank())) {
                            isValid = false
                            return@forEach
                        }
                        
                        // Check regex validation for non-empty values
                        if (field.validation != null && value is String && value.isNotBlank()) {
                            val regex = field.validation.regex.toRegex()
                            if (!regex.matches(value)) {
                                isValid = false
                                return@forEach
                            }
                        }
                    }
                }
            }
            
            // Validate subsections recursively
            section.subSections.forEach { validateSection(it) }
        }
        
        formScreen.sections.forEach { validateSection(it) }
        
        // If field validation failed, return false
        if (!isValid) {
            return false
        }
        
        // Check enableSubmitWhen conditions
        formScreen.layout.enableSubmitWhen.forEach { condition ->
            if (!evaluateSubmitCondition(condition)) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Evaluate SubmitCondition (for enableSubmitWhen)
     */
    fun evaluateSubmitCondition(condition: SubmitCondition): Boolean {
        return when (condition.type) {
            "ALL_FIELDS_VALID" -> {
                // All fields are valid (no errors)
                fieldErrors.isEmpty() && isFormValid(formScreen)
            }
            "FIELD_EQUALS" -> {
                if (condition.field == null) return true
                val fieldValue = unwrapValue(formData[condition.field])
                val expectedValue = condition.value
                
                when {
                    expectedValue is Boolean -> fieldValue == expectedValue
                    expectedValue is String -> fieldValue?.toString()?.equals(expectedValue, ignoreCase = true) == true
                    else -> fieldValue == expectedValue
                }
            }
            else -> true
        }
    }
    
    /**
     * Evaluate EnabledCondition (for enabledWhen)
     */
    fun evaluateEnabledCondition(condition: EnabledCondition, sectionIndex: Int? = null): Boolean {
        val fieldId = condition.field
        val actualFieldId = if (sectionIndex != null) "${fieldId}_$sectionIndex" else fieldId
        val rawValue = formData[actualFieldId]
        val unwrappedValue = unwrapValue(rawValue)
        val actualValue = unwrappedValue?.toString()?.trim() ?: ""
        val expectedValue = condition.value
        
        return when (condition.operator.uppercase()) {
            "EQUALS", "==" -> {
                when {
                    expectedValue is Boolean -> unwrappedValue == expectedValue
                    expectedValue is String -> actualValue.equals(expectedValue.toString().trim(), ignoreCase = true)
                    else -> actualValue == expectedValue.toString()
                }
            }
            "NOT_EQUALS", "!=" -> {
                // If field is empty/null, disable dependent field
                if (actualValue.isBlank()) {
                    return false
                }
                when {
                    expectedValue is Boolean -> unwrappedValue != expectedValue
                    expectedValue is String -> !actualValue.equals(expectedValue.toString().trim(), ignoreCase = true)
                    else -> actualValue != expectedValue.toString()
                }
            }
            "IN" -> {
                when {
                    expectedValue is List<*> -> {
                        val expectedValues = expectedValue.mapNotNull { it?.toString()?.trim() }
                        expectedValues.any { it.equals(actualValue, ignoreCase = true) }
                    }
                    expectedValue is String -> {
                        // Handle comma-separated string like "Voter Id,Passport,None"
                        val expectedValues = expectedValue.split(",").map { it.trim() }
                        expectedValues.any { it.equals(actualValue, ignoreCase = true) }
                    }
                    else -> false
                }
            }
            "NOT_IN" -> {
                // If field is empty/null, disable dependent field
                if (actualValue.isBlank()) {
                    return false
                }
                when {
                    expectedValue is List<*> -> {
                        val expectedValues = expectedValue.mapNotNull { it?.toString()?.trim() }
                        !expectedValues.any { it.equals(actualValue, ignoreCase = true) }
                    }
                    expectedValue is String -> {
                        // Handle comma-separated string like "None"
                        val expectedValues = expectedValue.split(",").map { it.trim() }
                        !expectedValues.any { it.equals(actualValue, ignoreCase = true) }
                    }
                    else -> true
                }
            }
            else -> true
        }
    }
}

