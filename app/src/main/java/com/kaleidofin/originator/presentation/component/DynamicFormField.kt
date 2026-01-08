package com.kaleidofin.originator.presentation.component

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaleidofin.originator.domain.model.FormField
import com.kaleidofin.originator.presentation.ui.state.DynamicFormUiState

@Composable
fun DynamicFormField(
    field: FormField,
    value: Any?,
    error: String?,
    onValueChange: (String) -> Unit,
    onBlur: () -> Unit,
    onVerificationClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isVerified: Boolean = false // New parameter to check verification status
) {
    // Convert value to string, handling null and empty cases
    // IMPORTANT: Don't use remember here - we want it to update immediately when value prop changes
    // The value prop should trigger recomposition when state updates
    val textValue = when (value) {
        null -> ""
        is String -> value
        else -> value.toString()
    }
    
    // Debug log to verify value is being received and updated
    LaunchedEffect(value) {
        if (field.type == "TEXTAREA" || field.id.contains("phone") || field.id.contains("address")) {
            Log.d("DynamicFormField", "Field ${field.id} value prop changed to: '$value' (type: ${value?.javaClass?.simpleName}), textValue: '$textValue'")
        }
    }

    val keyboardType = when (field.keyboard) {
        "PHONE" -> KeyboardType.Phone
        "NUMBER" -> KeyboardType.Number
        "EMAIL" -> KeyboardType.Email
        else -> KeyboardType.Text
    }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            // newValue contains the FULL text including what user just typed
            // Filter for NUMBER keyboard
            val filtered = if (field.keyboard == "NUMBER") {
                newValue.filter { it.isDigit() }
            } else {
                newValue
            }

            // Apply maxLength constraint
            val finalValue = if (field.maxLength != null && filtered.length > field.maxLength) {
                filtered.take(field.maxLength)
            } else {
                filtered
            }
            
            // Debug log
            Log.d("DynamicFormField", "Field: ${field.id}, Current textValue: '$textValue', New value from TextField: '$newValue', Final: '$finalValue'")
            
            // Always call onValueChange with the final value
            // This triggers state update in ViewModel
            onValueChange(finalValue)
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(field.label + if (field.required) " *" else "") },
        placeholder = { Text(field.placeholder ?: "") },
        singleLine = field.type != "TEXTAREA",
        maxLines = if (field.type == "TEXTAREA") 10 else 1,
        minLines = if (field.type == "TEXTAREA") 3 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        isError = error != null,
        enabled = isEnabled,
        readOnly = field.readOnly, // Use readOnly from field definition
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            errorIndicatorColor = Color(0xFFB00020)
        ),
        trailingIcon = {
            when {
                error != null -> {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Error",
                        tint = Color(0xFFB00020)
                    )
                }
                field.verification?.showStatusIcon == true && isVerified -> {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color(0xFF16A34A)
                    )
                }
                else -> null
            }
        },
        supportingText = error?.let {
            { Text(it, color = Color(0xFFB00020)) }
        }
    )

    // 👇 Trigger blur validation only when field loses focus (not on every change)
    // Removed auto-blur on value change to prevent interference with text input
}


@Composable
fun DynamicDropdownField(
    field: FormField,
    value: Any?,
    error: String?,
    options: List<String>,
    onValueChange: (String) -> Unit,
    onBlur: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = value?.toString() ?: ""
    val interactionSource = remember { MutableInteractionSource() }

    Log.d("DynamicDropdownField", "Field: ${field.id}, Selected: '$selected', Options count: ${options.size}, Options: $options")

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            enabled = isEnabled,
            label = { Text(field.label + if (field.required) " *" else "") },
            placeholder = { Text(field.placeholder ?: "Select ${field.label}") },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            },
            isError = error != null,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = isEnabled) {
                    if (isEnabled) {
                        expanded = true
                    }
                },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                errorIndicatorColor = Color(0xFFB00020)
            ),
            supportingText = error?.let {
                { Text(it, color = Color(0xFFB00020)) }
            }
        )

        DropdownMenu(
            expanded = expanded && isEnabled,
            onDismissRequest = {
                expanded = false
                onBlur()
            },
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No options available") },
                    onClick = { expanded = false }
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            Log.d("DynamicDropdownField", "Selected option: '$option' for field: ${field.id}")
                            expanded = false
                            // Update value first - this clears error in ViewModel
                            onValueChange(option)
                            // Call blur to trigger validation with updated state
                            onBlur()
                        }
                    )
                }
            }
        }

        // Make TextField taps reliably open the dropdown
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release && isEnabled) {
                    expanded = true
                }
            }
        }
    }
}

@Composable
fun DynamicDateField(
    field: FormField,
    value: Any?,
    error: String?,
    isEnabled: Boolean,
    onDateClick: () -> Unit,
    onBlur: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    OutlinedTextField(
        value = value?.toString() ?: "",
        onValueChange = {},
        readOnly = true,
        enabled = isEnabled,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (!it.isFocused) onBlur()
            },
        label = {
            Text(field.label + if (field.required) " *" else "")
        },
        placeholder = {
            Text(field.placeholder ?: "Select Date")
        },
        isError = error != null,
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            errorIndicatorColor = Color(0xFFB00020)
        ),
        trailingIcon = {
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = "Select date"
            )
        },
        supportingText = error?.let {
            { Text(it, color = Color(0xFFB00020)) }
        }
    )

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release && isEnabled) {
                onDateClick()
            }
        }
    }
}


@Composable
fun DynamicNumberField(
    field: FormField,
    value: Any?,
    error: String?,
    onValueChange: (String) -> Unit,
    onBlur: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value?.toString() ?: "",
        onValueChange = { newValue ->
            if (newValue.all { it.isDigit() }) {
                onValueChange(newValue)
            }
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(field.label + if (field.required) " *" else "") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = error != null,
        supportingText = error?.let {
            { Text(it, color = Color(0xFFB00020)) }
        }
    )

    LaunchedEffect(value) { onBlur() }
}

@Composable
fun DynamicVerifiedInputField(
    field: FormField,
    value: Any?,
    error: String?,
    onValueChange: (String) -> Unit,
    onBlur: () -> Unit,
    onVerifyClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isVerified: Boolean = false
) {
    val textValue = when (value) {
        null -> ""
        is String -> value
        else -> value.toString()
    }
    
    val config = field.verifiedInputConfig
    val inputConfig = config?.input
    
    // For API_VERIFICATION, use field config directly
    val keyboardType = when (inputConfig?.keyboard ?: field.keyboard) {
        "PHONE", "NUMBER" -> KeyboardType.Phone
        "EMAIL" -> KeyboardType.Email
        else -> KeyboardType.Text
    }
    
    val maxLength = inputConfig?.maxLength ?: field.maxLength

    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            val filtered = when (inputConfig?.dataType) {
                "NUMBER" -> newValue.filter { it.isDigit() }
                else -> newValue
            }
            
            val finalValue = if (maxLength != null && filtered.length > maxLength) {
                filtered.take(maxLength)
            } else {
                filtered
            }
            
            onValueChange(finalValue)
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(field.label + if (field.required) " *" else "") },
        placeholder = { Text(field.placeholder ?: "") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        isError = error != null,
        enabled = isEnabled,
        readOnly = field.readOnly, // Keep fields editable even after verification success
        colors = TextFieldDefaults.colors(
            // Use plain background for all states (no green background when verified)
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            errorIndicatorColor = Color(0xFFB00020)
        ),
        trailingIcon = {
            when {
                error != null -> {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Error",
                        tint = Color(0xFFB00020)
                    )
                }
                isVerified -> {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color(0xFF16A34A) // Green check when verified
                    )
                }
                else -> {
                    // Blue tick icon for verification - show for VERIFIED_INPUT or API_VERIFICATION
                    val showVerifyIcon = field.type == "VERIFIED_INPUT" || field.type == "API_VERIFICATION"
                    // Blue tick is enabled if: field is enabled, not read-only, and has value
                    // Validation will be checked on click
                    val canVerify = isEnabled && !field.readOnly && textValue.isNotBlank()
                    
                    if (showVerifyIcon) {
                        // Always show the icon, but disable it when field is empty or read-only
                        // Validation will be checked when user clicks the icon
                        IconButton(
                            onClick = { 
                                android.util.Log.d("DynamicVerifiedInputField", "Blue tick clicked for field: ${field.id}, type: ${field.type}, value: '$textValue', enabled: $isEnabled, readOnly: ${field.readOnly}")
                                if (canVerify) {
                                    android.util.Log.d("DynamicVerifiedInputField", "Calling onVerifyClick for field: ${field.id} - validation will be checked in handler")
                                    onVerifyClick() // Validation will be checked in the handler
                                } else {
                                    android.util.Log.d("DynamicVerifiedInputField", "Cannot verify: isEnabled=$isEnabled, readOnly=${field.readOnly}, textValue.isBlank()=${textValue.isBlank()}")
                                }
                            },
                            enabled = canVerify
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Verify",
                                tint = if (canVerify) Color(0xFF2563EB) else Color(0xFF2563EB).copy(alpha = 0.4f) // Blue color, dimmed when disabled
                            )
                        }
                    }
                }
            }
        },
        supportingText = error?.let {
            { Text(it, color = Color(0xFFB00020)) }
        }
    )
}


