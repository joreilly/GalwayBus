package dev.johnoreilly.galwaybus

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal actual fun nowEpochMilliseconds(): Long = System.currentTimeMillis()

internal actual fun todayIn(timeZone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(timeZone).date

internal actual fun nowLocalDateTimeIn(timeZone: TimeZone): LocalDateTime =
    Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(timeZone)
