# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/jooreill/devtools/adt/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.kts.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# keep everything in this package from being removed or renamed
-keep class dev.johnoreilly.galwaybus.** { *; }
-keep class com.surrus.galwaybus.** { *; }

# okhttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.squareup.okhttp.**


-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**



-keep,allowoptimization class com.google.android.libraries.maps.** { *; }
-keep,allowoptimization class com.google.android.apps.gmm.renderer.** { *; }


-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames class * implements android.os.Parcelable
-keepclassmembers class * implements android.os.Parcelable {
  public static final *** CREATOR;
}

-keep @interface com.google.android.gms.common.annotation.KeepName
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
  @com.google.android.gms.common.annotation.KeepName *;
}

#-keep @interface com.google.android.gms.common.util.DynamiteApi
#-keep public @com.google.android.gms.common.util.DynamiteApi class * {
#  public <fields>;
#  public <methods>;
#}

-dontwarn android.security.NetworkSecurityPolicy

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


#Kotlin

-dontwarn kotlin.**
-dontwarn org.jetbrains.annotations.NotNull

-dontwarn org.slf4j.impl.StaticLoggerBinder



-dontwarn com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
-dontwarn com.fasterxml.jackson.dataformat.xml.XmlMapper
-dontwarn com.sun.org.apache.xml.internal.utils.PrefixResolver
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IndexedPropertyDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.MethodDescriptor
-dontwarn java.beans.PropertyDescriptor
-dontwarn javax.swing.tree.TreeNode
-dontwarn org.apache.xml.utils.PrefixResolver
-dontwarn org.jaxen.FunctionContext
-dontwarn org.jaxen.NamespaceContext
-dontwarn org.jaxen.Navigator
-dontwarn org.jaxen.VariableContext
-dontwarn org.jaxen.XPathFunctionContext
-dontwarn org.jaxen.dom.DocumentNavigator
-dontwarn org.jetbrains.dokka.CoreExtensions
-dontwarn org.jetbrains.dokka.DokkaConfiguration$DokkaSourceSet
-dontwarn org.jetbrains.dokka.DokkaConfiguration$PluginConfiguration
-dontwarn org.jetbrains.dokka.DokkaConfiguration$SerializationFormat
-dontwarn org.jetbrains.dokka.DokkaConfiguration
-dontwarn org.jetbrains.dokka.DokkaSourceSetID
-dontwarn org.jetbrains.dokka.Platform
-dontwarn org.jetbrains.dokka.analysis.kotlin.internal.DocumentableSourceLanguageParser
-dontwarn org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin
-dontwarn org.jetbrains.dokka.analysis.kotlin.internal.ModuleAndPackageDocumentationReader
-dontwarn org.jetbrains.dokka.analysis.kotlin.internal.SampleProviderFactory
-dontwarn org.jetbrains.dokka.generation.Generation
-dontwarn org.jetbrains.dokka.links.Callable
-dontwarn org.jetbrains.dokka.links.DRI
-dontwarn org.jetbrains.dokka.links.DriTarget
-dontwarn org.jetbrains.dokka.model.Annotations$Annotation
-dontwarn org.jetbrains.dokka.model.Annotations$AnnotationScope
-dontwarn org.jetbrains.dokka.model.DisplaySourceSet
-dontwarn org.jetbrains.dokka.model.DisplaySourceSetKt
-dontwarn org.jetbrains.dokka.model.ExtraModifiers$KotlinOnlyModifiers$External
-dontwarn org.jetbrains.dokka.model.ExtraModifiers$KotlinOnlyModifiers$TailRec
-dontwarn org.jetbrains.dokka.model.ExtraModifiers$KotlinOnlyModifiers
-dontwarn org.jetbrains.dokka.model.JavaModifier$Final
-dontwarn org.jetbrains.dokka.model.JavaVisibility$Public
-dontwarn org.jetbrains.dokka.model.KotlinModifier$Final
-dontwarn org.jetbrains.dokka.model.KotlinVisibility$Public
-dontwarn org.jetbrains.dokka.model.Modifier
-dontwarn org.jetbrains.dokka.model.Visibility
-dontwarn org.jetbrains.dokka.model.WithChildren
-dontwarn org.jetbrains.dokka.pages.RendererSpecificResourcePage
-dontwarn org.jetbrains.dokka.pages.RenderingStrategy$Copy
-dontwarn org.jetbrains.dokka.pages.RenderingStrategy
-dontwarn org.jetbrains.dokka.plugability.ConfigurableBlock
-dontwarn org.jetbrains.dokka.plugability.DokkaContext
-dontwarn org.jetbrains.dokka.plugability.DokkaPlugin$ExtensionProvider

-dontwarn org.jetbrains.dokka.plugability.DokkaPlugin
-dontwarn org.jetbrains.dokka.plugability.DokkaPluginKt
-dontwarn org.jetbrains.dokka.plugability.ExtendingDSL
-dontwarn org.jetbrains.dokka.plugability.Extension
-dontwarn org.jetbrains.dokka.plugability.ExtensionPoint
-dontwarn org.jetbrains.dokka.plugability.OrderDsl
-dontwarn org.jetbrains.dokka.renderers.Renderer
-dontwarn org.jetbrains.dokka.transformers.documentation.DocumentableMerger
-dontwarn org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
-dontwarn org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
-dontwarn org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
-dontwarn org.jetbrains.dokka.transformers.pages.PageTransformer
-dontwarn org.jetbrains.dokka.utilities.DokkaLogger
-dontwarn org.jetbrains.dokka.utilities.JsonKt
-dontwarn org.jetbrains.dokka.utilities.TypeReference$Companion
-dontwarn org.jetbrains.dokka.utilities.TypeReference
-dontwarn org.python.core.Py
-dontwarn org.python.core.PyDictionary
-dontwarn org.python.core.PyFloat
-dontwarn org.python.core.PyInteger
-dontwarn org.python.core.PyLong
-dontwarn org.python.core.PyNone
-dontwarn org.python.core.PyObject
-dontwarn org.python.core.PySequence
-dontwarn org.python.core.PyStringMap
-dontwarn org.python.core.PySystemState
-dontwarn org.zeroturnaround.javarebel.ClassEventListener
-dontwarn org.zeroturnaround.javarebel.Reloader
-dontwarn org.zeroturnaround.javarebel.ReloaderFactory