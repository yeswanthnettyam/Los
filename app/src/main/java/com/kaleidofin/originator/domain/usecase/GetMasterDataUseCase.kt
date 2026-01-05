package com.kaleidofin.originator.domain.usecase

import com.kaleidofin.originator.domain.repository.FormRepository
import javax.inject.Inject

class GetMasterDataUseCase @Inject constructor(
    private val formRepository: FormRepository
) {
    suspend operator fun invoke(dataSource: String): List<String> {
        return formRepository.getMasterData(dataSource)
    }
}

