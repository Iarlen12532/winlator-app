package com.winlator.core;

public abstract class TosTheme {
    private static void c(WineRegistryEditor r, String name, String rgb) {
        r.setStringValue("Control Panel\\Colors", name, rgb);
    }

    public static void applyColors(WineRegistryEditor r, boolean dark) {
        if (dark) {
            c(r, "ActiveBorder", "44 44 46");
            c(r, "ActiveTitle", "44 44 46");
            c(r, "GradientActiveTitle", "44 44 46");
            c(r, "TitleText", "235 235 245");
            c(r, "InactiveBorder", "36 36 38");
            c(r, "InactiveTitle", "36 36 38");
            c(r, "GradientInactiveTitle", "36 36 38");
            c(r, "InactiveTitleText", "140 140 150");
            c(r, "ButtonFace", "58 58 60");
            c(r, "ButtonAlternateFace", "58 58 60");
            c(r, "ButtonHilight", "72 72 74");
            c(r, "ButtonLight", "72 72 74");
            c(r, "ButtonShadow", "28 28 30");
            c(r, "ButtonDkShadow", "0 0 0");
            c(r, "ButtonText", "235 235 245");
            c(r, "Window", "30 30 32");
            c(r, "WindowText", "235 235 245");
            c(r, "WindowFrame", "20 20 22");
            c(r, "Menu", "44 44 46");
            c(r, "MenuBar", "44 44 46");
            c(r, "MenuText", "235 235 245");
            c(r, "MenuHilight", "10 132 255");
            c(r, "Hilight", "10 132 255");
            c(r, "HilightText", "255 255 255");
            c(r, "HotTrackingColor", "10 132 255");
            c(r, "Scrollbar", "44 44 46");
            c(r, "GrayText", "140 140 150");
            c(r, "InfoWindow", "58 58 60");
            c(r, "InfoText", "235 235 245");
        }
        else {
            c(r, "ActiveBorder", "236 236 236");
            c(r, "ActiveTitle", "236 236 236");
            c(r, "GradientActiveTitle", "236 236 236");
            c(r, "TitleText", "60 60 60");
            c(r, "InactiveBorder", "246 246 246");
            c(r, "InactiveTitle", "246 246 246");
            c(r, "GradientInactiveTitle", "246 246 246");
            c(r, "InactiveTitleText", "150 150 150");
            c(r, "ButtonFace", "236 236 236");
            c(r, "ButtonAlternateFace", "246 246 246");
            c(r, "ButtonHilight", "255 255 255");
            c(r, "ButtonLight", "246 246 246");
            c(r, "ButtonShadow", "200 200 200");
            c(r, "ButtonDkShadow", "150 150 150");
            c(r, "ButtonText", "30 30 30");
            c(r, "Window", "255 255 255");
            c(r, "WindowText", "30 30 30");
            c(r, "WindowFrame", "200 200 200");
            c(r, "Menu", "246 246 246");
            c(r, "MenuBar", "246 246 246");
            c(r, "MenuText", "30 30 30");
            c(r, "MenuHilight", "0 122 255");
            c(r, "Hilight", "0 122 255");
            c(r, "HilightText", "255 255 255");
            c(r, "HotTrackingColor", "0 122 255");
            c(r, "Scrollbar", "246 246 246");
            c(r, "GrayText", "150 150 150");
            c(r, "InfoWindow", "255 255 255");
            c(r, "InfoText", "30 30 30");
        }
    }
}
