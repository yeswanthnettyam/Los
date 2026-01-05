package com.kaleidofin.originator.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaleidofin.originator.domain.model.FormField
import com.kaleidofin.originator.domain.model.FormSection
import com.kaleidofin.originator.presentation.component.*
import com.kaleidofin.originator.presentation.ui.state.DynamicFormUiState
import com.kaleidofin.originator.presentation.viewmodel.DynamicFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFormScreen(
    target: String,
    onNavigateBack: () -> Unit,
    onNavigateToNext: (String) -> Unit,
    viewModel: DynamicFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val flowStack = viewModel.flowStack.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // 🔑 Focus management
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    
    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerFieldId by remember { mutableStateOf<String?>(null) }
    var datePickerConstraints by remember { mutableStateOf<com.kaleidofin.originator.domain.model.FieldConstraints?>(null) }
    var datePickerMode by remember { mutableStateOf<String?>(null) }
    var datePickerMinDate by remember { mutableStateOf<String?>(null) }
    var datePickerMaxDate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(target) {
        // Check if this is initial load (flow stack is empty)
        val isInitialLoad = flowStack.isEmpty()
        // Check if we have pending restore data (from back navigation)
        val restoreData = viewModel.getPendingRestoreData()
        viewModel.loadFormConfiguration(target, isInitialLoad, restoreData)
    }

    LaunchedEffect(uiState.nextScreen) {
        uiState.nextScreen?.let(onNavigateToNext)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    // 🎯 Focus first invalid field
    LaunchedEffect(uiState.firstErrorFieldId) {
        uiState.firstErrorFieldId?.let { fieldId ->
            focusRequesters[fieldId]?.requestFocus()
            viewModel.clearFirstErrorField()
        }
    }

    val formScreen = uiState.formScreen

    // Handle system back button using flow stack
    BackHandler(enabled = flowStack.size > 1) {
        viewModel.handleBackNavigation(
            onNavigateToScreen = { screenId ->
                onNavigateToNext(screenId)
            },
            onExitFlow = onNavigateBack
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },

        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 2.dp
            ) {
                Column {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (flowStack.size > 1) {
                                    viewModel.handleBackNavigation(
                                        onNavigateToScreen = { screenId ->
                                            onNavigateToNext(screenId)
                                        },
                                        onExitFlow = onNavigateBack
                                    )
                                } else {
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = formScreen?.title ?: "Form",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        },

        bottomBar = {
            if (formScreen?.layout?.stickyFooter == true) {
                Surface(shadowElevation = 8.dp) {
                    PrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        text = formScreen.layout.submitButtonText,
                        onClick = { viewModel.submitForm() },
                        isLoading = uiState.isSubmitting
                    )
                }
            }
        }
    ) { innerPadding ->

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            formScreen != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                ) {

                    // Render sections (only root sections, subsections are rendered within FormSection)
                    formScreen.sections.forEach { section ->
                        // Only render sections that don't have a parent (root sections)
                        // Subsections will be rendered within their parent
                        if (section.subSectionOf == null) {
                            val instanceCount =
                                if (section.repeatable)
                                    uiState.sectionInstances[section.sectionId] ?: section.minInstances
                                else 1

                            repeat(instanceCount) { index ->
                                FormSection(
                                    section = section,
                                    uiState = uiState,
                                    sectionIndex = if (section.repeatable) index else null,
                                    focusRequesters = focusRequesters,
                                    viewModel = viewModel,
                                    onValueChange = { fieldId, value ->
                                        val actualIndex = if (section.repeatable) index else null
                                        viewModel.updateFieldValue(fieldId, value, actualIndex)
                                    },
                                    onBlur = { fieldId ->
                                        val field = section.fields.find { it.id == fieldId }
                                        
                                        // Skip blur validation for DATE fields - only validate on submit
                                        if (field?.type != "DATE") {
                                            val fieldValue = uiState.getFieldValue(
                                                if (section.repeatable) "${fieldId}_$index" else fieldId
                                            )
                                            val actualIndex = if (section.repeatable) index else null
                                            viewModel.validateFieldOnBlur(fieldId, fieldValue, actualIndex)
                                        }
                                        
                                        // Check if verification should be triggered on blur
                                        field?.verification?.let { verification ->
                                            if (verification.trigger == "ON_BLUR" && verification.enabled) {
                                                viewModel.openModal(verification.modalId)
                                            }
                                        }
                                    },
                                    onTriggerVerification = { fieldId, value ->
                                        val actualIndex = if (section.repeatable) index else null
                                        viewModel.checkAndTriggerVerification(fieldId, value, actualIndex)
                                    },
                                    onDateClick = { fieldKey, field ->
                                        datePickerFieldId = fieldKey
                                        datePickerConstraints = field.constraints
                                        datePickerMode = field.dateMode
                                        datePickerMinDate = field.minDate
                                        datePickerMaxDate = field.maxDate
                                        showDatePicker = true
                                    }
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }

                    // Non-sticky submit
                    if (!formScreen.layout.stickyFooter) {
                        Spacer(Modifier.height(24.dp))
                        PrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = formScreen.layout.submitButtonText,
                            onClick = { viewModel.submitForm() },
                            isLoading = uiState.isSubmitting
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                }
                
                // Handle modals
                uiState.openModalId?.let { modalId ->
                    val modal = formScreen.modals.find { it.modalId == modalId }
                    modal?.let {
                        VerificationModal(
                            modal = it,
                            onDismiss = { viewModel.closeModal() },
                            onSuccess = { fieldId, value ->
                                viewModel.updateHiddenField(fieldId, value)
                                viewModel.closeModal()
                            }
                        )
                    }
                }
                
                // Handle date picker dialog
                if (showDatePicker && datePickerFieldId != null) {
                    val selectedDateMillis = uiState.getFieldValue(datePickerFieldId!!)?.let {
                        try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.toString())?.time
                        } catch (e: Exception) {
                            null
                        }
                    } ?: Calendar.getInstance().timeInMillis
                    
                    DatePickerDialog(
                        initialSelectedDateMillis = selectedDateMillis,
                        onDateSelected = { dateString ->

                            // dateString is already "yyyy-MM-dd"
                            val rawKey = datePickerFieldId!!

                            val lastPart = rawKey.substringAfterLast("_", missingDelimiterValue = "")
                            val sectionIndex = lastPart.toIntOrNull()

                            val fieldId = if (sectionIndex != null) {
                                rawKey.substringBeforeLast("_")
                            } else {
                                rawKey
                            }

                            viewModel.updateFieldValue(fieldId, dateString, sectionIndex)

                            showDatePicker = false
                            datePickerFieldId = null
                            datePickerConstraints = null
                            datePickerMode = null
                            datePickerMinDate = null
                            datePickerMaxDate = null
                        },

                                dateMode = datePickerMode,
                        constraints = datePickerConstraints,
                        minDate = datePickerMinDate,
                        maxDate = datePickerMaxDate,
                        onDismiss = {
                            showDatePicker = false
                            datePickerFieldId = null
                            datePickerConstraints = null
                            datePickerMode = null
                            datePickerMinDate = null
                            datePickerMaxDate = null
                        }
                    )
                }
            }
        }
    }
}



@Composable
private fun FormSection(
    section: FormSection,
    uiState: DynamicFormUiState,
    sectionIndex: Int?,
    focusRequesters: MutableMap<String, FocusRequester>,
    viewModel: DynamicFormViewModel,
    onValueChange: (String, Any) -> Unit,
    onBlur: (String) -> Unit,
    onTriggerVerification: (String, Any) -> Unit = { _, _ -> }, // New callback for verification
    onDateClick: (String, com.kaleidofin.originator.domain.model.FormField) -> Unit // Callback for date picker
) {
    var isExpanded by remember { mutableStateOf(section.expanded) }
    val instanceCount = if (section.repeatable) {
        uiState.sectionInstances[section.sectionId] ?: section.minInstances
    } else {
        1
    }
    val canAdd = section.repeatable && (section.maxInstances == null || instanceCount < section.maxInstances)
    val canRemove = section.repeatable && instanceCount > section.minInstances
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Section header with title, expand/collapse, and add/remove buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = section.collapsible) { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = section.instanceLabel?.replace("{{index}}", "${(sectionIndex ?: 0) + 1}") ?: section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Add button for repeatable sections (only show on first instance)
                    if (section.repeatable && sectionIndex == 0 && canAdd) {
                            IconButton(
                            onClick = { viewModel.addSectionInstance(section.sectionId) }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add ${section.title}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Remove button for repeatable sections
                    if (section.repeatable && canRemove) {
                        IconButton(
                            onClick = { viewModel.removeSectionInstance(section.sectionId) }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Remove ${section.title}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Expand/Collapse icon
                    if (section.collapsible) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            if (isExpanded) {
                Spacer(Modifier.height(12.dp))

                // Render fields
                section.fields.forEach { field ->
                val fieldKey =
                    if (sectionIndex != null) "${field.id}_$sectionIndex" else field.id
                
                // Check if field is enabled based on enabledWhen conditions
                val isFieldEnabled = if (field.enabledWhen.isNotEmpty()) {
                    field.enabledWhen.all { condition ->
                        uiState.evaluateEnabledCondition(condition, sectionIndex)
                    }
                } else {
                    true // Field is always enabled if no enabledWhen conditions
                }

                // Get current value from state - this should trigger recomposition when state changes
                val currentValue = uiState.formData[fieldKey]
                
                // Debug log to verify value is being read from state
                LaunchedEffect(currentValue, fieldKey) {
                    if (field.type == "TEXTAREA" || field.id.contains("phone") || field.id.contains("address")) {
                        android.util.Log.d("FormSection", "Field $fieldKey value from state: '$currentValue'")
                    }
                }
                
                DynamicFormFieldRenderer(
                    field = field,
                    value = currentValue,
                    error = uiState.fieldErrors[fieldKey],
                    fieldKey = fieldKey,
                    focusRequesters = focusRequesters,
                    uiState = uiState,
                    isEnabled = isFieldEnabled,
                    onValueChange = { newValue -> 
                        // Convert Any to String if needed, then pass to parent
                        val stringValue = when (newValue) {
                            is String -> newValue
                            else -> newValue.toString()
                        }
                        android.util.Log.d("FormSection", "Field $fieldKey onValueChange called with: '$stringValue'")
                        onValueChange(field.id, stringValue)
                        // Trigger verification check if needed
                        onTriggerVerification(field.id, stringValue)
                    },
                    onBlur = { onBlur(field.id) },
                    onDateClick = onDateClick
                )

                    Spacer(Modifier.height(8.dp))
                }
                
                // Render subsections
                section.subSections.forEach { subSection ->
                    val subInstanceCount =
                        if (subSection.repeatable)
                            uiState.sectionInstances[subSection.sectionId] ?: subSection.minInstances
                        else 1
                    
                    repeat(subInstanceCount) { subIndex ->
                        Spacer(Modifier.height(8.dp))
                        FormSection(
                            section = subSection,
                            uiState = uiState,
                            sectionIndex = if (subSection.repeatable) subIndex else null,
                            focusRequesters = focusRequesters,
                            viewModel = viewModel,
                            onValueChange = { fieldId, value ->
                                val actualIndex = if (subSection.repeatable) subIndex else null
                                viewModel.updateFieldValue(fieldId, value, actualIndex)
                            },
                            onBlur = { fieldId ->
                                val field = subSection.fields.find { it.id == fieldId }
                                
                                // Skip blur validation for DATE fields - only validate on submit
                                if (field?.type != "DATE") {
                                    val fieldValue = uiState.getFieldValue(
                                        if (subSection.repeatable) "${fieldId}_$subIndex" else fieldId
                                    )
                                    val actualIndex = if (subSection.repeatable) subIndex else null
                                    viewModel.validateFieldOnBlur(fieldId, fieldValue, actualIndex)
                                }
                                
                                // Check if verification should be triggered on blur
                                field?.verification?.let { verification ->
                                    if (verification.trigger == "ON_BLUR" && verification.enabled) {
                                        viewModel.openModal(verification.modalId)
                                    }
                                }
                            },
                            onTriggerVerification = { fieldId, value ->
                                val actualIndex = if (subSection.repeatable) subIndex else null
                                viewModel.checkAndTriggerVerification(fieldId, value, actualIndex)
                            },
                            onDateClick = { fieldKey, field ->
                                onDateClick(fieldKey, field)
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun DynamicFormFieldRenderer(
    field: FormField,
    value: Any?,
    error: String?,
    fieldKey: String,
    focusRequesters: MutableMap<String, FocusRequester>,
    uiState: DynamicFormUiState,
    isEnabled: Boolean = true,
    onValueChange: (Any) -> Unit,
    onBlur: () -> Unit,
    onDateClick: (String, com.kaleidofin.originator.domain.model.FormField) -> Unit = { _, _ -> }
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequesters[fieldKey] = focusRequester
    }
    
    // Check verification status for this field
    val isVerified = field.verification?.let { verification ->
        uiState.getFieldValue(verification.statusField) as? Boolean ?: false
    } ?: false

    when (field.type) {
        "DROPDOWN" -> {
            // Get options based on dataSource type
            val options = when (field.dataSource?.type) {
                "INLINE" -> {
                    // Get from inlineData using field.id as key
                    uiState.inlineData[field.id] ?: emptyList()
                }
                "MASTER" -> {
                    // Get from masterData using dataSource.key
                    field.dataSource.key?.let { key ->
                        uiState.masterData[key] ?: emptyList()
                    } ?: emptyList()
                }
                "API" -> {
                    // API dataSource - for now return empty, will be loaded on demand
                    emptyList()
                }
                else -> emptyList()
            }
            
            android.util.Log.d("DynamicFormFieldRenderer", "Dropdown field: ${field.id}, dataSource type: ${field.dataSource?.type}, key: ${field.dataSource?.key}, options: $options")
            
            DynamicDropdownField(
                field = field,
                value = value,
                error = error,
                options = options,
                modifier = Modifier.focusRequester(focusRequester),
                isEnabled = isEnabled,
                onValueChange = { newValue ->
                    onValueChange(newValue)
                },
                onBlur = onBlur
            )
        }
        "DATE" -> {
            DynamicDateField(
                field = field,
                value = value,
                error = error,
                isEnabled = isEnabled,
                modifier = Modifier.focusRequester(focusRequester),
                onDateClick = {
                    onDateClick(fieldKey, field)
                },
                onBlur = onBlur
            )
        }
        else -> {
            DynamicFormField(
                field = field,
                value = value,
                error = error,
                modifier = Modifier.focusRequester(focusRequester),
                isEnabled = isEnabled,
                isVerified = isVerified,
                onValueChange = { newValue -> 
                    // newValue is String from DynamicFormField
                    // Pass it as Any to match the callback signature
                    onValueChange(newValue as Any)
                },
                onBlur = onBlur
            )
        }
    }
}


@Composable
private fun VerificationModal(
    modal: com.kaleidofin.originator.domain.model.FormModal,
    onDismiss: () -> Unit,
    onSuccess: (String, Any) -> Unit
) {
    // Simplified modal - can be enhanced with actual OTP input
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(modal.header?.title ?: "Verification") },
        text = { Text(modal.consentText ?: "Please verify") },
        confirmButton = {
            TextButton(
                onClick = {
                    val successAction = modal.actions.firstOrNull()?.onSuccess
                    if (successAction != null) {
                        onSuccess(successAction.updateField, successAction.value)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(modal.actions.firstOrNull()?.label ?: "OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



