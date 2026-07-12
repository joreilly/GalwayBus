package dev.johnoreilly.galwaybus

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
internal actual fun nowEpochMilliseconds(): Long = time(null).toLong() * 1000L

internal actual fun todayIn(timeZone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(nowEpochMilliseconds()).toLocalDateTime(timeZone).date

internal actual fun nowLocalDateTimeIn(timeZone: TimeZone): LocalDateTime =
    Instant.fromEpochMilliseconds(nowEpochMilliseconds()).toLocalDateTime(timeZone)
