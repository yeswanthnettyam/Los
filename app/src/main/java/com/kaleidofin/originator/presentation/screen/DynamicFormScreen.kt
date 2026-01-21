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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.input.KeyboardType
import androidx.activity.compose.BackHandler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.widget.Toast
import android.util.Base64
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.kaleidofin.originator.domain.model.FormField
import com.kaleidofin.originator.domain.model.FormSection
import com.kaleidofin.originator.presentation.component.*
import com.kaleidofin.originator.presentation.navigation.Screen
import com.kaleidofin.originator.presentation.ui.state.DynamicFormUiState
import com.kaleidofin.originator.presentation.viewmodel.DynamicFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * SINGLE DynamicFormScreen for entire flow
 * Screen transitions happen via ViewModel state updates, not navigation
 * Screen ID comes from ViewModel state (formScreen.screenId or runtimeSession.currentScreenId)
 */
fun DynamicFormScreen(
    flowId: String? = null,
    productCode: String? = null,
    partnerCode: String? = null,
    branchCode: String? = null,
    navController: NavController,
    onNavigateBack: () -> Unit,
    viewModel: DynamicFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // State for Aadhaar QR decoding
    var isDecodingAadhaar by remember { mutableStateOf(false) }
    var showAadhaarDecodeProgressDialog by remember { mutableStateOf(false) }
    var aadhaarDecodeError by remember { mutableStateOf<String?>(null) }

    // 🔑 Focus management
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    
    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerFieldId by remember { mutableStateOf<String?>(null) }
    var datePickerConfig by remember { mutableStateOf<com.kaleidofin.originator.domain.model.DateConfig?>(null) }

    // Verified input state
    var showConsentDialog by remember { mutableStateOf(false) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var verifyingFieldId by remember { mutableStateOf<String?>(null) }
    var verifyingFieldValue by remember { mutableStateOf<String?>(null) }
    
    // Generic Verification Result Dialog state (for all verification types)
    var showVerificationResultDialog by remember { mutableStateOf(false) }
    var verificationResultMessage by remember { mutableStateOf<String?>(null) }
    var verificationResultIsSuccess by remember { mutableStateOf(false) }
    var verificationResultShowDialog by remember { mutableStateOf(true) } // Default to show dialog
    
    // Snackbar messages for verification (fallback when showDialog is false)
    var verificationErrorMessage by remember { mutableStateOf<String?>(null) }
    var verificationSuccessMessage by remember { mutableStateOf<String?>(null) }
    
    // Show snackbar messages (only when dialog is not shown)
    LaunchedEffect(verificationErrorMessage) {
        verificationErrorMessage?.let {
            if (!verificationResultShowDialog) {
                snackbarHostState.showSnackbar(it)
            }
            verificationErrorMessage = null
        }
    }
    
    LaunchedEffect(verificationSuccessMessage) {
        verificationSuccessMessage?.let {
            if (!verificationResultShowDialog) {
                snackbarHostState.showSnackbar(it)
            }
            verificationSuccessMessage = null
        }
    }
    
    // Helper function to show verification result (success or failure)
    val showVerificationResult: (String, Boolean, Boolean) -> Unit = { message, isSuccess, showDialog ->
        if (showDialog) {
            verificationResultMessage = message
            verificationResultIsSuccess = isSuccess
            verificationResultShowDialog = true
            showVerificationResultDialog = true
        } else {
            if (isSuccess) {
                verificationSuccessMessage = message
            } else {
                verificationErrorMessage = message
            }
        }
    }

    // Handle flow completion - navigate back to Home when flow ends
    LaunchedEffect(uiState.isFlowCompleted) {
        if (uiState.isFlowCompleted) {
            android.util.Log.d("DynamicFormScreen", "Flow completed - navigating back to Home")
            // Reset completion flag and navigate back to Home
            viewModel.resetFlowCompletion()
            onNavigateBack()
        }
    }

    // CRITICAL: LaunchedEffect MUST NOT call Runtime API for screen transitions
    // Runtime API is called ONLY from:
    // 1. startFlow() - when entering from Home (runtimeSession == null)
    // 2. submitForm() - when user submits form
    // Screen transitions happen via ViewModel state updates - UI recomposes automatically
    LaunchedEffect(flowId, productCode, partnerCode, branchCode, uiState.runtimeSession, uiState.isFlowCompleted) {
        val runtimeSession = uiState.runtimeSession
        val currentScreen = uiState.formScreen
        
        android.util.Log.d("DynamicFormScreen", "LaunchedEffect triggered - runtimeSession: ${runtimeSession?.applicationId}, currentScreen: ${currentScreen?.screenId}, flowId: $flowId, isFlowCompleted: ${uiState.isFlowCompleted}")
        
        // If flow has completed, don't restart it
        if (uiState.isFlowCompleted) {
            android.util.Log.d("DynamicFormScreen", "Flow completed - not restarting")
            return@LaunchedEffect
        }
        
        // If runtimeSession exists, flow has already started
        // Screen transitions happen via state updates from submitForm() - no action needed
        if (runtimeSession != null) {
            android.util.Log.d("DynamicFormScreen", "Flow already started - applicationId: ${runtimeSession.applicationId}, currentScreenId: ${runtimeSession.currentScreenId}")
            return@LaunchedEffect
        }
        
        // FLOW START MODE: runtimeSession == null
        // This happens ONLY when entering from Home (dashboard click)
        // Call startFlow() ONLY if we have required parameters
        if (uiState.isRestoringFromBack || uiState.isLoadingFromResponse) {
            android.util.Log.d("DynamicFormScreen", "Skipping flow start - restoring or loading")
            return@LaunchedEffect
        }
        
        // Validate required parameters for flow start
        if (flowId == null || productCode == null) {
            android.util.Log.w("DynamicFormScreen", "Flow start parameters missing - flowId: $flowId, productCode: $productCode")
            return@LaunchedEffect
        }
        
        // Call startFlow() - ONLY place where flow start Runtime API is called
        android.util.Log.d("DynamicFormScreen", "🚀 Calling startFlow() - flowId: $flowId, productCode: $productCode")
        viewModel.startFlow(
            flowId = flowId,
            productCode = productCode,
            partnerCode = partnerCode,
            branchCode = branchCode
        )
    }
    
    // Screen ID comes from ViewModel state, not navigation params
    val currentScreenId = uiState.formScreen?.screenId ?: uiState.runtimeSession?.currentScreenId

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
    val allowBackNavigation = formScreen?.layout?.allowBackNavigation ?: true // Default to true for backward compatibility

    // Handle system back button - respect allowBackNavigation flag
    BackHandler(enabled = allowBackNavigation) {
        if (allowBackNavigation) {
        viewModel.handleBackNavigation(
            onExitFlow = {
                // Exit flow - reset completion flag and navigate back to Home
                viewModel.resetFlowCompletion()
                onNavigateBack()
            },
            onError = { error ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(error)
                }
            }
        )
        }
        // If allowBackNavigation is false, BackHandler is disabled, so back is blocked
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
                        // Show back button only if allowBackNavigation is true
                        if (allowBackNavigation) {
                        IconButton(
                            onClick = {
                                    viewModel.handleBackNavigation(
                                    onExitFlow = {
                                        // Exit flow - reset completion flag and navigate back to Home
                                        viewModel.resetFlowCompletion()
                                    onNavigateBack()
                                    },
                                        onError = { error ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(error)
                                }
                                        }
                                    )
                            },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            }
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
            formScreen?.let { screen ->
                Surface(shadowElevation = 8.dp) {
                    PrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        text = screen.layout.submitButtonText,
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
                                    navController = navController,
                                    isDecodingAadhaar = isDecodingAadhaar,
                                    coroutineScope = coroutineScope,
                                    context = context,
                                    onSetDecodingAadhaar = { value -> isDecodingAadhaar = value },
                                    onShowAadhaarDecodeProgressDialog = { value -> showAadhaarDecodeProgressDialog = value },
                                    onSetAadhaarDecodeError = { error -> aadhaarDecodeError = error },
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
                                        datePickerConfig = field.dateConfig
                                        showDatePicker = true
                                    },
                                    onVerifyClick = { fieldIdForVerify, fieldValue ->
                                        android.util.Log.d("DynamicFormScreen", "onVerifyClick called for fieldId: $fieldIdForVerify, fieldValue: $fieldValue")
                                        
                                        // Find the field to get config - search in all sections and subsections
                                        val allFields = formScreen?.sections?.flatMap { it.fields }
                                            ?.plus(formScreen.sections.flatMap { it.subSections.flatMap { sub -> sub.fields } })
                                        val fieldToVerify = allFields?.find { 
                                            it.id == fieldIdForVerify
                                        }
                                        
                                        android.util.Log.d("DynamicFormScreen", "Found field: ${fieldToVerify?.id}, type: ${fieldToVerify?.type}")
                                        
                                        // Get the actual fieldKey (with section index if applicable) for this field
                                        val actualFieldKey = if (section.repeatable) "${fieldIdForVerify}_$index" else fieldIdForVerify
                                        
                                        when (fieldToVerify?.type) {
                                            "VERIFIED_INPUT" -> {
                                                android.util.Log.d("DynamicFormScreen", "VERIFIED_INPUT field found, checking consent config")
                                                // For VERIFIED_INPUT: Show consent dialog
                                                val consent = fieldToVerify.verifiedInputConfig?.verification?.otp?.consent
                                                android.util.Log.d("DynamicFormScreen", "Consent config: $consent")
                                                if (consent != null) {
                                                    // Clear any previous verification error for this field
                                                    viewModel.clearFieldError(actualFieldKey)
                                                    
                                                    // Store field info for consent dialog (use actualFieldKey for verification status)
                                                    verifyingFieldId = actualFieldKey
                                                    verifyingFieldValue = fieldValue
                                                    showConsentDialog = true
                                                    android.util.Log.d("DynamicFormScreen", "Opening consent dialog for field: ${fieldToVerify.id}")
                                                } else {
                                                    android.util.Log.d("DynamicFormScreen", "No consent config found for field: ${fieldToVerify.id}")
                                                }
                                            }
                                            "API_VERIFICATION" -> {
                                                android.util.Log.d("DynamicFormScreen", "API_VERIFICATION field found - calling API directly")
                                                // For API_VERIFICATION: Call API directly (NO CONSENT DIALOG)
                                                val apiConfig = fieldToVerify.apiVerificationConfig
                                                if (apiConfig != null) {
                                                    verifyingFieldId = actualFieldKey
                                                    verifyingFieldValue = fieldValue
                                                    
                                                    // Clear any previous verification error for this field
                                                    viewModel.clearFieldError(actualFieldKey)
                                                    
                                                    // Call API verification
                                                    viewModel.verifyApi(
                                                        endpoint = apiConfig.endpoint ?: "",
                                                        method = apiConfig.method ?: "GET",
                                                        requestMapping = apiConfig.requestMapping,
                                                        fieldValue = fieldValue,
                                                        fieldId = fieldToVerify.id,
                                                        fieldKey = actualFieldKey, // Pass fieldKey to set error on failure
                                                        successCondition = apiConfig.successCondition,
                                                        onSuccess = {
                                                            // Show success message
                                                            val successMsg = apiConfig.messages?.get("success") ?: "Verification successful"
                                                            // Mark as verified (use actualFieldKey to handle section indices)
                                                            viewModel.updateFieldValue("${actualFieldKey}_verified", true, null)
                                                            
                                                            // Show result using generic dialog (can be dialog or snackbar based on config)
                                                            showVerificationResult(successMsg, true, apiConfig.showDialog == true)
                                                        },
                                                        onFailure = { error ->
                                                            // Show failure message below the field
                                                            val failureMsg = apiConfig.messages?.get("failure") ?: error
                                                            viewModel.setFieldError(actualFieldKey, failureMsg)
                                                            
                                                            // Also show in dialog/snackbar if showDialog is true
                                                            if (apiConfig.showDialog == true) {
                                                                showVerificationResult(failureMsg, false, true)
                                                            }
                                                        }
                                                    )
                                                } else {
                                                    android.util.Log.d("DynamicFormScreen", "No apiVerificationConfig found for field: ${fieldToVerify.id}")
                                                }
                                            }
                                            else -> {
                                                android.util.Log.d("DynamicFormScreen", "Field type not supported for verification: ${fieldToVerify?.type}")
                                            }
                                        }
                                    }
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }

                    // Add bottom padding to account for sticky submit button
                    Spacer(Modifier.height(80.dp))
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
                        dateConfig = datePickerConfig,
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
                            datePickerConfig = null
                        },
                        onDismiss = {
                            showDatePicker = false
                            datePickerFieldId = null
                            datePickerConfig = null
                        }
                    )
                }
                
                // Consent dialog for verified input
                if (showConsentDialog && verifyingFieldId != null && verifyingFieldValue != null) {
                    val fieldToVerify = formScreen?.sections?.flatMap { it.fields }
                        ?.plus(formScreen.sections.flatMap { it.subSections.flatMap { sub -> sub.fields } })
                        ?.find { it.id == verifyingFieldId }
                    
                    fieldToVerify?.verifiedInputConfig?.verification?.otp?.consent?.let { consent ->
                        // Replace placeholder in subtitle with actual phone number
                        val subTitleWithPhone = consent.subTitle?.replace(
                            "phone number ",
                            "phone number $verifyingFieldValue"
                        ) ?: "Please enter the ${fieldToVerify.verifiedInputConfig?.verification?.otp?.otpLength ?: 6} digit consent code sent to $verifyingFieldValue"
                        
                        ConsentDialog(
                            title = consent.title ?: "User Agreement",
                            subTitle = subTitleWithPhone,
                            message = consent.message ?: "",
                            positiveButtonText = consent.positiveButtonText ?: "Agree & Proceed",
                            negativeButtonText = consent.negativeButtonText ?: "Cancel",
                            onPositiveClick = {
                                showConsentDialog = false
                                // Send OTP when user agrees
                                fieldToVerify.verifiedInputConfig?.verification?.otp?.api?.sendOtp?.let { sendOtpConfig ->
                                    viewModel.sendOtp(
                                        endpoint = sendOtpConfig.endpoint ?: "/api/otp/send",
                                        method = sendOtpConfig.method ?: "POST",
                                        phoneNumber = verifyingFieldValue ?: "",
                                        onSuccess = {
                                            showOtpDialog = true
                                        },
                                        onFailure = { error ->
                                            // Show error message
                                            verificationErrorMessage = error
                                        }
                                    )
                                } ?: run {
                                    // If no API config, just show OTP dialog
                                    showOtpDialog = true
                                }
                            },
                            onNegativeClick = {
                                showConsentDialog = false
                                verifyingFieldId = null
                                verifyingFieldValue = null
                            }
                        )
                    }
                }
                
                // OTP dialog for verified input
                if (showOtpDialog && verifyingFieldId != null && verifyingFieldValue != null) {
                    val fieldToVerify = formScreen?.sections?.flatMap { it.fields }
                        ?.plus(formScreen.sections.flatMap { it.subSections.flatMap { sub -> sub.fields } })
                        ?.find { it.id == verifyingFieldId }
                    
                    fieldToVerify?.verifiedInputConfig?.verification?.otp?.let { otpConfig ->
                        // Get subtitle from consent config with phone number
                        val subTitleWithPhone = otpConfig.consent?.subTitle?.replace(
                            "phone number ",
                            "phone number ${verifyingFieldValue}"
                        ) ?: "Please enter the ${otpConfig.otpLength ?: 6} digit code sent to ${verifyingFieldValue}"
                        
                        OtpDialog(
                            phoneNumber = verifyingFieldValue ?: "",
                            subTitle = subTitleWithPhone,
                            otpLength = otpConfig.otpLength ?: 6,
                            resendIntervalSeconds = otpConfig.resendIntervalSeconds ?: 60,
                            onVerify = { otp, onComplete ->
                                // Clear any previous verification error for this field
                                viewModel.clearFieldError(verifyingFieldId ?: "")
                                
                                // Call API to verify OTP
                                otpConfig.api?.verifyOtp?.let { verifyOtpConfig ->
                                    viewModel.verifyOtp(
                                        endpoint = verifyOtpConfig.endpoint ?: "/api/otp/verify",
                                        method = verifyOtpConfig.method ?: "GET",
                                        phoneNumber = verifyingFieldValue ?: "",
                                        otp = otp,
                                        fieldId = verifyingFieldId ?: "",
                                        fieldKey = verifyingFieldId ?: "", // Pass fieldKey to set error on failure
                                        onSuccess = {
                                            showOtpDialog = false
                                            verifyingFieldId = null
                                            verifyingFieldValue = null
                                            // Show success message if available
                                            val successMsg = fieldToVerify.verifiedInputConfig?.verification?.messages?.get("success")
                                            val showDialog = fieldToVerify.verifiedInputConfig?.verification?.showDialog == true
                                            if (!successMsg.isNullOrEmpty()) {
                                                showVerificationResult(successMsg, true, showDialog)
                                            }
                                            onComplete(true)
                                        },
                                        onFailure = { error ->
                                            // Show failure message below the field
                                            val failureMsg = fieldToVerify.verifiedInputConfig?.verification?.messages?.get("failure") ?: error
                                            viewModel.setFieldError(verifyingFieldId ?: "", failureMsg)
                                            
                                            // Also show in dialog/snackbar if showDialog is true
                                            val showDialog = fieldToVerify.verifiedInputConfig?.verification?.showDialog == true
                                            if (showDialog) {
                                                showVerificationResult(failureMsg, false, true)
                                            }
                                            onComplete(false)
                                        }
                                    )
                                } ?: run {
                                    // If no API config, just mark as verified (verifyingFieldId already includes section index if applicable)
                                    viewModel.updateFieldValue("${verifyingFieldId}_verified", true, null)
                                    showOtpDialog = false
                                    verifyingFieldId = null
                                    verifyingFieldValue = null
                                    onComplete(true)
                                }
                            },
                            onDismiss = {
                                showOtpDialog = false
                                verifyingFieldId = null
                                verifyingFieldValue = null
                            },
                            onResend = {
                                // Call API to resend OTP
                                otpConfig.api?.sendOtp?.let { sendOtpConfig ->
                                    viewModel.sendOtp(
                                        endpoint = sendOtpConfig.endpoint ?: "/api/otp/send",
                                        method = sendOtpConfig.method ?: "POST",
                                        phoneNumber = verifyingFieldValue ?: "",
                                        onSuccess = {
                                            // OTP sent successfully, timer will reset automatically
                                        },
                                        onFailure = { error ->
                                            verificationErrorMessage = error
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
                
                // Generic Verification Result Dialog (for all verification fields)
                if (showVerificationResultDialog && verificationResultMessage != null) {
                    VerificationResultDialog(
                        message = verificationResultMessage ?: "",
                        isSuccess = verificationResultIsSuccess,
                        onDismiss = {
                            showVerificationResultDialog = false
                            verificationResultMessage = null
                            // Don't clear verifyingFieldId/verifyingFieldValue here as they might still be needed
                        }
                    )
                }
                
                // Aadhaar QR Decode Progress Dialog
                if (showAadhaarDecodeProgressDialog) {
                    Dialog(onDismissRequest = { /* Prevent dismiss during API call */ }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Reading Aadhaar QR…",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
                
                // Aadhaar QR Decode Error Dialog
                aadhaarDecodeError?.let { errorMessage ->
                    AlertDialog(
                        onDismissRequest = {
                            aadhaarDecodeError = null
                        },
                        title = {
                            Text("Error")
                        },
                        text = {
                            Text(errorMessage)
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    aadhaarDecodeError = null
                                }
                            ) {
                                Text("OK")
                            }
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
    navController: NavController,
    isDecodingAadhaar: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onSetDecodingAadhaar: (Boolean) -> Unit,
    onShowAadhaarDecodeProgressDialog: (Boolean) -> Unit,
    onSetAadhaarDecodeError: (String?) -> Unit,
    onValueChange: (String, Any) -> Unit,
    onBlur: (String) -> Unit,
    onTriggerVerification: (String, Any) -> Unit = { _, _ -> }, // New callback for verification
    onDateClick: (String, com.kaleidofin.originator.domain.model.FormField) -> Unit, // Callback for date picker
    onVerifyClick: (String, String) -> Unit = { _, _ -> } // Callback for verified input verification
) {
    var isExpanded by remember { mutableStateOf(section.expanded) }
    val instanceCount = if (section.repeatable) {
        uiState.sectionInstances[section.sectionId] ?: section.minInstances
    } else {
        1
    }
    val canAdd = section.repeatable && (section.maxInstances?.let { instanceCount < it } ?: true)
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
                    // For subsections, always show title. For main sections, show instanceLabel if available
                    text = if (section.subSectionOf != null) {
                        // Subsection: always use title
                        section.title
                    } else {
                        // Main section: use instanceLabel if available, otherwise title
                        section.instanceLabel?.replace("{{index}}", "${(sectionIndex ?: 0) + 1}") ?: section.title
                    },
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
                val isFieldEnabled = field.enabledWhen?.let { condition ->
                    uiState.evaluateDependencyCondition(condition, sectionIndex)
                } ?: true // Field is always enabled if no enabledWhen conditions
                
                // Check if field is visible based on visibleWhen conditions
                val isFieldVisible = field.visibleWhen?.let { condition ->
                    uiState.evaluateDependencyCondition(condition, sectionIndex)
                } ?: true // Field is always visible if no visibleWhen conditions
                
                // Check if field is required based on requiredWhen conditions
                val isFieldRequired = field.required || (field.requiredWhen?.let { condition ->
                    uiState.evaluateDependencyCondition(condition, sectionIndex)
                } ?: false)
                
                // Skip rendering if field is not visible
                if (!isFieldVisible) {
                    return@forEach
                }

                // Get current value from state - this should trigger recomposition when state changes
                // Unwrap value from { "value": ... } object
                val wrappedValue = uiState.formData[fieldKey]
                val currentValue = if (wrappedValue is Map<*, *> && wrappedValue.containsKey("value")) {
                    wrappedValue["value"]
                } else {
                    wrappedValue
                }
                
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
                    viewModel = viewModel,
                    navController = navController,
                    isEnabled = isFieldEnabled,
                    isDecodingAadhaar = isDecodingAadhaar,
                    coroutineScope = coroutineScope,
                    context = context,
                    onSetDecodingAadhaar = onSetDecodingAadhaar,
                    onShowAadhaarDecodeProgressDialog = onShowAadhaarDecodeProgressDialog,
                    onSetAadhaarDecodeError = onSetAadhaarDecodeError,
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
                        onDateClick = onDateClick,
                        onVerifyClick = { fieldKeyForVerify, fieldValue ->
                            onVerifyClick(fieldKeyForVerify, fieldValue)
                        }
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
                            navController = navController,
                            isDecodingAadhaar = isDecodingAadhaar,
                            coroutineScope = coroutineScope,
                            context = context,
                            onSetDecodingAadhaar = onSetDecodingAadhaar,
                            onShowAadhaarDecodeProgressDialog = onShowAadhaarDecodeProgressDialog,
                            onSetAadhaarDecodeError = onSetAadhaarDecodeError,
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
                            },
                            onVerifyClick = onVerifyClick
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
    viewModel: DynamicFormViewModel,
    navController: NavController,
    isEnabled: Boolean = true,
    isDecodingAadhaar: Boolean = false,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onSetDecodingAadhaar: (Boolean) -> Unit,
    onShowAadhaarDecodeProgressDialog: (Boolean) -> Unit,
    onSetAadhaarDecodeError: (String?) -> Unit,
    onValueChange: (Any) -> Unit,
    onBlur: () -> Unit,
    onDateClick: (String, com.kaleidofin.originator.domain.model.FormField) -> Unit = { _, _ -> },
    onVerifyClick: (String, String) -> Unit = { _, _ -> }
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequesters[fieldKey] = focusRequester
    }
    
    // Check verification status for this field (for both VERIFIED_INPUT and API_VERIFICATION)
    // Use fieldKey to handle section indices correctly (e.g., "fieldId_0_verified")
    val isVerified = when {
        field.verifiedInputConfig != null -> {
            uiState.getFieldValue("${fieldKey}_verified") as? Boolean ?: false
        }
        field.apiVerificationConfig != null -> {
            uiState.getFieldValue("${fieldKey}_verified") as? Boolean ?: false
        }
        field.verification != null -> {
            uiState.getFieldValue(field.verification.statusField) as? Boolean ?: false
        }
        else -> false
    }

    // Extract section index from fieldKey (e.g., "fieldId_0" -> 0, "fieldId" -> null)
    val sectionIndex = if (fieldKey.contains("_")) {
        val lastPart = fieldKey.substringAfterLast("_")
        if (lastPart.all { it.isDigit() }) {
            lastPart.toIntOrNull()
        } else {
            null
        }
    } else {
        null
    }

    when (field.type) {
        "VERIFIED_INPUT" -> {
            DynamicVerifiedInputField(
                field = field,
                value = value,
                error = error,
                modifier = Modifier.focusRequester(focusRequester),
                isEnabled = isEnabled,
                isVerified = isVerified,
                onValueChange = { newValue ->
                    onValueChange(newValue as Any)
                },
                onBlur = onBlur,
                onVerifyClick = {
                    val fieldValue = value?.toString() ?: ""
                    // Validate field before allowing verification
                    val validationError = viewModel.validateSingleField(field, fieldValue, sectionIndex)
                    if (validationError != null) {
                        // Show error message below field if validation fails
                        viewModel.setFieldError(fieldKey, validationError)
                        android.util.Log.d("DynamicFormScreen", "Validation failed for field ${field.id}: $validationError")
                    } else {
                        // Clear any existing error and proceed with verification flow
                        viewModel.clearFieldError(fieldKey)
                        android.util.Log.d("DynamicFormScreen", "Validation passed for field ${field.id}, proceeding with verification")
                        // Pass field.id instead of fieldKey to make lookup easier
                        onVerifyClick(field.id, fieldValue)
                    }
                }
            )
        }
        "API_VERIFICATION" -> {
            DynamicVerifiedInputField(
                field = field,
                value = value,
                error = error,
                modifier = Modifier.focusRequester(focusRequester),
                isEnabled = isEnabled,
                isVerified = isVerified,
                onValueChange = { newValue ->
                    onValueChange(newValue as Any)
                },
                onBlur = onBlur,
                onVerifyClick = {
                    val fieldValue = value?.toString() ?: ""
                    // Validate field before allowing verification
                    val validationError = viewModel.validateSingleField(field, fieldValue, sectionIndex)
                    if (validationError != null) {
                        // Show error message below field if validation fails
                        viewModel.setFieldError(fieldKey, validationError)
                        android.util.Log.d("DynamicFormScreen", "Validation failed for field ${field.id}: $validationError")
                    } else {
                        // Clear any existing error and proceed with verification flow
                        viewModel.clearFieldError(fieldKey)
                        android.util.Log.d("DynamicFormScreen", "Validation passed for field ${field.id}, proceeding with API verification")
                        // Pass field.id instead of fieldKey to make lookup easier
                        onVerifyClick(field.id, fieldValue)
                    }
                }
            )
        }
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
                onValueChange = { selectedLabel ->
                    // For STATIC_JSON fields (normalized to INLINE), map label to value
                    val selectedValue = if (field.dataSource?.staticData != null && field.dataSource.staticData.isNotEmpty()) {
                        // Find the value corresponding to the selected label
                        field.dataSource.staticData.find { it.label == selectedLabel }?.value ?: selectedLabel
                    } else {
                        // For non-STATIC_JSON fields, use the value as-is
                        selectedLabel
                    }
                    android.util.Log.d("DynamicFormFieldRenderer", "Field: ${field.id}, Selected label: '$selectedLabel', Storing value: '$selectedValue'")
                    onValueChange(selectedValue)
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
        "RADIO" -> {
            DynamicRadioField(
                field = field,
                value = value,
                error = error,
                modifier = Modifier.focusRequester(focusRequester),
                isEnabled = isEnabled,
                onValueChange = { newValue ->
                    onValueChange(newValue)
                },
                onBlur = onBlur
            )
        }
        "CAMERA_CAPTURE" -> {
            val cameraConfig = field.cameraConfig
            if (cameraConfig != null) {
                DynamicCameraCaptureField(
                    field = field,
                    value = value,
                    error = error,
                    modifier = Modifier.focusRequester(focusRequester),
                    isEnabled = isEnabled,
                    uploadImage = if (cameraConfig.uploadApi != null) {
                        { imageBytes, mimeType ->
                            viewModel.uploadImage(
                                endpoint = cameraConfig.uploadApi!!.endpoint,
                                method = cameraConfig.uploadApi!!.method,
                                imageBytes = imageBytes,
                                mimeType = mimeType
                            )
                        }
                    } else {
                        // If no uploadApi, return null (image will be stored locally as base64 or file path)
                        { _, _ -> null }
                    },
                    navController = navController,
                    onValueChange = { url ->
                        onValueChange(url)
                    },
                    onBlur = onBlur
                )
            }
        }
        "WEBVIEW_LAUNCH" -> {
            android.util.Log.d("DynamicFormFieldRenderer", "Rendering WEBVIEW_LAUNCH field: ${field.id}")
            val webViewConfig = field.webViewConfig
            android.util.Log.d("DynamicFormFieldRenderer", "Field ${field.id}: webViewConfig=$webViewConfig")
            if (webViewConfig != null) {
                android.util.Log.d("DynamicFormFieldRenderer", "Field ${field.id}: urlSource=${webViewConfig.urlSource}, staticUrl=${webViewConfig.staticUrl}")
                
                DynamicWebViewLaunchField(
                    field = field,
                    error = error,
                    modifier = Modifier.focusRequester(focusRequester),
                    isEnabled = isEnabled,
                    onLaunchWebView = { url ->
                        // Navigate to WebView screen with field label as title
                        val title = field.label.takeIf { it.isNotBlank() } ?: "External Process"
                        navController.navigate(Screen.WebView.createRoute(url, title))
                    },
                    getUrlFromApi = {
                        if (webViewConfig.launchApi != null) {
                            viewModel.getWebViewUrl(
                                endpoint = webViewConfig.launchApi.endpoint,
                                method = webViewConfig.launchApi.method,
                                responseUrlField = webViewConfig.responseUrlField
                            )
                        } else {
                            null
                        }
                    }
                )
            }
        }
        "QR_SCANNER" -> {
            val qrConfig = field.qrConfig
            if (qrConfig != null) {
                DynamicQRScannerField(
                    field = field,
                    error = error,
                    navController = navController,
                    modifier = Modifier.focusRequester(focusRequester),
                    isEnabled = isEnabled,
                    onQRScanned = { prefillData ->
                        // Prefill target fields with scanned values (standard JSON QR)
                        prefillData.forEach { (targetFieldId, scannedValue) ->
                            // Find the target field and its section to determine section index
                            var targetSectionIndex: Int? = null
                            
                            uiState.formScreen?.sections?.forEach { section ->
                                // Check main section fields
                                section.fields.find { it.id == targetFieldId }?.let {
                                    targetSectionIndex = if (section.repeatable) {
                                        0
                                    } else {
                                        null
                                    }
                                }
                                
                                // Check subsection fields
                                section.subSections.forEach { subSection ->
                                    subSection.fields.find { it.id == targetFieldId }?.let {
                                        targetSectionIndex = if (subSection.repeatable) {
                                            0
                                        } else {
                                            null
                                        }
                                    }
                                }
                            }
                            
                            // Update the field value
                            viewModel.updateFieldValue(targetFieldId, scannedValue, targetSectionIndex)
                        }
                    },
                    onAadhaarQRDetected = { rawBytes ->
                        // Handle Aadhaar Secure QR binary payload
                        coroutineScope.launch {
                            try {
                                onSetDecodingAadhaar(true)
                                
                                // Show progress dialog
                                onShowAadhaarDecodeProgressDialog(true)
                                onSetAadhaarDecodeError(null)
                                
                                // Base64 encode rawBytes (NO_WRAP)
                                val qrDataBase64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                                
                                // Call backend decode API
                                val result = viewModel.decodeAadhaarQR(qrDataBase64)
                                
                                result.onSuccess { decodedData ->
                                    // Close progress dialog on success
                                    onShowAadhaarDecodeProgressDialog(false)
                                    
                                    // Prefill fields using qrConfig.prefillMapping if available
                                    if (qrConfig.prefillMapping.isNotEmpty()) {
                                        qrConfig.prefillMapping.forEach { mapping ->
                                            when (mapping.qrKey.lowercase()) {
                                                "name", "n" -> decodedData.name?.let {
                                                    viewModel.updateFieldValue(mapping.targetFieldId, it, null)
                                                }
                                                "gender", "g" -> decodedData.gender?.let {
                                                    viewModel.updateFieldValue(mapping.targetFieldId, it, null)
                                                }
                                                "dob", "yob", "date_of_birth" -> decodedData.dob?.let {
                                                    viewModel.updateFieldValue(mapping.targetFieldId, it, null)
                                                }
                                                "aadhaarlast4", "aadhaar_last4", "uid_last4" -> decodedData.aadhaarLast4?.let {
                                                    viewModel.updateFieldValue(mapping.targetFieldId, it, null)
                                                }
                                            }
                                        }
                                    } else {
                                        // Fallback: try to find fields by common names
                                        uiState.formScreen?.sections?.forEach { section ->
                                            section.fields.forEach { field ->
                                                when (field.id.lowercase()) {
                                                    "name", "full_name", "applicant_name" -> decodedData.name?.let {
                                                        viewModel.updateFieldValue(field.id, it, null)
                                                    }
                                                    "gender" -> decodedData.gender?.let {
                                                        viewModel.updateFieldValue(field.id, it, null)
                                                    }
                                                    "dob", "date_of_birth", "yob" -> decodedData.dob?.let {
                                                        viewModel.updateFieldValue(field.id, it, null)
                                                    }
                                                    "aadhaar_last4", "uid_last4" -> decodedData.aadhaarLast4?.let {
                                                        viewModel.updateFieldValue(field.id, it, null)
                                                    }
                                                }
                                            }
                                            
                                            section.subSections.forEach { subSection ->
                                                subSection.fields.forEach { field ->
                                                    when (field.id.lowercase()) {
                                                        "name", "full_name", "applicant_name" -> decodedData.name?.let {
                                                            viewModel.updateFieldValue(field.id, it, null)
                                                        }
                                                        "gender" -> decodedData.gender?.let {
                                                            viewModel.updateFieldValue(field.id, it, null)
                                                        }
                                                        "dob", "date_of_birth", "yob" -> decodedData.dob?.let {
                                                            viewModel.updateFieldValue(field.id, it, null)
                                                        }
                                                        "aadhaar_last4", "uid_last4" -> decodedData.aadhaarLast4?.let {
                                                            viewModel.updateFieldValue(field.id, it, null)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Show success Toast
                                    Toast.makeText(context, "Aadhaar details fetched successfully", Toast.LENGTH_SHORT).show()
                                }.onFailure { error ->
                                    // Close progress dialog and show error
                                    onShowAadhaarDecodeProgressDialog(false)
                                    onSetAadhaarDecodeError(error.message ?: "Unable to read Aadhaar QR")
                                    android.util.Log.e("DynamicFormScreen", "Aadhaar decode failed: ${error.message}", error)
                                }
                            } catch (e: Exception) {
                                // Close progress dialog and show error
                                onShowAadhaarDecodeProgressDialog(false)
                                onSetAadhaarDecodeError(e.message ?: "Unable to read Aadhaar QR")
                                android.util.Log.e("DynamicFormScreen", "Aadhaar QR handling exception: ${e.message}", e)
                            } finally {
                                onSetDecodingAadhaar(false)
                            }
                        }
                    }
                )
            }
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

@Composable
private fun ConsentDialog(
    title: String,
    subTitle: String?,
    message: String,
    positiveButtonText: String,
    negativeButtonText: String,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onNegativeClick,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!subTitle.isNullOrEmpty()) {
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = positiveButtonText,
                onClick = onPositiveClick,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(
                onClick = onNegativeClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(negativeButtonText)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun OtpDialog(
    phoneNumber: String,
    subTitle: String,
    otpLength: Int,
    resendIntervalSeconds: Int,
    onVerify: (String, (Boolean) -> Unit) -> Unit, // Added callback to handle completion
    onDismiss: () -> Unit,
    onResend: () -> Unit
) {
    var otpValue by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf(resendIntervalSeconds) }
    var isVerifying by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Start countdown when dialog opens
        timeRemaining = resendIntervalSeconds
    }
    
    LaunchedEffect(timeRemaining) {
        if (timeRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            timeRemaining--
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enter OTP",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = otpValue,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= otpLength) {
                            otpValue = newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("OTP") },
                    placeholder = { Text("Enter $otpLength digit code") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true,
                    enabled = !isVerifying,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                
                if (timeRemaining > 0) {
                    Text(
                        text = "Resend OTP in ${timeRemaining}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    TextButton(
                        onClick = {
                            timeRemaining = resendIntervalSeconds
                            onResend()
                        },
                        enabled = !isVerifying
                    ) {
                        Text("Resend OTP")
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Verify",
                onClick = {
                    if (otpValue.length == otpLength && !isVerifying) {
                        isVerifying = true
                        onVerify(otpValue) { success ->
                            isVerifying = false
                            if (!success) {
                                // If verification failed, keep dialog open
                                // Don't clear OTP, just reset verifying state
                            }
                        }
                    }
                },
                enabled = otpValue.length == otpLength && !isVerifying,
                isLoading = isVerifying,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isVerifying,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}


