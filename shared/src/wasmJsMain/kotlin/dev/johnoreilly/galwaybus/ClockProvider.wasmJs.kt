package dev.johnoreilly.galwaybus

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal actual fun nowEpochMilliseconds(): Long = Clock.System.now().toEpochMilliseconds()

internal actual fun todayIn(timeZone: TimeZone): LocalDate =
    Clock.System.now().toLocalDateTime(timeZone).date

internal actual fun nowLocalDateTimeIn(timeZone: TimeZone): LocalDateTime =
    Clock.System.now().toLocalDateTime(timeZone)
