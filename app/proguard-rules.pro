# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.comfortfriend.app.**$$serializer { *; }
-keepclassmembers class com.comfortfriend.app.** { *** Companion; }
-keepclasseswithmembers class com.comfortfriend.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Retrofit
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep interface com.comfortfriend.app.data.remote.GroqApi { *; }
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
