package android.os;

/**
 * Shadow class for android.os.Build used in unit tests.
 * ExoPlayer accesses Build fields in static initializers.
 * This stub provides default values allowing us to use mockk on ExoPlayer.
 */
public class Build {
    public static final String DEVICE = "generic";
    public static final String MANUFACTURER = "STUB_MANUFACTURER";
    public static final String MODEL = "STUB_MODEL";

    /** Minimal subset of android.os.Build.VERSION_CODES needed by unit tests. */
    public static class VERSION_CODES {
        public static final int M = 23;
        public static final int N_MR1 = 25;
        public static final int O = 26;
    }
}
