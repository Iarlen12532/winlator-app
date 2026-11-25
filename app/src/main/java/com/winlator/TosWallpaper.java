package com.winlator;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;

import com.winlator.container.Container;
import com.winlator.core.WineThemeManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class TosWallpaper {
    public static void install(Context context, Container container) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("tos_wallpaper_v1", false)) return;
        try (InputStream in = context.getAssets().open("tos_wallpaper_blue.jpg")) {
            Bitmap bmp = BitmapFactory.decodeStream(in);
            if (bmp == null) return;
            File file = WineThemeManager.getUserWallpaperFile(context);
            File dir = file.getParentFile();
            if (dir != null && !dir.isDirectory()) dir.mkdirs();
            try (FileOutputStream out = new FileOutputStream(file)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            container.setDesktopTheme("DARK,IMAGE,#001B3A,user");
            container.saveData();
            prefs.edit().putBoolean("tos_wallpaper_v1", true).apply();
        }
        catch (Exception e) {}
    }
}
