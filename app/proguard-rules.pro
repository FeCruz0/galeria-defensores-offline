# ProGuard rules for the Galeria de Defensores Android application

# Keep all public classes and their members
-keep public class com.galeria.defensores.** { *; }

# Keep the Character data class
-keep class com.galeria.defensores.models.Character { *; }

# Keep the UI fragments
-keep class com.galeria.defensores.ui.** { *; }

# Keep the MainActivity
-keep class com.galeria.defensores.MainActivity { *; }

# Prevent obfuscation of the application entry point
-keep public class * extends android.app.Application {
    public <init>();
}