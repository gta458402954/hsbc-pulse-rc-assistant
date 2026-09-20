# Proguard rules for HSBC Pulse RC Assistant

# --- Kotlinx Serialization ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.emohappy.pulse.model.** { *; }

# --- AndroidX Room ---
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public void <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- AndroidX DataStore ---
-keep class androidx.datastore.** { *; }
