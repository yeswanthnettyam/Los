package com.kaleidofin.originator.presentation.component

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.kaleidofin.originator.domain.model.DateConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialSelectedDateMillis: Long? = null,
    dateConfig: DateConfig? = null,
    onDateSelected: (String) -> Unit,         // 🔥 ALWAYS STRING
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    val validationType = dateConfig?.validationType ?: "ANY"

    /* ---------------------------------------------------
     * RUNTIME CONSTRAINT COMPUTATION (MANDATORY)
     * --------------------------------------------------- */
    val (minDateLocal: LocalDate?, maxDateLocal: LocalDate?) = when (validationType.uppercase()) {
        "ANY" -> {
            // No constraints
            null to null
        }
        
        "FUTURE_ONLY", "FUTURE" -> {
            // minDate = today, maxDate = null
            today to null
        }
        
        "PAST_ONLY", "PAST" -> {
            // minDate = null, maxDate = today
            null to today
        }
        
        "AGE_RANGE" -> {
            // IF minAge exists: maxDate = today - minAge years
            // IF maxAge exists: minDate = today - maxAge years
            val computedMinDate = dateConfig?.maxAge?.let { today.minusYears(it.toLong()) }
            val computedMaxDate = dateConfig?.minAge?.let { today.minusYears(it.toLong()) }
            computedMinDate to computedMaxDate
        }
        
        "DATE_RANGE" -> {
            // minDate = config.minDate, maxDate = config.maxDate
            val min = dateConfig?.minDate?.let { 
                try { LocalDate.parse(it) } catch (e: Exception) { null }
            }
            val max = dateConfig?.maxDate?.let { 
                try { LocalDate.parse(it) } catch (e: Exception) { null }
            }
            min to max
        }
        
        "OFFSET" -> {
            // minDate = today + offset (MONTH), maxDate = today
            val offset = dateConfig?.offset ?: 0
            val unit = dateConfig?.unit?.uppercase() ?: "MONTH"
            val computedMinDate = when (unit) {
                "MONTH" -> today.plusMonths(offset.toLong())
                "YEAR" -> today.plusYears(offset.toLong())
                "DAY" -> today.plusDays(offset.toLong())
                else -> today.plusMonths(offset.toLong())
            }
            computedMinDate to today
        }
        
        else -> {
            // Fallback: ANY (no constraints)
            null to null
        }
    }

    /* ---------------------------------------------------
     * 2️⃣ SAFE initial date (🔥 NEVER boundary dates)
     * --------------------------------------------------- */
    val safeInitialDate = initialSelectedDateMillis
        ?.let {
            val parsedDate = Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            
            // Coerce within bounds if constraints exist
            when {
                minDateLocal != null && maxDateLocal != null -> parsedDate.coerceIn(minDateLocal, maxDateLocal)
                minDateLocal != null -> parsedDate.coerceAtLeast(minDateLocal)
                maxDateLocal != null -> parsedDate.coerceAtMost(maxDateLocal)
                else -> parsedDate
            }
        }
        ?: when (validationType.uppercase()) {
            "AGE_RANGE" -> maxDateLocal ?: today.minusYears(1)  // DOB → safe past date
            "PAST_ONLY", "PAST" -> today.minusDays(1)          // ❌ not today
            "FUTURE_ONLY", "FUTURE" -> today.plusDays(1)        // ❌ not today
            "DATE_RANGE" -> minDateLocal?.plusDays(1) ?: today.minusYears(1)  // ❌ not edge
            "OFFSET" -> minDateLocal?.plusDays(1) ?: today.plusDays(1)
            else -> today.minusYears(1)                         // ANY
        }

    val initialMillis =
        safeInitialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /* ---------------------------------------------------
     * 3️⃣ DatePicker state with computed year range
     * --------------------------------------------------- */
    val yearRange = when {
        minDateLocal != null && maxDateLocal != null -> IntRange(minDateLocal.year, maxDateLocal.year)
        minDateLocal != null -> IntRange(minDateLocal.year, today.year + 10)
        maxDateLocal != null -> IntRange(1900, maxDateLocal.year)
        else -> IntRange(1900, today.year + 10) // Default range for ANY
    }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = yearRange
    )
    
    /* ---------------------------------------------------
     * 4️⃣ Dialog UI
     * --------------------------------------------------- */
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,

        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate =
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        
                        // Validate date is within constraints
                        val isValid = (minDateLocal == null || !selectedDate.isBefore(minDateLocal)) &&
                                     (maxDateLocal == null || !selectedDate.isAfter(maxDateLocal))
                        
                        if (isValid) {
                            // 🔥 ALWAYS store as STRING
                            onDateSelected(selectedDate.toString()) // yyyy-MM-dd
                            onDismiss()
                        }
                        // If invalid, don't dismiss - user can select another date
                    } ?: run {
                        // No date selected, just dismiss
                        onDismiss()
                    }
                }
            ) {
                Text("OK")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier
        )
    }
}
