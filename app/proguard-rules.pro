# ProGuard rules for RE TimeBox Lite
-keep class com.retimebox.lite.data.local.entity.** { *; }
-keep class com.retimebox.lite.data.local.converter.** { *; }

# TBS X5 SDK
-keep class com.tencent.smtt.** { *; }
-dontwarn com.tencent.smtt.**
