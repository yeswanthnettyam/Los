package com.kaleidofin.originator.data.api

import com.google.gson.Gson
import com.kaleidofin.originator.data.dto.*
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
        private const val APPLICANT_IDENTITY_JSON = """{
  "screenId": "applicant_identity_details",
  "title": "Applicant Identity Details",
  "version": 1,
  "status": "DRAFT",
  "scope": {
    "type": "PRODUCT",
    "productCode": "PL",
    "partnerCode": "",
    "branchCode": ""
  },
  "ui": {
    "layout": "FORM",
    "sections": [
      {
        "id": "section_1",
        "title": "Applicant Identity Details",
        "collapsible": false,
        "defaultExpanded": true,
        "subSections": [
          {
            "id": "subsection_1767690894344",
            "title": "Identity Details",
            "repeatable": false,
            "minInstances": 1,
            "maxInstances": 5,
            "instanceLabel": "Instance",
            "order": 0,
            "parentSectionId": "section_1",
            "fields": [
              {
                "_key": "3f3dbd25-d677-4dfe-a04e-a3fbb5d0c181",
                "id": "aadhaar_number",
                "type": "VERIFIED_INPUT",
                "label": "Aadhaar Number",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter Aadhaar Number",
                "order": 0,
                "parentId": "subsection_1767690894344",
                "parentType": "SUBSECTION",
                "keyboard": "numeric",
                "maxLength": "12",
                "visibleWhen": {},
                "min": "12",
                "max": "12",
                "enabledWhen": {},
                "requiredWhen": {},
                "verifiedInputConfig": {
                  "input": {
                    "dataType": "NUMBER",
                    "keyboard": "NUMBER",
                    "min": "12",
                    "max": "12"
                  },
                  "verification": {
                    "mode": "OTP",
                    "messages": {
                      "success": "",
                      "failure": ""
                    },
                    "showDialog": false,
                    "otp": {
                      "channel": "MOBILE",
                      "otpLength": "6",
                      "resendIntervalSeconds": "60",
                      "consent": {
                        "title": "Aadhaar Consent",
                        "subTitle": "Please Enter the 6 digit OTP send to the aadhaar linked phone number",
                        "message": "I consent to receive consent via SMS and/or email from kaleidofin for authentication and aadhaar verification purposes. I acknowledge that I have read and agree to the Privacy Policy and Terms & Conditions",
                        "positiveButtonText": "Agree & Proceed"
                      },
                      "api": {
                        "sendOtp": {
                          "endpoint": "/api/otp/send",
                          "method": "POST"
                        },
                        "verifyOtp": {
                          "endpoint": "/api/otp/verify",
                          "method": "GET"
                        }
                      }
                    }
                  }
                }
              },
              {
                "_key": "91e584c0-268c-4f6a-9de0-ef59174a7c39",
                "id": "aadhaar_linked_phone_number",
                "type": "VERIFIED_INPUT",
                "label": "Aadhaar Linked Phone Number",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter Mobile Number",
                "order": 1,
                "parentId": "subsection_1767690894344",
                "parentType": "SUBSECTION",
                "keyboard": "numeric",
                "visibleWhen": {},
                "min": "10",
                "max": "10",
                "enabledWhen": {},
                "requiredWhen": {},
                "verifiedInputConfig": {
                  "input": {
                    "dataType": "NUMBER",
                    "keyboard": "NUMBER",
                    "min": "10",
                    "max": "10"
                  },
                  "verification": {
                    "mode": "OTP",
                    "messages": {
                      "success": "",
                      "failure": "Bad Client Details"
                    },
                    "showDialog": true,
                    "otp": {
                      "channel": "MOBILE",
                      "otpLength": "6",
                      "resendIntervalSeconds": "60",
                      "consent": {
                        "title": "User Agreement",
                        "subTitle": "Please enter the 6 digit consent code sent to phone number ",
                        "message": "I consent to receive code via SMS and/or email from kaleidofin for authentication and verification purposes. I also authorize kaleidofin to fetch my Know Your Customer (KYC) details from relevant databases for identity verification and compliance purposes, and to pull my credit report from authorized credit bureaus to evaluate my creditworthiness and eligibility for services. I acknowledge that I have read and agree to the Privacy Policy and Terms & Conditions",
                        "positiveButtonText": "Agree & Proceed"
                      },
                      "api": {
                        "sendOtp": {
                          "endpoint": "/api/otp/send",
                          "method": "POST"
                        },
                        "verifyOtp": {
                          "endpoint": "/api/otp/verify",
                          "method": "GET"
                        }
                      }
                    }
                  }
                }
              }
            ]
          },
          {
            "id": "subsection_1767691200583",
            "title": "Personal Details",
            "repeatable": false,
            "minInstances": 1,
            "maxInstances": 5,
            "collapsible": true,
            "instanceLabel": "Instance",
            "order": 1,
            "parentSectionId": "section_1",
            "fields": [
              {
                "_key": "4081d20b-d985-4059-a15b-f9c271199d31",
                "id": "salutation",
                "type": "DROPDOWN",
                "label": "Salutation",
                "required": true,
                "readOnly": false,
                "placeholder": "",
                "order": 0,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "visibleWhen": {},
                "dataSource": {
                  "type": "STATIC_JSON",
                  "staticData": [
                    {
                      "value": "miss",
                      "label": "MISS"
                    },
                    {
                      "value": "mr",
                      "label": "MR"
                    },
                    {
                      "value": "mrs",
                      "label": "MRS"
                    }
                  ],
                  "masterDataKey": "MISS, MR, MRS"
                },
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "094256fe-5b78-410a-b3cd-05da98a7749c",
                "id": "first_name",
                "type": "TEXT",
                "label": "First Name",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter First Name",
                "order": 1,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "keyboard": "default",
                "maxLength": "50",
                "visibleWhen": {},
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "bcd66eb4-bb2d-4ae1-93ab-5e23e9ed37eb",
                "id": "last_name",
                "type": "TEXT",
                "label": "Last Name",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter Last Name",
                "order": 2,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "keyboard": "default",
                "maxLength": "49",
                "visibleWhen": {},
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "1d99cb28-9942-45af-9a2e-effc92e4407b",
                "id": "middle_name",
                "type": "TEXT",
                "label": "Middle Name",
                "required": false,
                "readOnly": false,
                "placeholder": "Enter Middle Name",
                "order": 3,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "keyboard": "default",
                "maxLength": "50",
                "visibleWhen": {},
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "edcc0c8e-056a-4bac-8aaa-6d181000fe80",
                "id": "birthdate",
                "type": "DATE",
                "label": "Birthdate/DOB",
                "required": true,
                "readOnly": false,
                "placeholder": "",
                "order": 4,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "visibleWhen": {},
                "enabledWhen": {},
                "requiredWhen": {},
                "dateConfig": {
                  "format": "DD/MM/YYYY",
                  "validationType": "AGE_RANGE"
                }
              },
              {
                "_key": "06c24000-13fe-44a5-903d-069610cd8066",
                "id": "gender",
                "type": "DROPDOWN",
                "label": "Gender",
                "required": true,
                "readOnly": false,
                "placeholder": "",
                "order": 5,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "visibleWhen": {},
                "dataSource": {
                  "type": "STATIC_JSON",
                  "staticData": [
                    {
                      "value": "f",
                      "label": "Female"
                    },
                    {
                      "value": "m",
                      "label": "Male"
                    }
                  ]
                },
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "acd5cc55-7900-43d8-885d-a69f9e77ec9f",
                "id": "pan",
                "type": "API_VERIFICATION",
                "label": "PAN",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter PAN Number",
                "order": 6,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "keyboard": "numeric",
                "maxLength": "10",
                "visibleWhen": {},
                "min": "10",
                "max": "10",
                "enabledWhen": {},
                "requiredWhen": {},
                "apiVerificationConfig": {
                  "endpoint": "/api/pan/verification",
                  "method": "GET",
                  "requestMapping": "{\"pan\":\"{{panValue}}\"",
                  "successCondition": {
                    "field": "status",
                    "equals": "VERIFIED"
                  },
                  "messages": {
                    "success": "Applicant PAN Verified",
                    "failure": "Applicant PAN Verification failed"
                  },
                  "showDialog": true
                }
              },
              {
                "_key": "472e7c10-e571-45c8-8d03-6f2bf48cd525",
                "id": "identity_proof",
                "type": "DROPDOWN",
                "label": "Select Secondary Id Proof",
                "required": false,
                "readOnly": false,
                "placeholder": "",
                "order": 7,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "visibleWhen": {},
                "dataSource": {
                  "type": "STATIC_JSON",
                  "staticData": [
                    {
                      "value": "none",
                      "label": "Select(None)"
                    },
                    {
                      "value": "voterId",
                      "label": "Voter id"
                    },
                    {
                      "value": "drivingLicense",
                      "label": "Driving license"
                    }
                  ]
                },
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "f46cfc68-8519-4b07-a5e2-f4a3882013d2",
                "id": "email",
                "type": "TEXT",
                "label": "Email",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter Email Id",
                "order": 8,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "keyboard": "email",
                "maxLength": "254",
                "visibleWhen": {},
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "738412ac-dc04-4df8-8ba4-d0a8c8c52b69",
                "id": "education_qualification",
                "type": "DROPDOWN",
                "label": "Education Qualification",
                "required": true,
                "readOnly": false,
                "placeholder": "",
                "order": 9,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "visibleWhen": {},
                "dataSource": {
                  "type": "STATIC_JSON",
                  "staticData": [
                    {
                      "value": "10th",
                      "label": "10th"
                    },
                    {
                      "value": "12th",
                      "label": "12th"
                    },
                    {
                      "value": "belowMatric",
                      "label": "Below Matric"
                    },
                    {
                      "value": "graduate",
                      "label": "Graduate"
                    }
                  ]
                },
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "62c119c2-a0ec-417a-bd60-f63e1f702335",
                "id": "marital_status",
                "type": "DROPDOWN",
                "label": "Marital Status",
                "required": true,
                "readOnly": false,
                "placeholder": "",
                "order": 10,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "visibleWhen": {},
                "dataSource": {
                  "type": "STATIC_JSON",
                  "staticData": [
                    {
                      "value": "d",
                      "label": "Divorced"
                    },
                    {
                      "value": "m",
                      "label": "Married"
                    },
                    {
                      "value": "s",
                      "label": "Single"
                    },
                    {
                      "value": "w",
                      "label": "Widow"
                    }
                  ]
                },
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "791631b0-2c95-4d46-a80e-0193e3d7d356",
                "id": "religion",
                "type": "DROPDOWN",
                "label": "Religion",
                "required": true,
                "readOnly": false,
                "placeholder": "",
                "order": 11,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "visibleWhen": {},
                "dataSource": {
                  "type": "STATIC_JSON",
                  "staticData": [
                    {
                      "value": "c",
                      "label": "Christian"
                    },
                    {
                      "value": "h",
                      "label": "Hindu"
                    },
                    {
                      "value": "m",
                      "label": "Muslim"
                    },
                    {
                      "value": "o",
                      "label": "Others"
                    }
                  ]
                },
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "f8ae446c-26da-4cea-b780-fda72618f792",
                "id": "caste",
                "type": "DROPDOWN",
                "label": "Caste",
                "required": true,
                "readOnly": false,
                "placeholder": "",
                "order": 12,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "visibleWhen": {},
                "dataSource": {
                  "type": "STATIC_JSON",
                  "staticData": [
                    {
                      "value": "g",
                      "label": "General Category (or Forward Castes)"
                    },
                    {
                      "value": "o",
                      "label": "Other Backward Classes (OBC)"
                    },
                    {
                      "value": "sc",
                      "label": "Scheduled Castes (SC)"
                    },
                    {
                      "value": "st",
                      "label": "Scheduled Tribes (ST)"
                    }
                  ]
                },
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "ff4b3ab9-694a-4ddd-bf18-5498f70e9580",
                "id": "reference_name",
                "type": "TEXT",
                "label": "Reference Name",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter Reference Name",
                "order": 13,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "keyboard": "default",
                "maxLength": "50",
                "visibleWhen": {},
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "29b130eb-adc0-4a09-b385-ed961cc68a7f",
                "id": "no_of_dependents",
                "type": "NUMBER",
                "label": "No of Dependents",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter No of Dependents",
                "order": 14,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "keyboard": "numeric",
                "maxLength": "5",
                "visibleWhen": {},
                "min": "1",
                "max": "5",
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "fa50f2da-b84d-4a7b-9af7-317835a077ec",
                "id": "vintage_with_partner",
                "type": "NUMBER",
                "label": "Vintage with Partner (in Years)",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter Vintage With Partner",
                "order": 15,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "keyboard": "numeric",
                "visibleWhen": {},
                "min": "1",
                "max": "2",
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "ef123525-e4c0-4a43-88cf-33a67b05c421",
                "id": "pep_status",
                "type": "DROPDOWN",
                "label": "Is Applicant a Politically Exposed Person?",
                "required": true,
                "readOnly": false,
                "placeholder": "",
                "order": 16,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "visibleWhen": {},
                "dataSource": {
                  "type": "STATIC_JSON",
                  "staticData": [
                    {
                      "value": "n",
                      "label": "No"
                    },
                    {
                      "value": "y",
                      "label": "Yes"
                    }
                  ]
                },
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "a0ffaa13-737e-47f1-9e4d-d91d3582e39b",
                "id": "occupation",
                "type": "TEXTAREA",
                "label": "Occupation",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter Occupation",
                "order": 17,
                "parentId": "subsection_1767691200583",
                "parentType": "SUBSECTION",
                "keyboard": "default",
                "visibleWhen": {},
                "enabledWhen": {},
                "requiredWhen": {}
              }
            ]
          },
          {
            "id": "subsection_1767693176103",
            "title": "Address Details",
            "repeatable": true,
            "minInstances": 1,
            "maxInstances": 5,
            "instanceLabel": "Instance",
            "order": 2,
            "parentSectionId": "section_1",
            "fields": [
              {
                "_key": "cd4135fc-3a6a-46c0-aa03-a6bd455a9e9a",
                "id": "residence_type",
                "type": "DROPDOWN",
                "label": "Residence Type",
                "required": true,
                "readOnly": false,
                "placeholder": "",
                "order": 0,
                "parentId": "subsection_1767693176103",
                "parentType": "SUBSECTION",
                "visibleWhen": {},
                "dataSource": {
                  "type": "STATIC_JSON",
                  "staticData": [
                    {
                      "value": "o",
                      "label": "Owned"
                    },
                    {
                      "value": "r",
                      "label": "Rented"
                    }
                  ]
                },
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "1f08da0c-1be7-4aa5-87d6-bddb49614d89",
                "id": "current_address_line_1",
                "type": "TEXTAREA",
                "label": "Current Address Line 1",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter Current Address Line1",
                "order": 1,
                "parentId": "subsection_1767693176103",
                "parentType": "SUBSECTION",
                "keyboard": "default",
                "visibleWhen": {},
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "e7790a2b-fe40-4aab-9472-f90834f0ceec",
                "id": "current_address_line_2",
                "type": "TEXTAREA",
                "label": "Current Address Line 2",
                "required": false,
                "readOnly": false,
                "placeholder": "Enter Current Address Line2",
                "order": 2,
                "parentId": "subsection_1767693176103",
                "parentType": "SUBSECTION",
                "keyboard": "default",
                "visibleWhen": {},
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "e72bbcf3-40b8-4c3f-b450-a96ee7a33bbe",
                "id": "post_office",
                "type": "TEXT",
                "label": "Post Office",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter Postoffice",
                "order": 3,
                "parentId": "subsection_1767693176103",
                "parentType": "SUBSECTION",
                "keyboard": "default",
                "maxLength": "50",
                "visibleWhen": {},
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "8f7e12ed-0a35-46fa-b705-b246a8ec2565",
                "id": "city",
                "type": "TEXT",
                "label": "City",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter City",
                "order": 4,
                "parentId": "subsection_1767693176103",
                "parentType": "SUBSECTION",
                "keyboard": "default",
                "maxLength": "50",
                "visibleWhen": {},
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "a1932eee-f440-4a43-a4ac-b09dc0fa2bd4",
                "id": "state",
                "type": "DROPDOWN",
                "label": "State",
                "required": true,
                "readOnly": false,
                "placeholder": "En",
                "order": 5,
                "parentId": "subsection_1767693176103",
                "parentType": "SUBSECTION",
                "visibleWhen": {},
                "dataSource": {
                  "type": "STATIC_JSON",
                  "staticData": [
                    {
                      "value": "an",
                      "label": "Andaman and nicobar islands"
                    },
                    {
                      "value": "ap",
                      "label": "Andhra pradesh"
                    },
                    {
                      "value": "ar",
                      "label": "Arunachal pradesh"
                    },
                    {
                      "value": "as",
                      "label": "Assam"
                    },
                    {
                      "value": "br",
                      "label": "Bihar"
                    },
                    {
                      "value": "ch",
                      "label": "Chandigarh"
                    },
                    {
                      "value": "ct",
                      "label": "Chhattisgarh"
                    },
                    {
                      "value": "dd",
                      "label": "Dadra & nagar haveli and daman and diu"
                    },
                    {
                      "value": "dl",
                      "label": "Delhi (national capital territory)"
                    },
                    {
                      "value": "ga",
                      "label": "Goa"
                    },
                    {
                      "value": "gj",
                      "label": "Gujarat"
                    },
                    {
                      "value": "hr",
                      "label": "Haryana"
                    },
                    {
                      "value": "hp",
                      "label": "Himachal pradesh"
                    },
                    {
                      "value": "jk",
                      "label": "Jammu and kashmir"
                    },
                    {
                      "value": "jh",
                      "label": "Jharkhand"
                    },
                    {
                      "value": "ka",
                      "label": "Karnataka"
                    },
                    {
                      "value": "kl",
                      "label": "Kerala"
                    },
                    {
                      "value": "la",
                      "label": "Ladakh"
                    },
                    {
                      "value": "ld",
                      "label": "Lakshadweep"
                    },
                    {
                      "value": "mp",
                      "label": "Madhya pradesh"
                    },
                    {
                      "value": "mh",
                      "label": "Maharashtra"
                    },
                    {
                      "value": "mn",
                      "label": "Manipur"
                    },
                    {
                      "value": "ml",
                      "label": "Meghalaya"
                    },
                    {
                      "value": "mz",
                      "label": "Mizoram"
                    },
                    {
                      "value": "nl",
                      "label": "Nagaland"
                    },
                    {
                      "value": "od",
                      "label": "Odisha"
                    },
                    {
                      "value": "py",
                      "label": "Puducherry"
                    },
                    {
                      "value": "pb",
                      "label": "Punjab"
                    },
                    {
                      "value": "rj",
                      "label": "Rajasthan"
                    },
                    {
                      "value": "sk",
                      "label": "Sikkim"
                    },
                    {
                      "value": "tn",
                      "label": "Tamil nadu"
                    },
                    {
                      "value": "tg",
                      "label": "Telangana"
                    },
                    {
                      "value": "tr",
                      "label": "Tripura"
                    },
                    {
                      "value": "up",
                      "label": "Uttar pradesh"
                    },
                    {
                      "value": "ut",
                      "label": "Uttarakhand"
                    },
                    {
                      "value": "wb",
                      "label": "West bengal"
                    }
                  ]
                },
                "enabledWhen": {},
                "requiredWhen": {}
              },
              {
                "_key": "5ca566ae-38f2-4749-8a61-1614facf42da",
                "id": "pincode",
                "type": "NUMBER",
                "label": "Pincode",
                "required": true,
                "readOnly": false,
                "placeholder": "Enter Pincode",
                "order": 6,
                "parentId": "subsection_1767693176103",
                "parentType": "SUBSECTION",
                "keyboard": "numeric",
                "visibleWhen": {},
                "min": "6",
                "max": "6",
                "enabledWhen": {},
                "requiredWhen": {}
              }
            ]
          }
        ]
      }
    ],
    "actions": [
      {
        "id": "action_1",
        "label": "Back",
        "api": "/api",
        "method": "GET",
        "successMessage": "",
        "failureMessage": ""
      },
      {
        "id": "action_2",
        "label": "Next",
        "api": "/api",
        "method": "POST",
        "successMessage": "",
        "failureMessage": ""
      }
    ]
  },
  "createdAt": "2026-01-06T09:30:27.185Z",
  "updatedAt": "2026-01-07T00:27:06.146Z",
  "createdBy": "current_user",
  "updatedBy": "current_user"
}"""
        
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
    
    // Flow Engine APIs - All return flowId, currentScreenId, and full screenConfig
    /**
     * Dashboard API - Get available flows
     * GET /api/v1/dashboard/flows
     * 
     * Returns dummy dashboard flows for testing
     */
    override suspend fun getDashboardFlows(): DashboardResponseDto {
        delay(500) // Simulate network delay
        
        return DashboardResponseDto(
            flows = listOf(
                DashboardFlowDto(
                    flowId = "APPLICANT_FLOW",
                    title = "Applicant Onboarding",
                    description = "Capture applicant personal and identity details",
                    icon = "APPLICANT_ONBOARDING",
                    ui = DashboardUiMetaDto(
                        backgroundColor = "#0B2F70",
                        textColor = "#FFFFFF",
                        iconColor = "#00B2FF"
                    ),
                    startable = true,
                    productCode = "PL",
                    partnerCode = null,
                    branchCode = null
                ),
                DashboardFlowDto(
                    flowId = "KYC_FLOW",
                    title = "KYC Verification",
                    description = "Complete KYC process with document upload",
                    icon = "KYC_CAPTURE",
                    ui = DashboardUiMetaDto(
                        backgroundColor = "#1E3A5F",
                        textColor = "#FFFFFF",
                        iconColor = "#FFD700"
                    ),
                    startable = true,
                    productCode = "PL",
                    partnerCode = null,
                    branchCode = null
                ),
                DashboardFlowDto(
                    flowId = "CREDIT_CHECK_FLOW",
                    title = "Credit Check",
                    description = "Run credit bureau check and analysis",
                    icon = "CREDIT_CHECK",
                    ui = DashboardUiMetaDto(
                        backgroundColor = "#2C5F2D",
                        textColor = "#FFFFFF",
                        iconColor = "#90EE90"
                    ),
                    startable = true,
                    productCode = "PL",
                    partnerCode = null,
                    branchCode = null
                ),
                DashboardFlowDto(
                    flowId = "PAYMENT_FLOW",
                    title = "Payment Collection",
                    description = "Collect processing fees and EMI setup",
                    icon = "PAYMENT_COLLECTION",
                    ui = DashboardUiMetaDto(
                        backgroundColor = "#8B0000",
                        textColor = "#FFFFFF",
                        iconColor = "#FFB6C1"
                    ),
                    startable = true,
                    productCode = "PL",
                    partnerCode = null,
                    branchCode = null
                )
            )
        )
    }
    
    /**
     * Runtime API - Single endpoint for all navigation
     * POST /runtime/next-screen
     * 
     * Handles:
     * 1. First load (currentScreenId = null or "__START__")
     * 2. Next screen (currentScreenId + formData)
     */
    override suspend fun nextScreen(request: NextScreenRequestDto): NextScreenResponseDto {
        delay(500) // Simulate network delay
        
        val nextScreenId = if (request.currentScreenId == null || request.currentScreenId == "__START__") {
            // First load or flow start - return initial screen
            "APPLICANT_IDENTITY"
        } else {
            // Navigate to next screen based on current screen
            // In real implementation, backend Flow Engine would evaluate conditions and formData
            when (request.currentScreenId) {
                "APPLICANT_IDENTITY", "applicant_identity_details" -> "APPLICANT_BUSINESS_DETAILS"
                "APPLICANT_BUSINESS_DETAILS", "applicant_business_details" -> "APPLICANT_IDENTITY" // Loop for testing
                else -> "APPLICANT_IDENTITY" // Default fallback
            }
        }
        
        val screenConfig = getFormConfiguration(nextScreenId)
        
        return NextScreenResponseDto(
            nextScreenId = screenConfig.screenId,
            screenConfig = screenConfig
        )
    }
    
    // Legacy Flow Engine APIs - Kept for backward compatibility
    @Deprecated("Use nextScreen() instead")
    override suspend fun startFlow(request: FlowStartRequestDto): FlowResponseDto {
        delay(500) // Simulate network delay
        
        // Determine initial screen based on flowType or use default
        val initialScreenId = request.flowType ?: "APPLICANT_IDENTITY"
        val screenConfig = getFormConfiguration(initialScreenId)
        
        return FlowResponseDto(
            flowId = "applicant_onboarding_flow",
            currentScreenId = screenConfig.screenId,
            screenConfig = screenConfig
        )
    }
    
    @Deprecated("Use nextScreen() instead")
    override suspend fun navigateNext(request: FlowNextRequestDto): FlowResponseDto {
        delay(500) // Simulate network delay
        
        val nextScreenId = when (request.currentScreenId) {
            "APPLICANT_IDENTITY", "applicant_identity_details" -> "APPLICANT_BUSINESS_DETAILS"
            "APPLICANT_BUSINESS_DETAILS", "applicant_business_details" -> "APPLICANT_IDENTITY"
            else -> "APPLICANT_IDENTITY"
        }
        
        val screenConfig = getFormConfiguration(nextScreenId)
        
        return FlowResponseDto(
            flowId = "applicant_onboarding_flow",
            currentScreenId = screenConfig.screenId,
            screenConfig = screenConfig
        )
    }
    
    @Deprecated("Use nextScreen() instead")
    override suspend fun navigateBack(request: FlowBackRequestDto): FlowResponseDto {
        delay(500) // Simulate network delay
        
        val previousScreenId = when (request.currentScreenId) {
            "APPLICANT_BUSINESS_DETAILS", "applicant_business_details" -> "APPLICANT_IDENTITY"
            else -> "APPLICANT_IDENTITY"
        }
        
        val previousScreenConfig = getFormConfiguration(previousScreenId)
        
        return FlowResponseDto(
            flowId = "applicant_onboarding_flow",
            currentScreenId = previousScreenConfig.screenId,
            screenConfig = previousScreenConfig
        )
    }
}
