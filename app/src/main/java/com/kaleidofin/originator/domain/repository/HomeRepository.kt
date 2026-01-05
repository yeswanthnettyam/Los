package com.kaleidofin.originator.domain.repository

import com.kaleidofin.originator.domain.model.HomeAction
import com.kaleidofin.originator.domain.model.HomeLayout

data class HomeScreenData(
    val layout: HomeLayout,
    val actions: List<HomeAction>
)

interface HomeRepository {
    suspend fun getHomeScreenData(): HomeScreenData
}


