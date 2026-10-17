# CloakRun R8 rules. Library rules ship as consumer rules with each dependency; only app-specific
# keeps belong here.

# Keep line numbers for readable Crashlytics stack traces, but hide the original source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Credential Manager finds its Play services provider by reflection (rule from the androidx.credentials docs).
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}
