package com.kaleidofin.originator.data.mapper

import androidx.compose.ui.graphics.Color
import com.kaleidofin.originator.data.dto.DashboardFlowDto
import com.kaleidofin.originator.data.dto.DashboardUiMetaDto
import com.kaleidofin.originator.domain.model.DashboardFlow
import com.kaleidofin.originator.domain.model.DashboardUiMeta

/**
 * Map Dashboard DTO to Domain model
 */
fun DashboardFlowDto.toDomain(): DashboardFlow {
    return DashboardFlow(
        flowId = flowId,
        title = title,
        description = description,
        icon = icon,
        ui = ui?.toDomain(),
        startable = startable,
        productCode = productCode,
        partnerCode = partnerCode,
        branchCode = branchCode
    )
}

/**
 * Map Dashboard UI Meta DTO to Domain model
 */
fun DashboardUiMetaDto.toDomain(): DashboardUiMeta {
    return DashboardUiMeta(
        backgroundColor = backgroundColor?.let { parseColor(it) },
        textColor = textColor?.let { parseColor(it) },
        iconColor = iconColor?.let { parseColor(it) }
    )
}

/**
 * Parse hex color string to Compose Color
 * Supports formats: #RGB, #ARGB, #RRGGBB, #AARRGGBB
 * Returns null if parsing fails
 */
private fun parseColor(hexColor: String): Color? {
    return try {
        val cleanHex = hexColor.removePrefix("#")
        val colorLong = when (cleanHex.length) {
            3 -> { // #RGB -> #RRGGBB
                val r = cleanHex[0].toString().repeat(2)
                val g = cleanHex[1].toString().repeat(2)
                val b = cleanHex[2].toString().repeat(2)
                "FF$r$g$b".toLong(16)
            }
            4 -> { // #ARGB -> #AARRGGBB
                val a = cleanHex[0].toString().repeat(2)
                val r = cleanHex[1].toString().repeat(2)
                val g = cleanHex[2].toString().repeat(2)
                val b = cleanHex[3].toString().repeat(2)
                "$a$r$g$b".toLong(16)
            }
            6 -> { // #RRGGBB -> #AARRGGBB
                "FF$cleanHex".toLong(16)
            }
            8 -> { // #AARRGGBB
                cleanHex.toLong(16)
            }
            else -> return null
        }
        Color(colorLong.toULong())
    } catch (e: Exception) {
        null // Return null on parse failure (fallback to defaults)
    }
}
