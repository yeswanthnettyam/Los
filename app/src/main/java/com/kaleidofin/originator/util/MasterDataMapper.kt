package com.kaleidofin.originator.util

object MasterDataMapper {
    fun getOptionsForDataSource(dataSource: String?): List<String> {
        return when (dataSource) {
            "MASTER_SALUTATION" -> listOf("Mr.", "Mrs.", "Ms.", "Dr.", "Prof.")
            "MASTER_GENDER" -> listOf("Male", "Female", "Other")
            "MASTER_SECONDARY_ID" -> listOf("Driving License", "Passport", "Voter ID", "Other")
            "MASTER_EDUCATION" -> listOf("Below 10th", "10th", "12th", "Graduate", "Post Graduate", "Professional")
            "MASTER_MARITAL_STATUS" -> listOf("Single", "Married", "Divorced", "Widowed")
            "MASTER_RELIGION" -> listOf("Hindu", "Muslim", "Christian", "Sikh", "Buddhist", "Jain", "Other")
            "MASTER_CASTE" -> listOf("General", "OBC", "SC", "ST", "Other")
            "MASTER_RESIDENCE_TYPE" -> listOf("Owned", "Rented", "Parental", "Other")
            "MASTER_STATE" -> listOf(
                "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
                "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand",
                "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur",
                "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
                "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
                "Uttar Pradesh", "Uttarakhand", "West Bengal"
            )
            "MASTER_ADDRESS_TYPE" -> listOf("Current", "Permanent", "Office", "Other")
            "MASTER_PREMISE_OWNERSHIP" -> listOf("Owned", "Rented", "Leased", "Other")
            "MASTER_SUB_INDUSTRY" -> listOf("Retail", "Manufacturing", "Services", "Agriculture", "Other")
            "MASTER_BUSINESS_KYC" -> listOf("GST Certificate", "Trade License", "Partnership Deed", "Other")
            "MASTER_OTHER_BUSINESS_KYC" -> listOf("MSME Certificate", "Import Export Code", "Other")
            "MASTER_LOAN_TENURE" -> listOf("6 Months", "12 Months", "24 Months", "36 Months", "48 Months", "60 Months")
            "MASTER_LOAN_PURPOSE" -> listOf("Working Capital", "Equipment Purchase", "Expansion", "Debt Consolidation", "Other")
            else -> emptyList()
        }
    }
}

