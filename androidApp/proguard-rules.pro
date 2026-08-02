# ── kotlinx.serialization ───────────────────────────────────────────────────
# The app deserializes the bundled GTFS snapshot and backend responses via
# kotlinx.serialization, which resolves serializers reflectively at runtime.
# These rules (from the official kotlinx.serialization docs) keep the generated
# serializers so R8 doesn't strip or rename them.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep `Companion` object fields of serializable classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Belt-and-braces: keep the app's own serializable model classes intact.
-keep @kotlinx.serialization.Serializable class dev.johnoreilly.galwaybus.** { *; }

# ── ML Kit text recognition (camera "Scan" tab) ──────────────────────────────
# TextRecognition.getClient() resolves its internal components reflectively via
# GMS/Firebase ComponentRegistrar discovery. Under R8 fullMode (default on AGP 8+)
# those internal classes get stripped/renamed, leaving a null field that crashes
# with an NPE inside com.google.mlkit.vision.text.internal.zzo. Keep the ML Kit
# public API and its GMS-internal vision-text implementation packages.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text.** { *; }
-dontwarn com.google.mlkit.**

# ── Ktor / OkHttp optional transitive deps (compile-only, not shipped) ────────
-dontwarn org.slf4j.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
