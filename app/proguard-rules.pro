# Card Counter App ProGuard Rules

# Keep line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep the database helper class
-keep class dm.app.card.fuck.df.data.CardDatabaseHelper { *; }

# Keep Compose runtime
-keep class androidx.compose.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Keep data classes used in JSON serialization
-keep class dm.app.card.fuck.df.data.** { *; }

# Keep Activity and Application classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# Keep SQLite classes
-keep class android.database.** { *; }
-keep class android.database.sqlite.** { *; }
