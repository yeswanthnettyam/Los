package com.kaleidofin.originator.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class HomeAction(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val textColor: Color,
    val actionType: String,
    val actionTarget: String
)

data class HomeLayout(
    val columns: Int,
    val cardAspectRatio: String,
    val spacingDp: Int
)


