# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class dev.kok.app.**$$serializer { *; }
-keepclassmembers class dev.kok.app.** { *** Companion; }
-keepclasseswithmembers class dev.kok.app.** { kotlinx.serialization.KSerializer serializer(...); }
