# CloakRun R8 rules. Library rules ship as consumer rules with each dependency; only app-specific
# keeps belong here.

# Keep line numbers for readable Crashlytics stack traces, but hide the original source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
