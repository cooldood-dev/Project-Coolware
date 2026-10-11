-injars  'c:/Users/Admin/Documents/Coolware v2/Coolware v2/MavenMCP-1.8.9-master/target/MCP-1.8.9-jar-with-dependencies.jar'(!META-INF/MANIFEST.MF)
-outjars 'c:/Users/Admin/Documents/Coolware v2/Coolware v2/MavenMCP-1.8.9-master/target/Coolware-obf.jar'

-libraryjars 'C:/Program Files/Java/jdk1.8.0_202/jre/lib/rt.jar'
-libraryjars 'C:/Program Files/Java/jdk1.8.0_202/jre/lib/jce.jar'

-dontshrink
-dontoptimize
-dontnote
-dontwarn **
-ignorewarnings

# Preserve generic signatures, inner classes, enclosing methods and source attributes for reflection and Gson
-keepattributes Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,*Annotation*

# Keep Alt manager model, enums, and GUI intact
-keep class coolware.alts.** { *; }
-keepclassmembers class coolware.alts.** { *; }
-keep enum coolware.alts.** { *; }
-keepclassmembers enum coolware.alts.** { *; }
-keep class coolware.gui.altmanager.** { *; }
-keepclassmembers class coolware.gui.altmanager.** { *; }

# Keep all standard Minecraft and 3rd party library packages completely untouched
-keep class net.minecraft.** { *; }
-keep interface net.minecraft.** { *; }
-keep enum net.minecraft.** { *; }

-keep class org.lwjgl.** { *; }
-keep interface org.lwjgl.** { *; }
-keep enum org.lwjgl.** { *; }

-keep class org.apache.** { *; }
-keep interface org.apache.** { *; }
-keep enum org.apache.** { *; }

-keep class io.netty.** { *; }
-keep interface io.netty.** { *; }
-keep enum io.netty.** { *; }

-keep class com.google.** { *; }
-keep interface com.google.** { *; }
-keep enum com.google.** { *; }

-keep class com.mojang.** { *; }
-keep interface com.mojang.** { *; }
-keep enum com.mojang.** { *; }

-keep class com.paulscode.** { *; }
-keep interface com.paulscode.** { *; }
-keep class paulscode.** { *; }
-keep interface paulscode.** { *; }

-keep class tv.twitch.** { *; }
-keep interface tv.twitch.** { *; }

-keep class oshi.** { *; }
-keep interface oshi.** { *; }

-keep class joptsimple.** { *; }
-keep interface joptsimple.** { *; }

-keep class javax.** { *; }
-keep interface javax.** { *; }

-keep class net.java.games.** { *; }
-keep interface net.java.games.** { *; }

-keep class net.optifine.** { *; }
-keep interface net.optifine.** { *; }
-keep class optifine.** { *; }
-keep interface optifine.** { *; }

# Keep native methods untouched
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep reflective enum values & valueOf
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Coolware public hooks and entry points
-keep public class coolware.Coolware {
    public static *;
    public *;
}
-keep public class coolware.security.ClientAuthManager {
    public static *;
    public *;
}

# Obfuscate Coolware modules, utils, and internal classes
-repackageclasses 'coolware.internal'
-allowaccessmodification
-overloadaggressively
-useuniqueclassmembernames
