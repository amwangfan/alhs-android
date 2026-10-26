# Retrofit / OkHttp / Gson
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.google.gson.**

# Keep API model fields for Gson
-keepclassmembers class com.ailihaosi.data.model.** {
    <fields>;
}
-keep class com.ailihaosi.data.model.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# Compose / coroutines
-dontwarn kotlinx.coroutines.**
-keep class androidx.compose.runtime.** { *; }

-keepclassmembers enum * { *; }

-keep class com.ailihaosi.data.local.** { *; }
