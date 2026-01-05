package com.kaleidofin.originator.data.dto

import com.google.gson.annotations.SerializedName

data class HomeScreenDto(
    @SerializedName("screenId")
    val screenId: String,
    @SerializedName("screenType")
    val screenType: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("layout")
    val layout: LayoutDto,
    @SerializedName("cards")
    val cards: List<CardDto>
)

data class LayoutDto(
    @SerializedName("columns")
    val columns: Int,
    @SerializedName("cardAspectRatio")
    val cardAspectRatio: String,
    @SerializedName("spacingDp")
    val spacingDp: Int
)

data class CardDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("icon")
    val icon: String,
    @SerializedName("backgroundColor")
    val backgroundColor: String,
    @SerializedName("textColor")
    val textColor: String,
    @SerializedName("action")
    val action: ActionDto
)

data class ActionDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("target")
    val target: String
)

