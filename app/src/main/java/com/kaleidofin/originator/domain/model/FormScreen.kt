package com.kaleidofin.originator.domain.model

data class FormScreen(
    val screenId: String,
    val flowId: String,
    val title: String,
    val layout: FormLayout,
    val hiddenFields: List<HiddenField>,
    val sections: List<FormSection>,
    val actions: List<FormAction>,
    val modals: List<FormModal>,
    val validations: FormValidations? = null
)

data class FormLayout(
    val type: String,
    val submitButtonText: String,
    val stickyFooter: Boolean,
    val enableSubmitWhen: List<SubmitCondition>,
    val allowBackNavigation: Boolean = true // Default to true for backward compatibility
)

data class SubmitCondition(
    val type: String,
    val field: String?,
    val value: Any?
)

data class HiddenField(
    val id: String,
    val type: String,
    val defaultValue: Any?
)

data class FormSection(
    val sectionId: String,
    val title: String,
    val collapsible: Boolean,
    val expanded: Boolean,
    val repeatable: Boolean,
    val minInstances: Int,
    val maxInstances: Int?,
    val addButtonText: String?,
    val removeButtonText: String?,
    val instanceLabel: String?,
    val validationRules: List<ValidationRule>,
    val fields: List<FormField>,
    val subSections: List<FormSection>,
    val subSectionOf: String?
)

data class ValidationRule(
    val type: String,
    val field: String?,
    val message: String?
)

data class FormField(
    val id: String,
    val type: String,
    val label: String,
    val placeholder: String?,
    val keyboard: String?,
    val maxLength: Int?,
    val required: Boolean,
    val readOnly: Boolean = false,
    val value: Any?,
    val dataSource: FieldDataSource?,
    val enabledWhen: List<EnabledCondition>,
    val verification: FieldVerification?,
    val validation: FieldValidation?,
    val constraints: FieldConstraints?,
    val min: Int?,
    val max: Int?,
    val dateMode: String?,
    val minDate: String?,
    val maxDate: String?,
    val dateConfig: DateConfig? = null,
    val verifiedInputConfig: VerifiedInputConfig? = null,
    val apiVerificationConfig: ApiVerificationConfig? = null
)

data class FieldDataSource(
    val type: String, // "INLINE", "MASTER", "API"
    val values: List<String>?, // For INLINE
    val key: String?, // For MASTER
    val endpoint: String?, // For API
    val method: String?, // For API
    val dependsOn: String?, // For API
    val paramKey: String? // For API
)

data class EnabledCondition(
    val field: String,
    val operator: String, // "EQUALS", "NOT_EQUALS", "IN", etc.
    val value: Any
)

data class FieldVerification(
    val enabled: Boolean,
    val type: String,
    val trigger: String,
    val modalId: String,
    val statusField: String,
    val showStatusIcon: Boolean
)

data class FieldValidation(
    val regex: String,
    val errorMessage: String
)

data class FieldConstraints(
    val minAge: Int?,
    val maxAge: Int?
)

data class DateConfig(
    val format: String?,
    val validationType: String?,
    val minAge: Int?,
    val maxAge: Int?,
    val minDate: String?,
    val maxDate: String?,
    val offset: Int?,
    val unit: String?
)

data class FormAction(
    val type: String,
    val api: String,
    val method: String,
    val nextScreen: String?
)

data class FormModal(
    val modalId: String,
    val type: String,
    val header: ModalHeader?,
    val otp: OtpConfig?,
    val consentText: String?,
    val actions: List<ModalAction>
)

data class ModalHeader(
    val title: String,
    val icon: String?
)

data class OtpConfig(
    val length: Int
)

data class ModalAction(
    val type: String,
    val label: String?,
    val api: String,
    val onSuccess: SuccessAction?
)

data class SuccessAction(
    val updateField: String,
    val value: Any,
    val closeModal: Boolean
)

data class VerifiedInputConfig(
    val input: VerifiedInputInput?,
    val verification: VerifiedInputVerification?
)

data class VerifiedInputInput(
    val dataType: String?,
    val keyboard: String?,
    val maxLength: Int?,
    val min: Int?,
    val max: Int?
)

data class VerifiedInputVerification(
    val mode: String?,
    val messages: Map<String, String>?,
    val showDialog: Boolean?,
    val otp: VerifiedInputOtp?,
    val api: VerifiedInputApi?
)

data class VerifiedInputOtp(
    val channel: String?,
    val otpLength: Int?,
    val resendIntervalSeconds: Int?,
    val consent: VerifiedInputConsent?,
    val api: VerifiedInputOtpApi?
)

data class VerifiedInputConsent(
    val title: String?,
    val subTitle: String?,
    val message: String?,
    val positiveButtonText: String?,
    val negativeButtonText: String?
)

data class VerifiedInputOtpApi(
    val sendOtp: VerifiedInputApiEndpoint?,
    val verifyOtp: VerifiedInputApiEndpoint?
)

data class VerifiedInputApiEndpoint(
    val endpoint: String?,
    val method: String?
)

data class VerifiedInputApi(
    val endpoint: String?,
    val method: String?,
    val successCondition: Map<String, Any>?
)

data class ApiVerificationConfig(
    val endpoint: String?,
    val method: String?,
    val requestMapping: String?,
    val successCondition: ApiVerificationSuccessCondition?,
    val messages: Map<String, String>?,
    val showDialog: Boolean?
)

data class ApiVerificationSuccessCondition(
    val field: String?,
    val equals: String?
)

data class FormValidations(
    val rules: List<FormValidationRule>
)

data class FormValidationRule(
    val id: String?,
    val fieldId: String?,
    val type: String,
    val message: String?,
    val pattern: String?,
    val executionTarget: String?
)
