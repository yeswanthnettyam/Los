package com.kaleidofin.originator.data.api

import com.google.gson.Gson
import com.kaleidofin.originator.data.dto.FormScreenDto
import kotlinx.coroutines.delay
import javax.inject.Inject

class FormApiServiceDummy @Inject constructor(
    private val gson: Gson
) : FormApiService {
    
    // Store form data per screen for testing
    private val formDataStore = mutableMapOf<String, Map<String, Any>>()
    
    fun updateFormData(screenId: String, formData: Map<String, Any>) {
        formDataStore[screenId] = formData
    }
    
    companion object {
        private const val APPLICANT_IDENTITY_JSON = """
{
  "screenId": "APPLICANT_IDENTITY",
  "flowId": "LOS_APPLICANT_ONBOARDING",
  "title": "Applicant - Identity Details",
  "layout": {
    "type": "FORM",
    "submitButtonText": "NEXT",
    "stickyFooter": true,
    "enableSubmitWhen": [
      {
        "type": "ALL_FIELDS_VALID"
      },
      {
        "type": "FIELD_EQUALS",
        "field": "phone_verified",
        "value": true
      },
      {
        "type": "FIELD_EQUALS",
        "field": "aadhaar_verified",
        "value": true
      },
      {
        "type": "FIELD_EQUALS",
        "field": "pan_verified",
        "value": true
      }
    ]
  },
  "hiddenFields": [
    {
      "id": "phone_verified",
      "type": "BOOLEAN",
      "defaultValue": false
    },
    {
      "id": "aadhaar_verified",
      "type": "BOOLEAN",
      "defaultValue": false
    },
    {
      "id": "pan_verified",
      "type": "BOOLEAN",
      "defaultValue": false
    }
  ],
  "sections": [
    {
      "sectionId": "APPLICANT_ROOT",
      "title": "Applicant Details",
      "collapsible": false,
      "expanded": true,
      "fields": [],
      "subSections": [
        {
          "sectionId": "IDENTITY_DETAILS",
          "title": "Identity Details",
          "subSectionOf": "APPLICANT_ROOT",
          "collapsible": false,
          "expanded": true,
          "fields": [
            {
              "id": "phone_number",
              "type": "TEXT",
              "label": "Phone Number",
              "keyboard": "NUMBER",
              "maxLength": 10,
              "required": true,
              "value": "",
              "verification": {
                "enabled": true,
                "type": "OTP",
                "trigger": "ON_COMPLETE",
                "modalId": "PHONE_OTP_MODAL",
                "statusField": "phone_verified",
                "showStatusIcon": true
              },
              "validation": {
                "regex": "^[6-9][0-9]{9}$",
                "errorMessage": "Enter valid mobile number"
              }
            },
            {
              "id": "aadhaar_number",
              "type": "TEXT",
              "label": "Aadhaar Number",
              "keyboard": "NUMBER",
              "maxLength": 12,
              "required": true,
              "verification": {
                "enabled": true,
                "type": "OTP",
                "trigger": "ON_COMPLETE",
                "modalId": "AADHAAR_OTP_MODAL",
                "statusField": "aadhaar_verified",
                "showStatusIcon": true
              },
              "validation": {
                "regex": "^[0-9]{12}$",
                "errorMessage": "Enter valid Aadhaar number"
              }
            }
          ]
        },
        {
          "sectionId": "PERSONAL_DETAILS",
          "title": "Personal details",
          "subSectionOf": "APPLICANT_ROOT",
          "collapsible": true,
          "expanded": true,
          "fields": [
            {
              "id": "salutation",
              "type": "DROPDOWN",
              "label": "Salutation",
              "required": true,
              "value": "",
              "dataSource": {
                "type": "INLINE",
                "values": [
                  "Mr",
                  "Mrs",
                  "Ms"
                ]
              }
            },
            {
              "id": "first_name",
              "type": "TEXT",
              "label": "First Name",
              "required": true,
              "value": ""
            },
            {
              "id": "last_name",
              "type": "TEXT",
              "label": "Last Name",
              "required": true
            },
            {
              "id": "middle_name",
              "type": "TEXT",
              "label": "Middle Name"
            },
            {
              "id": "dob",
              "type": "DATE",
              "label": "Birthdate / DOB",
              "required": true,
               "dateMode": "AGE",
              "constraints": {
                "minAge": 18,
                "maxAge": 70
              }
            },
            {
              "id": "gender",
              "type": "DROPDOWN",
              "label": "Gender",
              "required": true,
              "dataSource": {
                "type": "MASTER",
                "key": "GENDER"
              }
            },
            {
              "id": "pan_number",
              "type": "TEXT",
              "label": "PAN",
              "maxLength": 10,
              "required": true,
              "verification": {
                "enabled": true,
                "type": "API",
                "trigger": "ON_COMPLETE",
                "modalId": "PAN_VERIFY_MODAL",
                "statusField": "pan_verified",
                "showStatusIcon": true
              },
              "validation": {
                "regex": "^[A-Z]{5}[0-9]{4}[A-Z]{1}$",
                "errorMessage": "Enter valid PAN number"
              }
            },
            {
              "id": "secondary_id_type",
              "type": "DROPDOWN",
              "label": "Select Secondary Id Proof",
              "dataSource": {
                "type": "INLINE",
                "values": [
                  "Voter Id",
                  "Driving License",
                  "Passport",
                  "None"
                ]
              }
            },
            {
              "id": "secondary_id_number",
              "type": "TEXT",
              "label": "Secondary Id Number",
              "required": true,
              "enabledWhen": [
                {
                  "field": "secondary_id_type",
                  "operator": "NOT_EQUALS",
                  "value": "None"
                }
              ]
            },
            {
              "id": "email",
              "type": "TEXT",
              "label": "Email",
              "keyboard": "EMAIL",
              "required": true,
              "validation": {
                "regex": "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$",
                "errorMessage": "Enter valid email address"
              }
            },
            {
              "id": "education_qualification",
              "type": "DROPDOWN",
              "label": "Education Qualification",
              "required": true,
              "dataSource": {
                "type": "MASTER",
                "key": "EDUCATION_QUALIFICATION"
              }
            },
            {
              "id": "marital_status",
              "type": "DROPDOWN",
              "label": "Marital Status",
              "required": true,
              "dataSource": {
                "type": "MASTER",
                "key": "MARITAL_STATUS"
              }
            },
            {
              "id": "religion",
              "type": "DROPDOWN",
              "label": "Religion",
              "required": true,
              "dataSource": {
                "type": "MASTER",
                "key": "RELIGION"
              }
            },
            {
              "id": "caste",
              "type": "DROPDOWN",
              "label": "Caste",
              "required": true,
              "dataSource": {
                "type": "MASTER",
                "key": "CASTE"
              }
            },
            {
              "id": "reference_name",
              "type": "TEXT",
              "label": "Reference Name",
              "required": true
            },
            {
              "id": "vintage_with_partner",
              "type": "NUMBER",
              "label": "Vintage With Partner (In Years)",
              "required": true,
              "min": 0,
              "max": 2
            }
          ]
        },
        {
          "sectionId": "ADDRESS_DETAILS",
          "title": "Address details",
          "subSectionOf": "APPLICANT_ROOT",
          "collapsible": true,
          "expanded": false,
          "repeatable": true,
          "minInstances": 1,
          "maxInstances": 2,
          "instanceLabel": "Address {{index}}",
          "fields": [
            {
              "id": "residence_type",
              "type": "DROPDOWN",
              "label": "Residence Type",
              "required": true,
              "dataSource": {
                "type": "MASTER",
                "key": "RESIDENCE_TYPE"
              }
            },
            {
              "id": "address_line_1",
              "type": "TEXTAREA",
              "label": "Current Address Line 1",
              "required": true
            },
            {
              "id": "address_line_2",
              "type": "TEXTAREA",
              "label": "Current Address Line 2"
            },
            {
              "id": "post_office",
              "type": "TEXT",
              "label": "Postoffice",
              "required": true
            },
            {
              "id": "city",
              "type": "TEXT",
              "label": "City",
              "required": true
            },
            {
              "id": "state",
              "type": "DROPDOWN",
              "label": "State",
              "required": true,
              "dataSource": {
                "type": "MASTER",
                "key": "STATE"
              }
            },
            {
              "id": "pincode",
              "type": "TEXT",
              "keyboard": "NUMBER",
              "label": "Pincode",
              "required": true,
              "maxLength": 6,
              "validation": {
                "regex": "^[1-9][0-9]{5}$",
                "errorMessage": "Enter valid pincode"
              }
            }
          ]
        }
      ]
    }
  ],
  "modals": [
    {
      "modalId": "PHONE_OTP_MODAL",
      "type": "OTP_CONSENT",
      "otp": {
        "length": 6
      },
      "actions": [
        {
          "type": "VERIFY",
          "label": "AGREE & PROCEED",
          "api": "/api/v1/verify/phone",
          "onSuccess": {
            "updateField": "phone_verified",
            "value": true,
            "closeModal": true
          }
        }
      ]
    },
    {
      "modalId": "AADHAAR_OTP_MODAL",
      "type": "OTP_CONSENT",
      "otp": {
        "length": 6
      },
      "actions": [
        {
          "type": "VERIFY",
          "label": "VERIFY & PROCEED",
          "api": "/api/v1/verify/aadhaar",
          "onSuccess": {
            "updateField": "aadhaar_verified",
            "value": true,
            "closeModal": true
          }
        }
      ]
    },
    {
      "modalId": "PAN_VERIFY_MODAL",
      "type": "API",
      "actions": [
        {
          "type": "VERIFY",
          "label": "VERIFY",
          "api": "/api/v1/verify/pan",
          "onSuccess": {
            "updateField": "pan_verified",
            "value": true,
            "closeModal": true
          }
        }
      ]
    }
  ],
  "actions": [
    {
      "type": "SUBMIT",
      "api": "/api/v1/applicant/identity",
      "method": "POST",
      "nextScreen": "APPLICANT_BUSINESS_DETAILS"
    }
  ]
}
"""
        
        private const val APPLICANT_BUSINESS_DETAILS_JSON = """{
  "screenId": "APPLICANT_BUSINESS_DETAILS",
  "flowId": "LOS_APPLICANT_ONBOARDING",
  "title": "Applicant - Business Details",
  "layout": {
    "type": "FORM",
    "submitButtonText": "NEXT",
    "stickyFooter": true,
    "enableSubmitWhen": [
      {
        "type": "ALL_FIELDS_VALID"
      },
      {
        "type": "FIELD_EQUALS",
        "field": "business_phone_verified",
        "value": true
      },
      {
        "type": "FIELD_EQUALS",
        "field": "business_pan_verified",
        "value": true
      }
    ]
  },
  "hiddenFields": [
    {
      "id": "business_phone_verified",
      "type": "BOOLEAN",
      "defaultValue": false
    },
    {
      "id": "business_pan_verified",
      "type": "BOOLEAN",
      "defaultValue": false
    }
  ],
  "sections": [
    {
      "sectionId": "BUSINESS_ROOT",
      "title": "Applicant business details",
      "collapsible": false,
      "expanded": true,
      "fields": [],
      "subSections": [
        {
          "sectionId": "BUSINESS_DETAILS",
          "title": "Applicant business details",
          "subSectionOf": "BUSINESS_ROOT",
          "collapsible": true,
          "expanded": true,
          "fields": [
            {
              "id": "registered_business_name",
              "type": "TEXT",
              "label": "Registered Business Name",
              "placeholder": "Enter Name",
              "required": true
            },
            {
              "id": "business_premise_ownership_type",
              "type": "DROPDOWN",
              "label": "Business Premise Ownership Type",
              "required": true,
              "dataSource": {
                "type": "INLINE",
                "values": [
                  "Owned",
                  "Rented"
                ]
              }
            },
            {
              "id": "sub_industry",
              "type": "DROPDOWN",
              "label": "Sub-Industry",
              "required": true,
              "dataSource": {
                "type": "INLINE",
                "values": [
                  "Data"
                ]
              }
            },
            {
              "id": "business_email",
              "type": "TEXT",
              "label": "Business Email",
              "placeholder": "Enter Business Email Id",
              "keyboard": "EMAIL"
            },
            {
              "id": "business_phone",
              "type": "TEXT",
              "label": "Business Phone",
              "placeholder": "Enter Business Phone Number",
              "keyboard": "NUMBER",
              "maxLength": 10,
              "required": true,
              "verification": {
                "enabled": true,
                "type": "OTP",
                "trigger": "ON_COMPLETE",
                "modalId": "BUSINESS_PHONE_OTP_MODAL",
                "statusField": "business_phone_verified",
                "showStatusIcon": true
              },
              "validation": {
                "regex": "^[6-9][0-9]{9}$",
                "errorMessage": "Enter valid business phone number"
              }
            },
            {
              "id": "business_pan",
              "type": "TEXT",
              "label": "Business PAN",
              "placeholder": "Enter Business PAN Number",
              "maxLength": 10,
              "required": true,
              "verification": {
                "enabled": true,
                "type": "API",
                "trigger": "ON_COMPLETE",
                "modalId": "BUSINESS_PAN_VERIFY_MODAL",
                "statusField": "business_pan_verified",
                "showStatusIcon": true
              },
              "validation": {
                "regex": "^[A-Z]{5}[0-9]{4}[A-Z]{1}$",
                "errorMessage": "Enter valid Business PAN"
              }
            },
            {
              "id": "business_kyc_document",
              "type": "DROPDOWN",
              "label": "Business KYC Document",
              "required": true,
              "dataSource": {
                "type": "INLINE",
                "values": [
                  "UDHYAM"
                ]
              }
            },
            {
              "id": "other_business_kyc_document",
              "type": "DROPDOWN",
              "label": "Other Business KYC Document",
              "dataSource": {
                "type": "MASTER",
                "key": "STATE"
              }
            },
            {
              "id": "date_of_incorporation",
              "type": "DATE",
               "dateMode": "ANY",
              "label": "Date Of Incorporation",
              "required": true
            },
            {
              "id": "office_stability_months",
              "type": "NUMBER",
              "label": "Office Stability In Months",
              "placeholder": "Enter Months",
              "required": true,
              "min": 0,
              "max": 2
            }
          ]
        },
        {
          "sectionId": "BUSINESS_ADDRESS_DETAILS",
          "title": "Address details",
          "subSectionOf": "BUSINESS_ROOT",
          "collapsible": true,
          "expanded": false,
          "fields": [
            {
              "id": "business_address_line_1",
              "type": "TEXTAREA",
              "label": "Business Address Line 1",
              "placeholder": "Enter Address",
              "required": true
            },
            {
              "id": "business_address_line_2",
              "type": "TEXTAREA",
              "label": "Business Address Line 2",
              "placeholder": "Enter Address"
            },
            {
              "id": "business_post_office",
              "type": "TEXT",
              "label": "Post Office",
              "placeholder": "Enter Pincode",
              "required": true
            },
            {
              "id": "business_city",
              "type": "TEXT",
              "label": "City",
              "placeholder": "Enter City",
              "required": true
            },
            {
              "id": "business_state",
              "type": "DROPDOWN",
              "label": "State",
              "required": true,
              "dataSource": {
                "type": "MASTER",
                "key": "STATE"
              }
            },
            {
              "id": "business_pincode",
               "type": "TEXT",
              "keyboard": "NUMBER",              
              "label": "Pincode",
              "placeholder": "Enter Pincode",
              "required": true,
              "maxLength": 6,
              "validation": {
                "regex": "^[1-9][0-9]{5}$",
                "errorMessage": "Enter valid pincode"
              }
            }
          ]
        },
        {
          "sectionId": "LOAN_DETAILS",
          "title": "Loan details",
          "subSectionOf": "BUSINESS_ROOT",
          "collapsible": true,
          "expanded": false,
          "fields": [
            {
              "id": "requested_loan_amount",
              "type": "NUMBER",
              "label": "Requested Loan Amount",
              "placeholder": "Enter Loan Amount",
              "required": true,
              "min": 1000,
              "max": 10000000
            },
            {
              "id": "requested_loan_tenure_months",
              "type": "DROPDOWN",
              "label": "Requested Loan Tenure In Months",
              "required": true,
              "dataSource": {
                "type": "MASTER",
                "key": "RESIDENCE_TYPE"
              }
            },
            {
              "id": "loan_purpose",
              "type": "DROPDOWN",
              "label": "Purpose Of The Loan",
              "required": true,
              "dataSource": {
                "type": "MASTER",
                "key": "RESIDENCE_TYPE"
              }
            }
          ]
        }
      ]
    }
  ],
  "modals": [
    {
      "modalId": "BUSINESS_PHONE_OTP_MODAL",
      "type": "OTP_CONSENT",
      "otp": {
        "length": 6
      },
      "actions": [
        {
          "type": "VERIFY",
          "label": "VERIFY & PROCEED",
          "api": "/api/v1/verify/business-phone",
          "onSuccess": {
            "updateField": "business_phone_verified",
            "value": true,
            "closeModal": true
          }
        }
      ]
    },
    {
      "modalId": "BUSINESS_PAN_VERIFY_MODAL",
      "type": "API",
      "actions": [
        {
          "type": "VERIFY",
          "label": "VERIFY",
          "api": "/api/v1/verify/business-pan",
          "onSuccess": {
            "updateField": "business_pan_verified",
            "value": true,
            "closeModal": true
          }
        }
      ]
    }
  ],
  "actions": [
    {
      "type": "SUBMIT",
      "api": "/api/v1/applicant/business",
      "method": "POST",
      "nextScreen": "APPLICANT_LOAN_SUMMARY"
    }
  ]
}
"""
    }
    
    override suspend fun getFormConfiguration(target: String): FormScreenDto {
        delay(500) // Simulate network delay
        
        var jsonString = when (target) {
            "APPLICANT_IDENTITY" -> APPLICANT_IDENTITY_JSON
            "APPLICANT_BUSINESS_DETAILS" -> APPLICANT_BUSINESS_DETAILS_JSON
            else -> APPLICANT_IDENTITY_JSON // Default form
        }
        
        // Update JSON with stored form data if available
        val storedData = formDataStore[target]
        if (storedData != null) {
            jsonString = updateJsonWithFormData(jsonString, storedData)
        }
        
        return gson.fromJson(jsonString, FormScreenDto::class.java)
    }
    
    private fun updateJsonWithFormData(jsonString: String, formData: Map<String, Any>): String {
        // Parse JSON to a mutable structure
        val jsonElement = gson.fromJson(jsonString, com.google.gson.JsonObject::class.java)
        
        // Helper function to update a single field value
        fun updateFieldValue(field: com.google.gson.JsonObject, fieldId: String) {
            // Check for direct match first
            val directValue = formData[fieldId]
            if (directValue != null) {
                when (directValue) {
                    is String -> field.addProperty("value", directValue)
                    is Number -> field.addProperty("value", directValue)
                    is Boolean -> field.addProperty("value", directValue)
                    else -> field.addProperty("value", directValue.toString())
                }
                return
            }
            
            // Check for indexed values (for repeatable sections) - take the first instance
            val indexedValue = formData.entries.firstOrNull { 
                it.key.startsWith("${fieldId}_") 
            }?.value
            
            if (indexedValue != null) {
                when (indexedValue) {
                    is String -> field.addProperty("value", indexedValue)
                    is Number -> field.addProperty("value", indexedValue)
                    is Boolean -> field.addProperty("value", indexedValue)
                    else -> field.addProperty("value", indexedValue.toString())
                }
            }
        }
        
        // Helper function to recursively update field values in sections
        fun updateFieldsInSection(section: com.google.gson.JsonObject) {
            // Update fields in this section
            val fieldsArray = section.getAsJsonArray("fields")
            fieldsArray?.forEach { fieldElement ->
                val field = fieldElement.asJsonObject
                val fieldId = field.get("id")?.asString
                if (fieldId != null) {
                    updateFieldValue(field, fieldId)
                }
            }
            
            // Recursively update subsections
            val subSectionsArray = section.getAsJsonArray("subSections")
            subSectionsArray?.forEach { subSectionElement ->
                updateFieldsInSection(subSectionElement.asJsonObject)
            }
        }
        
        // Update all sections
        val sectionsArray = jsonElement.getAsJsonArray("sections")
        sectionsArray?.forEach { sectionElement ->
            val section = sectionElement.asJsonObject
            
            // Update fields in root section
            val fieldsArray = section.getAsJsonArray("fields")
            fieldsArray?.forEach { fieldElement ->
                val field = fieldElement.asJsonObject
                val fieldId = field.get("id")?.asString
                if (fieldId != null) {
                    updateFieldValue(field, fieldId)
                }
            }
            
            // Update subsections
            val subSectionsArray = section.getAsJsonArray("subSections")
            subSectionsArray?.forEach { subSectionElement ->
                updateFieldsInSection(subSectionElement.asJsonObject)
            }
        }
        
        return gson.toJson(jsonElement)
    }

    override suspend fun getMasterData(dataSource: String): Map<String, String> {
        delay(200) // Simulate network delay

        val masterDataMap = mapOf(
            "GENDER" to "Male,Female,Other",
            "EDUCATION_QUALIFICATION" to "Below 10th,10th,12th,Graduate,Post Graduate,Professional",
            "MARITAL_STATUS" to "Single,Married,Divorced,Widowed",
            "RELIGION" to "Hindu,Muslim,Christian,Sikh,Buddhist,Jain,Other",
            "CASTE" to "General,OBC,SC,ST,Other",
            "RESIDENCE_TYPE" to "Owned,Rented,Parental,Other",
            "STATE" to "Andhra Pradesh,Arunachal Pradesh,Assam,Bihar,Chhattisgarh,Goa,Gujarat,Haryana,Himachal Pradesh,Jharkhand,Karnataka,Kerala,Madhya Pradesh,Maharashtra,Manipur,Meghalaya,Mizoram,Nagaland,Odisha,Punjab,Rajasthan,Sikkim,Tamil Nadu,Telangana,Tripura,Uttar Pradesh,Uttarakhand,West Bengal"
        )

        return mapOf(dataSource to (masterDataMap[dataSource] ?: ""))
    }
}
