package vn.edu.usth.flightinfo.ui.common;

import android.content.Context;

public final class LocaleUtils {

    private LocaleUtils() {
    }

    /** True when the UI is currently shown in Vietnamese (system or in-app language). */
    public static boolean isVietnamese(Context context) {
        return "vi".equals(context.getResources().getConfiguration().getLocales().get(0).getLanguage());
    }
}
