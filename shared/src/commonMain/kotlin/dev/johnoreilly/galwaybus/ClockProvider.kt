package dev.johnoreilly.galwaybus

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

internal expect fun nowEpochMilliseconds(): Long
internal expect fun todayIn(timeZone: TimeZone): LocalDate
internal expect fun nowLocalDateTimeIn(timeZone: TimeZone): LocalDateTime
