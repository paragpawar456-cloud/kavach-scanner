# OkHttp / kotlinx.serialization (used by the scan engine)
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepattributes *Annotation*, InnerClasses
# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
# Tink (security-crypto)
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
