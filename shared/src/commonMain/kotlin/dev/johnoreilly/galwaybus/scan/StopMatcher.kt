package dev.johnoreilly.galwaybus.scan

import dev.johnoreilly.galwaybus.model.Stop

/**
 * Matches free-form OCR text (read off a bus-stop plate) against the known stops.
 *
 * Galway stop plates print a 6-digit stop code (the [Stop.stop_id]), sometimes broken up with a
 * dash or space (e.g. "5234-41"). We pull candidate 6-digit runs out of the recognised text and
 * look for an exact [Stop.stop_id] match. Requiring a full 6-digit token — rather than the legacy
 * `contains` check — avoids false positives from route numbers, fares, or street numbers that
 * happen to appear in frame.
 */
object StopMatcher {
    private val sixDigits = Regex("\\d{6}")

    /** The stop whose id is printed in [recognizedText], or null if no confident match. */
    fun match(recognizedText: String, stops: List<Stop>): Stop? {
        if (recognizedText.isBlank() || stops.isEmpty()) return null
        // Drop separators the plate/OCR may insert between digit groups so "5234-41" reads as one run.
        val normalized = recognizedText.replace("-", "").replace(" ", "")
        val byId = stops.associateBy { it.stop_id }
        return sixDigits.findAll(normalized)
            .map { it.value }
            .firstNotNullOfOrNull { byId[it] }
    }
}
