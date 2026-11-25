package com.winlator.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.winlator.win32.MSBitmap;
import com.winlator.xenvironment.RootFS;
import com.winlator.xserver.ScreenInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public abstract class WineThemeManager {
    public enum Theme {LIGHT, DARK}
    public enum BackgroundType {IMAGE, COLOR}
    public static final String DEFAULT_DESKTOP_THEME = Theme.LIGHT+","+BackgroundType.IMAGE+",#0277bd";
    public static final String DEFAULT_WALLPAPER_ID = "wallpaper-1";

    public static class ThemeInfo {
        public final Theme theme;
        public final BackgroundType backgroundType;
        public final int backgroundColor;
        public final String wallpaperId;

        public ThemeInfo(String value) {
            String[] values = value.split(",");
            theme = Theme.valueOf(values[0]);

            if (values.length < 3) {
                backgroundColor = Color.parseColor(values[1]);
                backgroundType = BackgroundType.IMAGE;
            }
            else {
                backgroundType = BackgroundType.valueOf(values[1]);
                backgroundColor = Color.parseColor(values[2]);
            }

            String lastValue = values[values.length-1];
            wallpaperId = lastValue.startsWith("wallpaper-") ? lastValue : (lastValue.equals("0") ? DEFAULT_WALLPAPER_ID : "user-wallpaper");
        }
    }

    public static void apply(Context context, ThemeInfo themeInfo, ScreenInfo screenInfo) {
        File rootDir = RootFS.find(context).getRootDir();
        File userRegFile = new File(rootDir, RootFS.WINEPREFIX+"/user.reg");
        String background = Color.red(themeInfo.backgroundColor)+" "+Color.green(themeInfo.backgroundColor)+" "+Color.blue(themeInfo.backgroundColor);

        if (themeInfo.backgroundType == BackgroundType.IMAGE) createWallpaperBMPFile(context, themeInfo.wallpaperId, screenInfo);

        try (WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile)) {
            if (themeInfo.backgroundType == BackgroundType.IMAGE) {
                registryEditor.setStringValue("Control Panel\\Desktop", "Wallpaper", RootFS.getDosUserCachePath()+"\\wallpaper.bmp");
            }
            else registryEditor.removeValue("Control Panel\\Desktop", "Wallpaper");

            if (themeInfo.theme == Theme.LIGHT) {
                registryEditor.setStringValue("Control Panel\\Colors", "ActiveBorder", "245 245 245");
                registryEditor.setStringValue("Control Panel\\Colors", "ActiveTitle", "96 125 139");
                registryEditor.setStringValue("Control Panel\\Colors", "Background", background);
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonAlternateFace", "245 245 245");
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonDkShadow", "158 158 158");
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonFace", "245 245 245");
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonHilight", "224 224 224");
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonLight", "255 255 255");
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonShadow", "158 158 158");
                registryEditor.setStringValue("Control Panel\\Colors", "ButtonText", "0 0 0");
                registryEditor.setStringValue("Control Panel\\Colors", "GradientActiveTitle", "96 125 139");
                registryEditor.setStringValue("Control Panel\\Colors", "GradientInactiveTitle", "117 117 117");
                registryEditor.setStringValue("Control Panel\\Colors", "GrayText", "158 158 158");
                registryEditor.setStringValue("Control Panel\\Colors", "Hilight", "2 136 209");
                registryEditor.setStringValue("Control Panel\\Colors", "HilightText", "255 255 255");
                registryEditor.setStringValue("Control Panel\\Colors", "HotTrackingColor", "2 1
