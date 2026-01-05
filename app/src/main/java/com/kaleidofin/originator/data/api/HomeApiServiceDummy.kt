package com.kaleidofin.originator.data.api

import com.kaleidofin.originator.data.dto.ActionDto
import com.kaleidofin.originator.data.dto.CardDto
import com.kaleidofin.originator.data.dto.HomeScreenDto
import com.kaleidofin.originator.data.dto.LayoutDto
import kotlinx.coroutines.delay

class HomeApiServiceDummy : HomeApiService {
    override suspend fun getHomeDashboard(): HomeScreenDto {
        // Simulate network delay
        delay(500)
        
        return HomeScreenDto(
            screenId = "HOME_DASHBOARD",
            screenType = "GRID",
            title = "Home Screen",
            layout = LayoutDto(
                columns = 2,
                cardAspectRatio = "1:1",
                spacingDp = 12
            ),
            cards = listOf(
                CardDto(
                    id = "APPLICANT_ONBOARDING",
                    title = "Applicant and co-applicant onboarding",
                    icon = "ic_applicant_onboarding",
                    backgroundColor = "#0A2A74",
                    textColor = "#FFFFFF",
                    action = ActionDto(
                        type = "NAVIGATION",
                        target = "APPLICANT_IDENTITY"
                    )
                ),
                CardDto(
                    id = "CREDIT_CHECK",
                    title = "Credit check",
                    icon = "ic_credit_check",
                    backgroundColor = "#0A2A74",
                    textColor = "#FFFFFF",
                    action = ActionDto(
                        type = "API_FLOW",
                        target = "CREDIT_BUREAU_CHECK"
                    )
                ),
                CardDto(
                    id = "GROUP_CREATION",
                    title = "Group creation",
                    icon = "ic_group_creation",
                    backgroundColor = "#0A2A74",
                    textColor = "#FFFFFF",
                    action = ActionDto(
                        type = "NAVIGATION",
                        target = "GROUP_CREATION_SCREEN"
                    )
                ),
                CardDto(
                    id = "KYC_CAPTURE",
                    title = "KYC Capture",
                    icon = "ic_kyc_capture",
                    backgroundColor = "#0A2A74",
                    textColor = "#FFFFFF",
                    action = ActionDto(
                        type = "NAVIGATION",
                        target = "KYC_FLOW"
                    )
                ),
                CardDto(
                    id = "FIELD_VERIFICATION",
                    title = "Field Verification",
                    icon = "ic_field_verification",
                    backgroundColor = "#0A2A74",
                    textColor = "#FFFFFF",
                    action = ActionDto(
                        type = "NAVIGATION",
                        target = "FIELD_VERIFICATION_FLOW"
                    )
                ),
                CardDto(
                    id = "PERSONAL_DISCUSSION",
                    title = "Personal discussion",
                    icon = "ic_personal_discussion",
                    backgroundColor = "#0A2A74",
                    textColor = "#FFFFFF",
                    action = ActionDto(
                        type = "NAVIGATION",
                        target = "PD_FLOW"
                    )
                ),
                CardDto(
                    id = "ELIGIBILITY_REJECTION",
                    title = "Eligibility / rejection",
                    icon = "ic_eligibility",
                    backgroundColor = "#0A2A74",
                    textColor = "#FFFFFF",
                    action = ActionDto(
                        type = "API_FLOW",
                        target = "ELIGIBILITY_CHECK"
                    )
                ),
                CardDto(
                    id = "LOAN_DOC_SIGNING",
                    title = "Loan documents signing",
                    icon = "ic_doc_signing",
                    backgroundColor = "#0A2A74",
                    textColor = "#FFFFFF",
                    action = ActionDto(
                        type = "NAVIGATION",
                        target = "DOCUMENT_SIGNING_FLOW"
                    )
                ),
                CardDto(
                    id = "PAYMENT_COLLECTION",
                    title = "Payment collection",
                    icon = "ic_payment_collection",
                    backgroundColor = "#0A2A74",
                    textColor = "#FFFFFF",
                    action = ActionDto(
                        type = "NAVIGATION",
                        target = "PAYMENT_COLLECTION_FLOW"
                    )
                )
            )
        )
    }
}

