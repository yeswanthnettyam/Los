package com.kaleidofin.originator.presentation.component

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.kaleidofin.originator.domain.model.FieldConstraints

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialSelectedDateMillis: Long? = null,
    dateMode: String? = null,                 // ANY | PAST | FUTURE | RANGE | AGE
    constraints: FieldConstraints? = null,    // used for AGE
    minDate: String? = null,                  // yyyy-MM-dd (for RANGE)
    maxDate: String? = null,                  // yyyy-MM-dd (for RANGE)
    onDateSelected: (String) -> Unit,         // 🔥 ALWAYS STRING
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()

    /* ---------------------------------------------------
     * 1️⃣ Calculate allowed date range
     * --------------------------------------------------- */
    val (minDateLocal, maxDateLocal) = when (dateMode) {
        "FUTURE" -> today to today.plusYears(100)

        "PAST" -> today.minusYears(100) to today

        "RANGE" -> {
            val min = minDate?.let { LocalDate.parse(it) } ?: today.minusYears(100)
            val max = maxDate?.let { LocalDate.parse(it) } ?: today.plusYears(100)
            min to max
        }

        "AGE" -> {
            val min = today.minusYears((constraints?.maxAge ?: 100).toLong())
            val max = today.minusYears((constraints?.minAge ?: 0).toLong())
            min to max
        }

        else -> {
            // ANY or null
            today.minusYears(50) to today.plusYears(50)
        }
    }

    /* ---------------------------------------------------
     * 2️⃣ SAFE initial date (🔥 NEVER boundary dates)
     * --------------------------------------------------- */
    val safeInitialDate = initialSelectedDateMillis
        ?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .coerceIn(minDateLocal, maxDateLocal)
        }
        ?: when (dateMode) {
            "AGE" -> maxDateLocal                 // DOB → safe past date
            "PAST" -> today.minusYears(1)         // ❌ not today
            "FUTURE" -> today.plusDays(1)         // ❌ not today
            "RANGE" -> minDateLocal.plusDays(1)   // ❌ not edge
            else -> today.minusYears(1)            // ANY
        }

    val initialMillis =
        safeInitialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /* ---------------------------------------------------
     * 3️⃣ DatePicker state
     * --------------------------------------------------- */
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = minDateLocal.year..maxDateLocal.year
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

                        // 🔥 ALWAYS store as STRING
                        onDateSelected(selectedDate.toString()) // yyyy-MM-dd
                    }
                    onDismiss()
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
