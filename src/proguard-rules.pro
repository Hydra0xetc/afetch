-keep public class org.afetch.Main {
    public static void main(java.lang.String[]);
}

# obfuscating stubClass cause RuntimeException
# see: https://issuetracker.google.com/issues/131619590
-keep class android.os.ServiceManager { *; }

-keep class android.app.IActivityManager { *; }
-keep class android.app.IActivityManager$Stub { *; }
