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
        modals = modals?.map { it.toDomain() } ?: emptyList()
    )
}

fun FormLayoutDto.toDomain(): FormLayout {
    return FormLayout(
        type = type,
        submitButtonText = submitButtonText,
        stickyFooter = stickyFooter,
        enableSubmitWhen = enableSubmitWhen?.map { it.toDomain() } ?: emptyList()
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
    return FormSection(
        sectionId = actualSectionId,
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
        subSections = subSections?.map { it.toDomain() } ?: emptyList(),
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
        enabledWhen = enabledWhenList.map { it.toDomain() },
        verification = verification?.toDomain(),
        validation = validation?.toDomain(),
        constraints = constraints?.toDomain(),
        min = minInt,
        max = maxInt,
        dateMode = actualDateMode,
        minDate = actualMinDate,
        maxDate = actualMaxDate
    )
}

fun DataSourceDto.toDomain(): FieldDataSource {
    // Normalize type names
    val normalizedType = when (type.uppercase()) {
        "STATIC_JSON" -> "INLINE"
        "MASTER_DATA" -> "MASTER"
        else -> type
    }
    
    return FieldDataSource(
        type = normalizedType,
        values = actualValues,
        key = actualKey,
        endpoint = actualEndpoint,
        method = method,
        dependsOn = dependsOn,
        paramKey = paramKey
    )
}

fun EnabledConditionDto.toDomain(): EnabledCondition {
    return EnabledCondition(
        field = field,
        operator = operator,
        value = value
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
