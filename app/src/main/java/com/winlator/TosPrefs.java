package com.winlator;

import android.content.Context;
import android.content.SharedPreferences;

public class TosPrefs {
    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("tos_prefs", Context.MODE_PRIVATE);
    }

    public static int dockIconDp(Context c) { return sp(c).getInt("dock_icon_dp", 56); }
    public static void setDockIconDp(Context c, int v) { sp(c).edit().putInt("dock_icon_dp", v).apply(); }

    public static int uiPercent(Context c) { return sp(c).getInt("ui_percent", 100); }
    public static void setUiPercent(Context c, int v) { sp(c).edit().putInt("ui_percent", v).apply(); }

    public static float density(Context c) { return c.getResources().getDisplayMetrics().density; }

    public static int px(Context c, float dp) {
        return Math.round(dp * density(c) * uiPercent(c) / 100f);
    }

    public static int dockPx(Context c) {
        return Math.round(dockIconDp(c) * density(c));
    }
}
