package io.homeassistant.companion.android.settings.shortcuts

import android.os.Build
import io.homeassistant.companion.android.common.util.SdkVersion

/** Whether app shortcuts are available on this device (requires Android 7.1 / API 25). */
internal fun areShortcutsSupported(): Boolean = SdkVersion.isAtLeast(Build.VERSION_CODES.N_MR1)
