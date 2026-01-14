package com.kaleidofin.originator.domain.repository

import com.kaleidofin.originator.domain.model.FormScreen

interface FormRepository {
    suspend fun getMasterData(dataSource: String): List<String>
}

