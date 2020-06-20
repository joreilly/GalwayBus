# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/jooreill/devtools/adt/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# keep everything in this package from being removed or renamed
-keep class com.surrus.galwaybus.** { *; }

# joda
-keep class net.danlew.android.joda.R$raw { *; }


# okhttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.squareup.okhttp.**


-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**



# Play Services

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames class * implements android.os.Parcelable
-keepclassmembers class * implements android.os.Parcelable {
  public static final *** CREATOR;
}

-keep @interface android.support.annotation.Keep
-keep @android.support.annotation.Keep class *
-keepclasseswithmembers class * {
  @android.support.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
  @android.support.annotation.Keep <methods>;
}

-keep @interface com.google.android.gms.common.annotation.KeepName
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
  @com.google.android.gms.common.annotation.KeepName *;
}

-keep @interface com.google.android.gms.common.util.DynamiteApi
-keep public @com.google.android.gms.common.util.DynamiteApi class * {
  public <fields>;
  public <methods>;
}

-dontwarn android.security.NetworkSecurityPolicy

-keep class com.stripe.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

-dontwarn android.content.**
-keep class android.content.**

-keep class android.support.** { *; }
-keep interface android.support.** { *; }

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Firebase Authentication
-keepattributes Signature
-keepattributes *Annotation*


-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

#Kotlin

-dontwarn kotlin.**
-dontwarn org.jetbrains.annotations.NotNull


