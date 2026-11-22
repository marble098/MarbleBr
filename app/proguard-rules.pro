# ProGuard rules for MarbleBr
-keep class com.marble098.marblebr.** { *; }
-keep class org.mozilla.geckoview.** { *; }

# GeckoView's internal engine classes (package org.mozilla.gecko.*, without the "view" suffix,
# e.g. org.mozilla.gecko.util.DebugConfig) must survive shrinking and repackaging as well.
# GeckoRuntime.create -> GeckoRuntime.init -> DebugConfig.fromFile deserializes YAML with
# SnakeYAML reflecting on DebugConfig and its members; if R8 strips or moves them, the runtime
# dies with ExceptionInInitializerError/NullPointerException before the browser can start.
-keep class org.mozilla.gecko.** { *; }
-dontwarn org.mozilla.geckoview.**
-dontwarn org.mozilla.gecko.**

# SnakeYAML initializes its loggers with Class.getPackage().getName() inside static initializers.
# If R8 moves the declaring class into the default package (or drops it), getPackage() returns null
# and the clinit throws NullPointerException wrapped in ExceptionInInitializerError — the exact
# crash observed at GeckoRuntime.create (fromFile -> ca4.<clinit>).
# Keep ALL SnakeYAML classes and their package names, not only the two loggers seen so far:
# keeping individual classes just moved the crash to the next unpinned class.
-keep,allowshrinking class org.yaml.snakeyaml.** { *; }
-keeppackagenames org.yaml.snakeyaml.**
-dontwarn org.yaml.snakeyaml.**
