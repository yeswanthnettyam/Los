package com.kaleidofin.originator.domain.usecase

import com.kaleidofin.originator.domain.repository.HomeRepository
import com.kaleidofin.originator.domain.repository.HomeScreenData
import javax.inject.Inject

class GetHomeActionsUseCase @Inject constructor(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke(): HomeScreenData {
        return homeRepository.getHomeScreenData()
    }
}


