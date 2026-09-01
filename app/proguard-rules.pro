# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Developer\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-optimizationpasses 5

-keepattributes SourceFile, LineNumberTable

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin / Coroutines
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# OkHttp3
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# rhino
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.classfile.**
-keep class org.mozilla.javascript.** { *; }
-keep class com.haleydu.cimoc.script.** { *; }

# jsoup
-keeppackagenames org.jsoup.nodes

# andrroid v4 v7
-dontwarn android.support.v4.**
-dontwarn android.support.v7.**

# rx
-dontwarn sun.misc.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

#mongodb
-dontwarn javax.**
-dontwarn java.lang.management.**
-dontwarn io.netty.**
-dontwarn org.ietf.jgss.**
-dontwarn org.slf4j.**
-dontwarn org.xerial.snappy.**

-keep class javax.** { *; }
-keep class java.lang.management.** { *; }
-keep class io.netty.** { *; }
-keep class org.ietf.jgss.** { *; }
-keep class org.slf4j.** { *; }
-keep class org.xerial.snappy.** { *; }

# guava
-dontwarn com.google.common.base.**
-keep class com.google.common.base.** {*;}
-dontwarn com.google.errorprone.annotations.**
-keep class com.google.errorprone.annotations.** {*;}
-dontwarn com.google.j2objc.annotations.**
-keep class com.google.j2objc.annotations.** { *; }
-dontwarn java.lang.ClassValue
-keep class java.lang.ClassValue { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-keep class org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement { *; }

