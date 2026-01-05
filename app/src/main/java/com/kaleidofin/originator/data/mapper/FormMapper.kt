package com.kaleidofin.originator.data.mapper

import com.kaleidofin.originator.data.dto.*
import com.kaleidofin.originator.domain.model.*

fun FormScreenDto.toDomain(): FormScreen {
    return FormScreen(
        screenId = screenId,
        flowId = flowId,
        title = title,
        layout = layout.toDomain(),
        hiddenFields = hiddenFields?.map { it.toDomain() } ?: emptyList(),
        sections = sections.map { it.toDomain() },
        actions = actions.map { it.toDomain() },
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
        sectionId = sectionId,
        title = title,
        collapsible = collapsible,
        expanded = expanded ?: true,
        repeatable = repeatable ?: false,
        minInstances = minInstances ?: 1,
        maxInstances = maxInstances,
        addButtonText = addButtonText,
        removeButtonText = removeButtonText,
        instanceLabel = instanceLabel,
        validationRules = validationRules?.map { it.toDomain() } ?: emptyList(),
        fields = fields.map { it.toDomain() },
        subSections = subSections?.map { it.toDomain() } ?: emptyList(),
        subSectionOf = subSectionOf
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
        maxLength = maxLength,
        required = required,
        value = value,
        dataSource = dataSource?.toDomain(),
        enabledWhen = enabledWhen?.map { it.toDomain() } ?: emptyList(),
        verification = verification?.toDomain(),
        validation = validation?.toDomain(),
        constraints = constraints?.toDomain(),
        min = min,
        max = max,
        dateMode = dateMode,
        minDate = minDate,
        maxDate = maxDate
    )
}

fun DataSourceDto.toDomain(): FieldDataSource {
    return FieldDataSource(
        type = type,
        values = values,
        key = key,
        endpoint = endpoint,
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
        type = type,
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
        actions = actions.map { it.toDomain() }
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
