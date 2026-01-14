package com.kaleidofin.originator.presentation.ui.state

import com.kaleidofin.originator.domain.model.DependencyCondition
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
    val firstErrorFieldId: String? = null, // Field ID to scroll to on validation error
    // Flow context - stored when flow starts, used for all subsequent API calls
    val flowId: String? = null,
    val productCode: String? = null,
    val partnerCode: String? = null,
    val branchCode: String? = null
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
                    val isFieldEnabled = field.enabledWhen?.let { condition ->
                        evaluateDependencyCondition(condition, if (section.repeatable) index else null)
                    } ?: true // Field is always enabled if no enabledWhen conditions
                    
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
     * Evaluate a dependency condition (for visibleWhen, enabledWhen, requiredWhen).
     * Supports both simple conditions and condition groups with AND/OR logic.
     * Also supports SINGLE and MULTIPLE select dropdowns.
     */
    fun evaluateDependencyCondition(condition: com.kaleidofin.originator.domain.model.DependencyCondition, sectionIndex: Int? = null): Boolean {
        return when (condition) {
            is com.kaleidofin.originator.domain.model.DependencyCondition.Condition -> {
                evaluateSimpleCondition(condition, sectionIndex)
            }
            is com.kaleidofin.originator.domain.model.DependencyCondition.ConditionGroup -> {
                evaluateConditionGroup(condition, sectionIndex)
            }
        }
    }
    
    /**
     * Evaluate a condition group with AND/OR logic.
     */
    private fun evaluateConditionGroup(group: com.kaleidofin.originator.domain.model.DependencyCondition.ConditionGroup, sectionIndex: Int? = null): Boolean {
        val results = group.conditions.map { evaluateDependencyCondition(it, sectionIndex) }
        return when (group.operator.uppercase()) {
            "AND" -> results.all { it }
            "OR" -> results.any { it }
            else -> results.all { it } // Default to AND
        }
    }
    
    /**
     * Evaluate a simple condition (field operator value).
     * Supports both SINGLE and MULTIPLE select dropdowns.
     */
    private fun evaluateSimpleCondition(condition: com.kaleidofin.originator.domain.model.DependencyCondition.Condition, sectionIndex: Int? = null): Boolean {
        val fieldId = condition.field
        val actualFieldId = if (sectionIndex != null) "${fieldId}_$sectionIndex" else fieldId
        val rawValue = formData[actualFieldId]
        val unwrappedValue = unwrapValue(rawValue)
        val actualValue = unwrappedValue?.toString()?.trim() ?: ""
        val expectedValue = condition.value
        
        // Check if this is a multi-select value (comma-separated string)
        // For multi-select dropdowns, value is stored as "VALUE1,VALUE2,VALUE3"
        val isMultiSelectValue = actualValue.contains(",")
        val actualValuesList = if (isMultiSelectValue) {
            actualValue.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            listOf(actualValue).filter { it.isNotBlank() }
        }
        
        return when (condition.operator.uppercase()) {
            "EQUALS", "==" -> {
                when {
                    expectedValue is Boolean -> unwrappedValue == expectedValue
                    expectedValue is String -> {
                        val expectedStr = expectedValue.toString().trim()
                        if (isMultiSelectValue) {
                            // For multi-select: TRUE if list CONTAINS the expected value
                            actualValuesList.any { it.equals(expectedStr, ignoreCase = true) }
                        } else {
                            // For single-select: exact match
                            actualValue.equals(expectedStr, ignoreCase = true)
                        }
                    }
                    else -> {
                        if (isMultiSelectValue) {
                            actualValuesList.any { it == expectedValue.toString() }
                        } else {
                            actualValue == expectedValue.toString()
                        }
                    }
                }
            }
            "NOT_EQUALS", "!=" -> {
                // If field is empty/null, disable dependent field
                if (actualValue.isBlank()) {
                    return false
                }
                when {
                    expectedValue is Boolean -> unwrappedValue != expectedValue
                    expectedValue is String -> {
                        val expectedStr = expectedValue.toString().trim()
                        if (isMultiSelectValue) {
                            // For multi-select: TRUE if list does NOT contain the expected value
                            !actualValuesList.any { it.equals(expectedStr, ignoreCase = true) }
                        } else {
                            // For single-select: not equal
                            !actualValue.equals(expectedStr, ignoreCase = true)
                        }
                    }
                    else -> {
                        if (isMultiSelectValue) {
                            !actualValuesList.any { it == expectedValue.toString() }
                        } else {
                            actualValue != expectedValue.toString()
                        }
                    }
                }
            }
            "IN" -> {
                when {
                    expectedValue is List<*> -> {
                        val expectedValues = expectedValue.mapNotNull { it?.toString()?.trim() }
                        if (isMultiSelectValue) {
                            // For multi-select: TRUE if ANY selected value matches
                            actualValuesList.any { selectedValue ->
                                expectedValues.any { it.equals(selectedValue, ignoreCase = true) }
                            }
                        } else {
                            // For single-select: check if actual value is in expected list
                        expectedValues.any { it.equals(actualValue, ignoreCase = true) }
                        }
                    }
                    expectedValue is String -> {
                        // Handle comma-separated string like "Voter Id,Passport,None"
                        val expectedValues = expectedValue.split(",").map { it.trim() }
                        if (isMultiSelectValue) {
                            // For multi-select: TRUE if ANY selected value matches
                            actualValuesList.any { selectedValue ->
                                expectedValues.any { it.equals(selectedValue, ignoreCase = true) }
                            }
                        } else {
                            // For single-select: check if actual value is in expected list
                        expectedValues.any { it.equals(actualValue, ignoreCase = true) }
                        }
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
                        if (isMultiSelectValue) {
                            // For multi-select: TRUE if NONE of selected values match
                            !actualValuesList.any { selectedValue ->
                                expectedValues.any { it.equals(selectedValue, ignoreCase = true) }
                            }
                        } else {
                            // For single-select: check if actual value is NOT in expected list
                        !expectedValues.any { it.equals(actualValue, ignoreCase = true) }
                        }
                    }
                    expectedValue is String -> {
                        // Handle comma-separated string like "None"
                        val expectedValues = expectedValue.split(",").map { it.trim() }
                        if (isMultiSelectValue) {
                            // For multi-select: TRUE if NONE of selected values match
                            !actualValuesList.any { selectedValue ->
                                expectedValues.any { it.equals(selectedValue, ignoreCase = true) }
                            }
                        } else {
                            // For single-select: check if actual value is NOT in expected list
                        !expectedValues.any { it.equals(actualValue, ignoreCase = true) }
                        }
                    }
                    else -> true
                }
            }
            "EXISTS" -> {
                // TRUE if value is not empty
                actualValue.isNotBlank()
            }
            "NOT_EXISTS" -> {
                // TRUE if value is empty
                actualValue.isBlank()
            }
            "GREATER_THAN", ">" -> {
                if (expectedValue == null) return false
                try {
                    val actualNum = actualValue.toDoubleOrNull()
                    val expectedNum = when (expectedValue) {
                        is Number -> expectedValue.toDouble()
                        is String -> expectedValue.toDoubleOrNull()
                        else -> null
                    }
                    actualNum != null && expectedNum != null && actualNum > expectedNum
                } catch (e: Exception) {
                    false
                }
            }
            "LESS_THAN", "<" -> {
                if (expectedValue == null) return false
                try {
                    val actualNum = actualValue.toDoubleOrNull()
                    val expectedNum = when (expectedValue) {
                        is Number -> expectedValue.toDouble()
                        is String -> expectedValue.toDoubleOrNull()
                        else -> null
                    }
                    actualNum != null && expectedNum != null && actualNum < expectedNum
                } catch (e: Exception) {
                    false
                }
            }
            else -> true
        }
    }
    
    /**
     * Legacy method for backward compatibility.
     * @deprecated Use evaluateDependencyCondition instead
     */
    @Deprecated("Use evaluateDependencyCondition instead", ReplaceWith("evaluateDependencyCondition(condition, sectionIndex)"))
    fun evaluateEnabledCondition(condition: com.kaleidofin.originator.domain.model.DependencyCondition.Condition, sectionIndex: Int? = null): Boolean {
        // This is now just a wrapper around evaluateSimpleCondition
        return evaluateSimpleCondition(condition, sectionIndex)
    }
}

