package lineageos.providers;

import android.net.Uri;
import android.content.ContentResolver;

public class LineageSettings {
    public static class Secure {
        public static Uri getUriFor(String name) { return null; }
    }
    public static class System {
        public static final Uri CONTENT_URI = Uri.parse("content://lineage_system");
        public static int getInt(ContentResolver cr, String name, int def) { return def; }
    }
}
