package dev.johnoreilly.galwaybus

/** Forces the `@js-joda/timezone` npm package (IANA tz database) into the webpack bundle;
 *  without a live reference to it, it gets tree-shaken and named zones fail to resolve. */
@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule

private val jsJodaTz = JsJodaTimeZoneModule
