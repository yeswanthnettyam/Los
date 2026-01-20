package com.kaleidofin.originator.data.mapper

import com.kaleidofin.originator.data.dto.*
import com.kaleidofin.originator.domain.model.*

fun FormScreenDto.toDomain(): FormScreen {
    return FormScreen(
        screenId = screenId,
        flowId = flowId ?: "",
        title = title,
        layout = actualLayout.toDomain(),
        hiddenFields = hiddenFields?.map { it.toDomain() } ?: emptyList(),
        sections = actualSections.map { it.toDomain() },
        actions = actualActions.map { it.toDomain() },
        modals = modals?.map { it.toDomain() } ?: emptyList(),
        validations = validations?.toDomain()
    )
}

fun FormLayoutDto.toDomain(): FormLayout {
    return FormLayout(
        type = type,
        submitButtonText = submitButtonText,
        stickyFooter = stickyFooter,
        enableSubmitWhen = enableSubmitWhen?.map { it.toDomain() } ?: emptyList(),
        allowBackNavigation = allowBackNavigation ?: true
    )
}

fun SubmitConditionDto.toDomain(): SubmitCondition {
    return SubmitCondition(
        type = type,
        field = field,
        value = value
    )
}

fun HiddenFieldDto.toDomain(): HiddenField {
    return HiddenField(
        id = id,
        type = type,
        defaultValue = defaultValue
    )
}

fun SectionDto.toDomain(): FormSection {
    val parentSectionId = actualSectionId
    return FormSection(
        sectionId = parentSectionId,
        title = title,
        collapsible = collapsible ?: false,
        expanded = actualExpanded,
        repeatable = repeatable ?: false,
        minInstances = minInstances ?: 1,
        maxInstances = maxInstances,
        addButtonText = addButtonText,
        removeButtonText = removeButtonText,
        instanceLabel = instanceLabel,
        validationRules = validationRules?.map { it.toDomain() } ?: emptyList(),
        fields = fields?.map { it.toDomain() } ?: emptyList(),
        // Map subsections and ensure they have the correct parent reference
        subSections = subSections?.map { subSectionDto ->
            // If subsection doesn't have parentSectionId set, set it to this section's ID
            val subSectionWithParent = if (subSectionDto.parentSectionId == null && subSectionDto.subSectionOf == null) {
                subSectionDto.copy(parentSectionId = parentSectionId)
            } else {
                subSectionDto
            }
            subSectionWithParent.toDomain()
        } ?: emptyList(),
        subSectionOf = actualSubSectionOf
    )
}

fun ValidationRuleDto.toDomain(): ValidationRule {
    return ValidationRule(
        type = type,
        field = field,
        message = message
    )
}

fun FieldDto.toDomain(): FormField {
    return FormField(
        id = id,
        type = type,
        label = label,
        placeholder = placeholder,
        keyboard = keyboard,
        maxLength = maxLengthInt,
        required = required,
        readOnly = readOnly,
        value = value,
        dataSource = dataSource?.toDomain(),
        enabledWhen = enabledWhenCondition?.toDomain(),
        visibleWhen = visibleWhenCondition?.toDomain(),
        requiredWhen = requiredWhenCondition?.toDomain(),
        verification = verification?.toDomain(),
        validation = validation?.toDomain(),
        constraints = constraints?.toDomain(),
        min = minInt,
        max = maxInt,
        dateMode = actualDateMode,
        minDate = actualMinDate,
        maxDate = actualMaxDate,
        dateConfig = dateConfig?.toDomain(),
        verifiedInputConfig = verifiedInputConfig?.toDomain(),
        apiVerificationConfig = apiVerificationConfig?.toDomain(),
        selectionMode = selectionMode,
        minSelections = minSelections,
        maxSelections = maxSelections,
        cameraConfig = cameraConfig?.toDomain(),
        webViewConfig = webViewConfig?.toDomain(),
        qrConfig = qrConfig?.toDomain()
    )
}

fun DateConfigDto.toDomain(): com.kaleidofin.originator.domain.model.DateConfig {
    return com.kaleidofin.originator.domain.model.DateConfig(
        format = format,
        validationType = validationType,
        minAge = minAge,
        maxAge = maxAge,
        minDate = minDate,
        maxDate = maxDate,
        offset = offset,
        unit = unit
    )
}

fun DataSourceDto.toDomain(): FieldDataSource {
    // Normalize type names - handle null type gracefully
    val normalizedType = when {
        type == null -> "INLINE" // Default to INLINE if type is null
        else -> when (type.uppercase()) {
            "STATIC_JSON" -> "INLINE"
            "MASTER_DATA" -> "MASTER"
            else -> type
        }
    }
    
    return FieldDataSource(
        type = normalizedType,
        values = actualValues,
        key = actualKey,
        endpoint = actualEndpoint,
        method = method,
        dependsOn = dependsOn,
        paramKey = paramKey,
        staticData = staticData?.map { it.toDomain() }
    )
}

fun StaticDataItemDto.toDomain(): com.kaleidofin.originator.domain.model.StaticDataItem {
    return com.kaleidofin.originator.domain.model.StaticDataItem(
        value = value,
        label = label
    )
}

fun DependencyConditionDto.toDomain(): com.kaleidofin.originator.domain.model.DependencyCondition {
    return when (this) {
        is DependencyConditionDto.ConditionDto -> {
            com.kaleidofin.originator.domain.model.DependencyCondition.Condition(
                field = field,
                operator = operator,
                value = value
            )
        }
        is DependencyConditionDto.ConditionGroupDto -> {
            com.kaleidofin.originator.domain.model.DependencyCondition.ConditionGroup(
                operator = operator,
                conditions = conditions.map { it.toDomain() }
            )
        }
    }
}

// Legacy mapper for backward compatibility
@Deprecated("Use DependencyConditionDto.toDomain() instead", ReplaceWith("toDomain()"))
fun EnabledConditionDto.toDomain(): EnabledCondition {
    return EnabledCondition(
        field = field,
        operator = operator,
        value = value ?: ""
    )
}

fun VerificationDto.toDomain(): FieldVerification {
    return FieldVerification(
        enabled = enabled,
        type = type,
        trigger = trigger,
        modalId = modalId,
        statusField = statusField,
        showStatusIcon = showStatusIcon
    )
}

fun ValidationDto.toDomain(): FieldValidation {
    return FieldValidation(
        regex = regex,
        errorMessage = errorMessage
    )
}

fun FieldConstraintsDto.toDomain(): FieldConstraints {
    return FieldConstraints(
        minAge = minAge,
        maxAge = maxAge
    )
}

fun FormActionDto.toDomain(): FormAction {
    return FormAction(
        type = type ?: "SUBMIT",
        api = api,
        method = method,
        nextScreen = nextScreen
    )
}

fun ModalDto.toDomain(): FormModal {
    return FormModal(
        modalId = modalId,
        type = type,
        header = header?.toDomain(),
        otp = otp?.toDomain(),
        consentText = consentText,
        actions = actions?.map { it.toDomain() } ?: emptyList()
    )
}

fun ModalHeaderDto.toDomain(): ModalHeader {
    return ModalHeader(
        title = title,
        icon = icon
    )
}

fun OtpConfigDto.toDomain(): OtpConfig {
    return OtpConfig(length = length)
}

fun ModalActionDto.toDomain(): ModalAction {
    return ModalAction(
        type = type,
        label = label,
        api = api,
        onSuccess = onSuccess?.toDomain()
    )
}

fun SuccessActionDto.toDomain(): SuccessAction {
    return SuccessAction(
        updateField = updateField,
        value = value,
        closeModal = closeModal
    )
}

fun VerifiedInputConfigDto.toDomain(): com.kaleidofin.originator.domain.model.VerifiedInputConfig {
    return com.kaleidofin.originator.domain.model.VerifiedInputConfig(
        input = input?.toDomain(),
        verification = verification?.toDomain()
    )
}

fun VerifiedInputInputDto.toDomain(): com.kaleidofin.originator.domain.model.VerifiedInputInput {
    return com.kaleidofin.originator.domain.model.VerifiedInputInput(
        dataType = dataType,
        keyboard = keyboard,
        maxLength = maxLength?.toIntOrNull(),
        min = min?.toIntOrNull(),
        max = max?.toIntOrNull()
    )
}

fun VerifiedInputVerificationDto.toDomain(): com.kaleidofin.originator.domain.model.VerifiedInputVerification {
    return com.kaleidofin.originator.domain.model.VerifiedInputVerification(
        mode = mode,
        messages = messages,
        showDialog = showDialog,
        otp = otp?.toDomain(),
        api = api?.toDomain()
    )
}

fun VerifiedInputOtpDto.toDomain(): com.kaleidofin.originator.domain.model.VerifiedInputOtp {
    val apiMap = api ?: emptyMap()
    return com.kaleidofin.originator.domain.model.VerifiedInputOtp(
        channel = channel,
        otpLength = otpLength?.toIntOrNull(),
        resendIntervalSeconds = resendIntervalSeconds?.toIntOrNull(),
        consent = consent?.toDomain(),
        api = if (apiMap.isNotEmpty()) {
            com.kaleidofin.originator.domain.model.VerifiedInputOtpApi(
                sendOtp = (apiMap["sendOtp"] as? Map<*, *>)?.let { map ->
                    com.kaleidofin.originator.domain.model.VerifiedInputApiEndpoint(
                        endpoint = map["endpoint"] as? String,
                        method = map["method"] as? String
                    )
                },
                verifyOtp = (apiMap["verifyOtp"] as? Map<*, *>)?.let { map ->
                    com.kaleidofin.originator.domain.model.VerifiedInputApiEndpoint(
                        endpoint = map["endpoint"] as? String,
                        method = map["method"] as? String
                    )
                }
            )
        } else {
            null
        }
    )
}

fun VerifiedInputConsentDto.toDomain(): com.kaleidofin.originator.domain.model.VerifiedInputConsent {
    return com.kaleidofin.originator.domain.model.VerifiedInputConsent(
        title = title,
        subTitle = subTitle,
        message = message,
        positiveButtonText = positiveButtonText,
        negativeButtonText = negativeButtonText
    )
}

fun VerifiedInputApiDto.toDomain(): com.kaleidofin.originator.domain.model.VerifiedInputApi {
    return com.kaleidofin.originator.domain.model.VerifiedInputApi(
        endpoint = endpoint,
        method = method,
        successCondition = successCondition
    )
}

fun ApiVerificationConfigDto.toDomain(): com.kaleidofin.originator.domain.model.ApiVerificationConfig {
    return com.kaleidofin.originator.domain.model.ApiVerificationConfig(
        endpoint = endpoint,
        method = method,
        requestMapping = requestMapping,
        successCondition = if (successCondition is Map<*, *>) {
            com.kaleidofin.originator.domain.model.ApiVerificationSuccessCondition(
                field = successCondition["field"] as? String,
                equals = successCondition["equals"] as? String
            )
        } else {
            null
        },
        messages = messages,
        showDialog = showDialog
    )
}

fun FormValidationsDto.toDomain(): com.kaleidofin.originator.domain.model.FormValidations {
    return com.kaleidofin.originator.domain.model.FormValidations(
        rules = rules?.map { it.toDomain() } ?: emptyList()
    )
}

fun FormValidationRuleDto.toDomain(): com.kaleidofin.originator.domain.model.FormValidationRule {
    return com.kaleidofin.originator.domain.model.FormValidationRule(
        id = id,
        fieldId = fieldId,
        type = type,
        message = message,
        pattern = pattern,
        executionTarget = executionTarget
    )
}

fun CameraConfigDto.toDomain(): com.kaleidofin.originator.domain.model.CameraConfig {
    return com.kaleidofin.originator.domain.model.CameraConfig(
        cameraType = cameraType ?: "BACK",
        minWidth = minWidth,
        minHeight = minHeight,
        enableBlurDetection = enableBlurDetection ?: true,
        uploadApi = uploadApi?.toDomain()
    )
}

fun CameraUploadApiDto.toDomain(): com.kaleidofin.originator.domain.model.CameraUploadApi {
    return com.kaleidofin.originator.domain.model.CameraUploadApi(
        endpoint = endpoint,
        method = method ?: "POST"
    )
}

fun WebViewConfigDto.toDomain(): com.kaleidofin.originator.domain.model.WebViewConfig {
    return com.kaleidofin.originator.domain.model.WebViewConfig(
        urlSource = urlSource,
        staticUrl = staticUrl,
        launchApi = launchApi?.toDomain()
    )
}

fun WebViewLaunchApiDto.toDomain(): com.kaleidofin.originator.domain.model.WebViewLaunchApi {
    return com.kaleidofin.originator.domain.model.WebViewLaunchApi(
        endpoint = endpoint,
        method = method ?: "POST"
    )
}

fun QRConfigDto.toDomain(): com.kaleidofin.originator.domain.model.QRConfig {
    return com.kaleidofin.originator.domain.model.QRConfig(
        format = format ?: "JSON",
        prefillMapping = prefillMapping.map { it.toDomain() }
    )
}

fun QRPrefillMappingDto.toDomain(): com.kaleidofin.originator.domain.model.QRPrefillMapping {
    return com.kaleidofin.originator.domain.model.QRPrefillMapping(
        targetFieldId = targetFieldId,
        qrKey = qrKey
    )
}
