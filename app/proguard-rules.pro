# kotlinx.serialization (used by :core to read rudiments.json)
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class io.github.rdiecv.paradiddle.core.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.github.rdiecv.paradiddle.core.**$$serializer { *; }
