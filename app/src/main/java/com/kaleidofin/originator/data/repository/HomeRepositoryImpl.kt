package com.kaleidofin.originator.data.repository

import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kaleidofin.originator.data.datasource.HomeDataSource
import com.kaleidofin.originator.domain.model.HomeAction
import com.kaleidofin.originator.domain.model.HomeLayout
import com.kaleidofin.originator.domain.repository.HomeRepository
import com.kaleidofin.originator.domain.repository.HomeScreenData
import com.kaleidofin.originator.util.IconMapper
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val homeDataSource: HomeDataSource
) : HomeRepository {
    override suspend fun getHomeScreenData(): HomeScreenData {
        val dto = homeDataSource.getHomeDashboard()
        
        val layout = HomeLayout(
            columns = dto.layout.columns,
            cardAspectRatio = dto.layout.cardAspectRatio,
            spacingDp = dto.layout.spacingDp
        )
        
        val actions = dto.cards.map { cardDto ->
            HomeAction(
                id = cardDto.id,
                title = cardDto.title,
                icon = IconMapper.getIcon(cardDto.icon),
                backgroundColor = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(cardDto.backgroundColor)),
                textColor = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(cardDto.textColor)),
                actionType = cardDto.action.type,
                actionTarget = cardDto.action.target
            )
        }
        
        return HomeScreenData(layout = layout, actions = actions)
    }
}


