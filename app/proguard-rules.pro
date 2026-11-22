# ProGuard rules for MarbleBr
-keep class com.marble098.marblebr.** { *; }
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**

# SnakeYAML uses Class.getPackage().getName() to initialize these loggers.
# Preserve their packages so R8 cannot move them into the default package.
-keep,allowshrinking class org.yaml.snakeyaml.TypeDescription
-keep,allowshrinking class org.yaml.snakeyaml.introspector.PropertySubstitute
