package com.kaleidofin.originator.data.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class FormScreenDto(
    @SerializedName("screenId") val screenId: String,
    @SerializedName("flowId") val flowId: String? = null,
    @SerializedName("title") val title: String,
    @SerializedName("version") val version: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("scope") val scope: ScopeDto? = null,
    @SerializedName("ui") val ui: FormUiDto? = null,
    // Legacy support - if ui is null, use these directly
    @SerializedName("layout") val layout: FormLayoutDto? = null,
    @SerializedName("hiddenFields") val hiddenFields: List<HiddenFieldDto>? = emptyList(),
    @SerializedName("sections") val sections: List<SectionDto>? = null,
    @SerializedName("actions") val actions: List<FormActionDto>? = null,
    @SerializedName("modals") val modals: List<ModalDto>? = emptyList(),
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("createdBy") val createdBy: String? = null,
    @SerializedName("updatedBy") val updatedBy: String? = null,
    @SerializedName("validations") val validations: FormValidationsDto? = null
) {
    // Helper properties to get values from ui or direct
    val actualLayout: FormLayoutDto
        get() = ui?.actualLayout ?: layout ?: throw IllegalStateException("No layout found")
    
    val actualSections: List<SectionDto>
        get() = ui?.sections?.takeIf { it.isNotEmpty() } ?: sections ?: emptyList()
    
    val actualActions: List<FormActionDto>
        get() = ui?.actions?.takeIf { it.isNotEmpty() } ?: actions ?: emptyList()
}

data class FormUiDto(
    @SerializedName("layout") val layout: JsonElement, // Can be string or object
    @SerializedName("sections") val sections: List<SectionDto>? = null,
    @SerializedName("actions") val actions: List<FormActionDto>? = null
) {
    // Helper to get FormLayoutDto from layout (handles both string and object)
    val actualLayout: FormLayoutDto
        get() {
            return when {
                layout.isJsonPrimitive -> {
                    val primitive = layout.asJsonPrimitive
                    if (primitive.isString) {
                        // Layout is a string like "FORM" - create default FormLayoutDto
                        FormLayoutDto(
                            type = primitive.asString,
                            submitButtonText = "Submit",
                            stickyFooter = false,
                            enableSubmitWhen = emptyList()
                        )
                    } else {
                        // Fallback for non-string primitives
                        FormLayoutDto(
                            type = "FORM",
                            submitButtonText = "Submit",
                            stickyFooter = false,
                            enableSubmitWhen = emptyList()
                        )
                    }
                }
                layout.isJsonObject -> {
                    // Layout is an object - deserialize normally
                    val obj = layout.asJsonObject
                    FormLayoutDto(
                        type = obj.get("type")?.asString ?: "FORM",
                        submitButtonText = obj.get("submitButtonText")?.asString ?: "Submit",
                        stickyFooter = obj.get("stickyFooter")?.asBoolean ?: false,
                        enableSubmitWhen = obj.get("enableSubmitWhen")?.asJsonArray?.mapNotNull { element ->
                            if (element.isJsonObject) {
                                val condObj = element.asJsonObject
                                SubmitConditionDto(
                                    type = condObj.get("type")?.asString ?: "",
                                    field = condObj.get("field")?.asString,
                                    value = condObj.get("value")?.let { 
                                        when {
                                            it.isJsonPrimitive -> {
                                                val prim = it.asJsonPrimitive
                                                when {
                                                    prim.isString -> prim.asString
                                                    prim.isNumber -> prim.asNumber
                                                    prim.isBoolean -> prim.asBoolean
                                                    else -> prim.asString
                                                }
                                            }
                                            else -> it.toString()
                                        }
                                    }
                                )
                            } else {
                                null
                            }
                        } ?: emptyList()
                    )
                }
                else -> {
                    // Fallback to default
                    FormLayoutDto(
                        type = "FORM",
                        submitButtonText = "Submit",
                        stickyFooter = false,
                        enableSubmitWhen = emptyList()
                    )
                }
            }
        }
}

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
    @SerializedName("id") val id: String? = null,
    @SerializedName("sectionId") val sectionId: String? = null, // Legacy support
    @SerializedName("title") val title: String,
    @SerializedName("collapsible") val collapsible: Boolean? = false,
    @SerializedName("defaultExpanded") val defaultExpanded: Boolean? = null,
    @SerializedName("expanded") val expanded: Boolean? = null, // Legacy support
    @SerializedName("repeatable") val repeatable: Boolean? = false,
    @SerializedName("minInstances") val minInstances: Int? = 1,
    @SerializedName("maxInstances") val maxInstances: Int? = null,
    @SerializedName("addButtonText") val addButtonText: String? = null,
    @SerializedName("removeButtonText") val removeButtonText: String? = null,
    @SerializedName("instanceLabel") val instanceLabel: String? = null,
    @SerializedName("order") val order: Int? = null,
    @SerializedName("validationRules") val validationRules: List<ValidationRuleDto>? = emptyList(),
    @SerializedName("fields") val fields: List<FieldDto>? = emptyList(),
    @SerializedName("subSections") val subSections: List<SectionDto>? = emptyList(),
    @SerializedName("subSectionOf") val subSectionOf: String? = null, // Legacy support
    @SerializedName("parentSectionId") val parentSectionId: String? = null
) {
    // Helper property to get sectionId from id or sectionId
    val actualSectionId: String
        get() = when {
            !id.isNullOrEmpty() -> id!!
            !sectionId.isNullOrEmpty() -> sectionId!!
            else -> "" // Fallback, but this should not happen in valid JSON
        }
    
    // Helper property to get expanded state
    val actualExpanded: Boolean
        get() = defaultExpanded ?: expanded ?: true
    
    // Helper property to get subSectionOf
    val actualSubSectionOf: String?
        get() = parentSectionId ?: subSectionOf
}

data class ValidationRuleDto(
    @SerializedName("type") val type: String,
    @SerializedName("field") val field: String? = null,
    @SerializedName("message") val message: String? = null
)

data class FieldDto(
    @SerializedName("id") val id: String,
    @SerializedName("_key") val key: String? = null,
    @SerializedName("type") val type: String,
    @SerializedName("label") val label: String,
    @SerializedName("placeholder") val placeholder: String? = null,
    @SerializedName("keyboard") val keyboard: String? = null,
    @SerializedName("maxLength") val maxLength: String? = null, // Can be string or int in JSON
    @SerializedName("required") val required: Boolean = false,
    @SerializedName("readOnly") val readOnly: Boolean = false,
    @SerializedName("order") val order: Int? = null,
    @SerializedName("parentId") val parentId: String? = null,
    @SerializedName("parentType") val parentType: String? = null,
    @SerializedName("value") val value: Any? = null,
    @SerializedName("dataSource") val dataSource: DataSourceDto? = null,
    @SerializedName("enabledWhen") val enabledWhen: JsonElement? = null, // Can be object or list
    @SerializedName("visibleWhen") val visibleWhen: JsonElement? = null, // Object
    @SerializedName("requiredWhen") val requiredWhen: JsonElement? = null, // Object
    @SerializedName("verification") val verification: VerificationDto? = null,
    @SerializedName("validation") val validation: ValidationDto? = null,
    @SerializedName("constraints") val constraints: FieldConstraintsDto? = null,
    @SerializedName("min") val min: String? = null, // Can be string or int in JSON
    @SerializedName("max") val max: String? = null, // Can be string or int in JSON
    @SerializedName("dateMode") val dateMode: String? = null, // Legacy support
    @SerializedName("minDate") val minDate: String? = null, // Legacy support
    @SerializedName("maxDate") val maxDate: String? = null, // Legacy support
    @SerializedName("dateConfig") val dateConfig: DateConfigDto? = null,
    @SerializedName("verifiedInputConfig") val verifiedInputConfig: VerifiedInputConfigDto? = null,
    @SerializedName("otpConfig") val otpConfig: OtpConfigDto? = null,
    @SerializedName("apiVerificationConfig") val apiVerificationConfig: ApiVerificationConfigDto? = null,
    @SerializedName("allowedFileTypes") val allowedFileTypes: List<String>? = null,
    @SerializedName("maxFileSizeMB") val maxFileSizeMB: String? = null,
    @SerializedName("maxFiles") val maxFiles: String? = null
) {
    // Helper to convert maxLength to Int
    val maxLengthInt: Int?
        get() = maxLength?.toIntOrNull()
    
    // Helper to convert min/max to Int
    val minInt: Int?
        get() = min?.toIntOrNull()
    
    val maxInt: Int?
        get() = max?.toIntOrNull()
    
    // Helper to convert enabledWhen to list
    val enabledWhenList: List<EnabledConditionDto>
        get() {
            return when {
                enabledWhen == null || enabledWhen.isJsonNull -> emptyList()
                enabledWhen.isJsonObject -> {
                    val obj = enabledWhen.asJsonObject
                    // Handle empty object {} - return empty list
                    if (obj.size() == 0) {
                        emptyList()
                    } else {
                        // Convert object to list
                        val field = obj.get("field")?.asString
                        val operator = obj.get("operator")?.asString
                        val valueElement = obj.get("value")
                        val value = when {
                            valueElement == null || valueElement.isJsonNull -> null
                            valueElement.isJsonPrimitive -> {
                                val primitive = valueElement.asJsonPrimitive
                                when {
                                    primitive.isString -> primitive.asString
                                    primitive.isNumber -> primitive.asNumber
                                    primitive.isBoolean -> primitive.asBoolean
                                    else -> primitive.asString
                                }
                            }
                            else -> valueElement.toString()
                        }
                        if (field != null && operator != null && field.isNotEmpty() && operator.isNotEmpty() && value != null) {
                            listOf(EnabledConditionDto(field, operator, value))
                        } else {
                            emptyList()
                        }
                    }
                }
                enabledWhen.isJsonArray -> {
                    // Already a list
                    enabledWhen.asJsonArray.mapNotNull { element ->
                        if (element.isJsonObject) {
                            val obj = element.asJsonObject
                            val field = obj.get("field")?.asString
                            val operator = obj.get("operator")?.asString
                            val valueElement = obj.get("value")
                            val value = when {
                                valueElement == null || valueElement.isJsonNull -> null
                                valueElement.isJsonPrimitive -> {
                                    val primitive = valueElement.asJsonPrimitive
                                    when {
                                        primitive.isString -> primitive.asString
                                        primitive.isNumber -> primitive.asNumber
                                        primitive.isBoolean -> primitive.asBoolean
                                        else -> primitive.asString
                                    }
                                }
                                else -> valueElement.toString()
                            }
                            if (field != null && operator != null && field.isNotEmpty() && operator.isNotEmpty() && value != null) {
                                EnabledConditionDto(field, operator, value)
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
                }
                else -> emptyList()
            }
        }
    
    // Helper to get dateMode from dateConfig
    val actualDateMode: String?
        get() = dateConfig?.validationType ?: dateMode
    
    val actualMinDate: String?
        get() = dateConfig?.minDate ?: minDate
    
    val actualMaxDate: String?
        get() = dateConfig?.maxDate ?: maxDate
}

data class DataSourceDto(
    @SerializedName("type") val type: String, // "INLINE", "MASTER", "MASTER_DATA", "API", "STATIC_JSON"
    @SerializedName("values") val values: List<String>? = null, // For INLINE (legacy)
    @SerializedName("staticData") val staticData: List<StaticDataItemDto>? = null, // For STATIC_JSON
    @SerializedName("key") val key: String? = null, // For MASTER (legacy)
    @SerializedName("masterDataKey") val masterDataKey: String? = null, // For MASTER_DATA
    @SerializedName("endpoint") val endpoint: String? = null, // For API (legacy)
    @SerializedName("apiEndpoint") val apiEndpoint: String? = null, // For API
    @SerializedName("method") val method: String? = null, // For API
    @SerializedName("dependsOn") val dependsOn: String? = null, // For API
    @SerializedName("paramKey") val paramKey: String? = null // For API
) {
    // Helper to get the actual key
    val actualKey: String?
        get() = masterDataKey ?: key
    
    // Helper to get the actual endpoint
    val actualEndpoint: String?
        get() = apiEndpoint ?: endpoint
    
    // Helper to get values from staticData
    val actualValues: List<String>?
        get() = staticData?.map { it.label } ?: values
}

data class StaticDataItemDto(
    @SerializedName("value") val value: String,
    @SerializedName("label") val label: String
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

data class DateConfigDto(
    @SerializedName("format") val format: String? = null,
    @SerializedName("validationType") val validationType: String? = null,
    @SerializedName("minAge") val minAge: Int? = null,
    @SerializedName("maxAge") val maxAge: Int? = null,
    @SerializedName("minDate") val minDate: String? = null,
    @SerializedName("maxDate") val maxDate: String? = null,
    @SerializedName("offset") val offset: Int? = null,
    @SerializedName("unit") val unit: String? = null
)

data class VerifiedInputConfigDto(
    @SerializedName("input") val input: VerifiedInputInputDto? = null,
    @SerializedName("verification") val verification: VerifiedInputVerificationDto? = null
)

data class VerifiedInputInputDto(
    @SerializedName("dataType") val dataType: String? = null,
    @SerializedName("keyboard") val keyboard: String? = null,
    @SerializedName("maxLength") val maxLength: String? = null,
    @SerializedName("min") val min: String? = null,
    @SerializedName("max") val max: String? = null
)

data class VerifiedInputVerificationDto(
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("messages") val messages: Map<String, String>? = null,
    @SerializedName("showDialog") val showDialog: Boolean? = null,
    @SerializedName("otp") val otp: VerifiedInputOtpDto? = null,
    @SerializedName("api") val api: VerifiedInputApiDto? = null,
    @SerializedName("successCondition") val successCondition: Map<String, Any>? = null
)

data class VerifiedInputOtpDto(
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("otpLength") val otpLength: String? = null,
    @SerializedName("resendIntervalSeconds") val resendIntervalSeconds: String? = null,
    @SerializedName("consent") val consent: VerifiedInputConsentDto? = null,
    @SerializedName("api") val api: Map<String, Any>? = null
)

data class VerifiedInputConsentDto(
    @SerializedName("title") val title: String? = null,
    @SerializedName("subTitle") val subTitle: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("positiveButtonText") val positiveButtonText: String? = null,
    @SerializedName("negativeButtonText") val negativeButtonText: String? = null
)

data class VerifiedInputApiDto(
    @SerializedName("endpoint") val endpoint: String? = null,
    @SerializedName("method") val method: String? = null,
    @SerializedName("successCondition") val successCondition: Map<String, Any>? = null
)

data class ApiVerificationConfigDto(
    @SerializedName("endpoint") val endpoint: String? = null,
    @SerializedName("method") val method: String? = null,
    @SerializedName("requestMapping") val requestMapping: String? = null,
    @SerializedName("successCondition") val successCondition: Map<String, Any>? = null,
    @SerializedName("messages") val messages: Map<String, String>? = null,
    @SerializedName("showDialog") val showDialog: Boolean? = null
)

data class FormActionDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null, // Legacy support
    @SerializedName("label") val label: String? = null,
    @SerializedName("api") val api: String,
    @SerializedName("method") val method: String,
    @SerializedName("nextScreen") val nextScreen: String? = null,
    @SerializedName("successMessage") val successMessage: String? = null,
    @SerializedName("failureMessage") val failureMessage: String? = null
)

data class ModalDto(
    @SerializedName("modalId") val modalId: String,
    @SerializedName("type") val type: String,
    @SerializedName("header") val header: ModalHeaderDto? = null,
    @SerializedName("otp") val otp: OtpConfigDto? = null,
    @SerializedName("consentText") val consentText: String? = null,
    @SerializedName("actions") val actions: List<ModalActionDto>? = emptyList()
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

data class ScopeDto(
    @SerializedName("type") val type: String? = null,
    @SerializedName("productCode") val productCode: String? = null,
    @SerializedName("partnerCode") val partnerCode: String? = null,
    @SerializedName("branchCode") val branchCode: String? = null
)

data class FormValidationsDto(
    @SerializedName("rules") val rules: List<FormValidationRuleDto>? = emptyList()
)

data class FormValidationRuleDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("fieldId") val fieldId: String? = null,
    @SerializedName("type") val type: String,
    @SerializedName("message") val message: String? = null,
    @SerializedName("pattern") val pattern: String? = null,
    @SerializedName("executionTarget") val executionTarget: String? = null
)
