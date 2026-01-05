package com.kaleidofin.originator.domain.usecase

import com.kaleidofin.originator.domain.model.FormScreen
import com.kaleidofin.originator.domain.repository.FormRepository
import javax.inject.Inject

class GetFormConfigurationUseCase @Inject constructor(
    private val formRepository: FormRepository
) {
    suspend operator fun invoke(target: String): FormScreen {
        return formRepository.getFormConfiguration(target)
    }
}

