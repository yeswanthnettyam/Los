package com.kaleidofin.originator.data.dto

import com.google.gson.annotations.SerializedName

data class FormScreenDto(
    @SerializedName("screenId") val screenId: String,
    @SerializedName("flowId") val flowId: String,
    @SerializedName("title") val title: String,
    @SerializedName("layout") val layout: FormLayoutDto,
    @SerializedName("hiddenFields") val hiddenFields: List<HiddenFieldDto>? = emptyList(),
    @SerializedName("sections") val sections: List<SectionDto>,
    @SerializedName("actions") val actions: List<FormActionDto>,
    @SerializedName("modals") val modals: List<ModalDto>? = emptyList()
)

data class FormLayoutDto(
    @SerializedName("type") val type: String,
    @SerializedName("submitButtonText") val submitButtonText: String,
    @SerializedName("stickyFooter") val stickyFooter: Boolean,
    @SerializedName("enableSubmitWhen") val enableSubmitWhen: List<SubmitConditionDto>? = emptyList()
)

data class SubmitConditionDto(
    @SerializedName("type") val type: String, // "ALL_FIELDS_VALID", "FIELD_EQUALS"
    @SerializedName("field") val field: String? = null,
    @SerializedName("value") val value: Any? = null
)

data class HiddenFieldDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("defaultValue") val defaultValue: Any?
)

data class SectionDto(
    @SerializedName("sectionId") val sectionId: String,
    @SerializedName("title") val title: String,
    @SerializedName("collapsible") val collapsible: Boolean,
    @SerializedName("expanded") val expanded: Boolean? = true,
    @SerializedName("repeatable") val repeatable: Boolean? = false,
    @SerializedName("minInstances") val minInstances: Int? = 1,
    @SerializedName("maxInstances") val maxInstances: Int? = null,
    @SerializedName("addButtonText") val addButtonText: String? = null,
    @SerializedName("removeButtonText") val removeButtonText: String? = null,
    @SerializedName("instanceLabel") val instanceLabel: String? = null,
    @SerializedName("validationRules") val validationRules: List<ValidationRuleDto>? = emptyList(),
    @SerializedName("fields") val fields: List<FieldDto> = emptyList(),
    @SerializedName("subSections") val subSections: List<SectionDto>? = emptyList(),
    @SerializedName("subSectionOf") val subSectionOf: String? = null
)

data class ValidationRuleDto(
    @SerializedName("type") val type: String,
    @SerializedName("field") val field: String? = null,
    @SerializedName("message") val message: String? = null
)

data class FieldDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("label") val label: String,
    @SerializedName("placeholder") val placeholder: String? = null,
    @SerializedName("keyboard") val keyboard: String? = null,
    @SerializedName("maxLength") val maxLength: Int? = null,
    @SerializedName("required") val required: Boolean = false,
    @SerializedName("value") val value: Any? = null,
    @SerializedName("dataSource") val dataSource: DataSourceDto? = null,
    @SerializedName("enabledWhen") val enabledWhen: List<EnabledConditionDto>? = emptyList(),
    @SerializedName("verification") val verification: VerificationDto? = null,
    @SerializedName("validation") val validation: ValidationDto? = null,
    @SerializedName("constraints") val constraints: FieldConstraintsDto? = null,
    @SerializedName("min") val min: Int? = null,
    @SerializedName("max") val max: Int? = null,
    @SerializedName("dateMode") val dateMode: String? = null,
    @SerializedName("minDate") val minDate: String? = null,
    @SerializedName("maxDate") val maxDate: String? = null
)

data class DataSourceDto(
    @SerializedName("type") val type: String, // "INLINE", "MASTER", "API"
    @SerializedName("values") val values: List<String>? = null, // For INLINE
    @SerializedName("key") val key: String? = null, // For MASTER
    @SerializedName("endpoint") val endpoint: String? = null, // For API
    @SerializedName("method") val method: String? = null, // For API
    @SerializedName("dependsOn") val dependsOn: String? = null, // For API
    @SerializedName("paramKey") val paramKey: String? = null // For API
)

data class EnabledConditionDto(
    @SerializedName("field") val field: String,
    @SerializedName("operator") val operator: String, // "EQUALS", "NOT_EQUALS", "IN", etc.
    @SerializedName("value") val value: Any
)

data class VerificationDto(
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("type") val type: String,
    @SerializedName("trigger") val trigger: String,
    @SerializedName("modalId") val modalId: String,
    @SerializedName("statusField") val statusField: String,
    @SerializedName("showStatusIcon") val showStatusIcon: Boolean
)

data class ValidationDto(
    @SerializedName("regex") val regex: String,
    @SerializedName("errorMessage") val errorMessage: String
)

data class FieldConstraintsDto(
    @SerializedName("minAge") val minAge: Int? = null,
    @SerializedName("maxAge") val maxAge: Int? = null
)

data class FormActionDto(
    @SerializedName("type") val type: String,
    @SerializedName("api") val api: String,
    @SerializedName("method") val method: String,
    @SerializedName("nextScreen") val nextScreen: String? = null
)

data class ModalDto(
    @SerializedName("modalId") val modalId: String,
    @SerializedName("type") val type: String,
    @SerializedName("header") val header: ModalHeaderDto? = null,
    @SerializedName("otp") val otp: OtpConfigDto? = null,
    @SerializedName("consentText") val consentText: String? = null,
    @SerializedName("actions") val actions: List<ModalActionDto>
)

data class ModalHeaderDto(
    @SerializedName("title") val title: String,
    @SerializedName("icon") val icon: String? = null
)

data class OtpConfigDto(
    @SerializedName("length") val length: Int
)

data class ModalActionDto(
    @SerializedName("type") val type: String,
    @SerializedName("label") val label: String? = null,
    @SerializedName("api") val api: String,
    @SerializedName("onSuccess") val onSuccess: SuccessActionDto? = null
)

data class SuccessActionDto(
    @SerializedName("updateField") val updateField: String,
    @SerializedName("value") val value: Any,
    @SerializedName("closeModal") val closeModal: Boolean
)
