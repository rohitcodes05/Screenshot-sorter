# ScreenSort Proguard rules
# Keep Google ML Kit Text Recognition classes
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep Room database entities and DAOs
-keep class com.screensort.app.data.local.** { *; }
