package com.winlator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class TosControlCenter extends FrameLayout {
    private interface IntCb { void on(int value); }

    private final TosShell shell;
    private final Activity activity;
    private final Context ctx;

    public TosControlCenter(final TosShell shell, final Activity activity) {
        super(activity);
        this.shell = shell;
        this.activity = activity;
        this.ctx = activity;
        setClickable(true);
        setOnClickListener(v -> shell.closeControlCenter());

        LinearLayout panel = new LinearLayout(ctx);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(px(12), px(12), px(12), px(12));
        panel.setBackground(glass(px(30), 0xEE0B1F4A));
        panel.setClickable(true);

        // Linha A: Wi-Fi + Bluetooth/Mouse | M\u00eddia
        LinearLayout rowA = new LinearLayout(ctx);
        rowA.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout leftCol = new LinearLayout(ctx);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        leftCol.addView(wideTile(TosGlyph.WIFI, "Wi-Fi", "Rede", () -> openWifi()),
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(68)));

        LinearLayout btRow = new LinearLayout(ctx);
        btRow.setOrientation(LinearLayout.HORIZONTAL);
        btRow.addView(circleTile(TosGlyph.BLUETOOTH, true, () -> openBluetooth(), null));
        btRow.addView(new View(ctx), new LinearLayout.LayoutParams(0, 1, 1f));
        btRow.addView(circleTile(TosGlyph.MOUSE, isCaptureOn(), null, () -> toggleCapture()));
        LinearLayout.LayoutParams brp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brp.topMargin = px(8);
        leftCol.addView(btRow, brp);
        rowA.addView(leftCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        mp.leftMargin = px(8);
        rowA.addView(mediaCard(), mp);
        panel.addView(rowA, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Linha B: Ajustes + Navegador
        LinearLayout rowB = new LinearLayout(ctx);
        rowB.setOrientation(LinearLayout.HORIZONTAL);
        rowB.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, px(68), 1f);
        rowB.addView(wideTile(TosGlyph.GEAR, "Ajustes", "Sistema", () -> {
            shell.closeControlCenter();
            shell.openSettings();
        }), wp);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(px(68), px(68));
        cp.leftMargin = px(8);
        rowB.addView(circleTile(TosGlyph.COMPASS, false, () -> {
            shell.closeControlCenter();
            shell.openBrowser();
        }, null), cp);
        LinearLayout.LayoutParams rbp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rbp.topMargin = px(8);
        panel.addView(rowB, rbp);

        // Brilho
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = px(8);
        panel.addView(sliderCard("Tela", TosGlyph.SUN, 5, 100, currentBrightness(), v -> setBrightness(v)), sp);

        // Som
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        final int max = Math.max(1, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int cur = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        panel.addView(sliderCard("Som", TosGlyph.SPEAKER, 0, max, cur, v -> {
            AudioManager a = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            a.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0);
        }), sp);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(360), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        lp.topMargin = px(36);
        lp.rightMargin = px(10);
        addView(panel, lp);
    }

    private int px(float dp) {
        return TosPrefs.px(ctx, dp);
    }

    private GradientDrawable glass(int radius, int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        g.setStroke(Math.max(1, px(1)), 0x55FFFFFF);
        return g;
    }

    private TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private View circleTile(int glyph, boolean on, final Runnable action, final Runnable toggle) {
        final FrameLayout f = new FrameLayout(ctx);
        final TosGlyph g = new TosGlyph(ctx, glyph, on ? 0xFF2F7BF6 : Color.WHITE);
        f.setBackground(circle(on ? 0xFFFFFFFF : 0x33FFFFFF));
        f.addView(g, new FrameLayout.LayoutParams(px(34), px(34), Gravity.CENTER));
        f.setLayoutParams(new LinearLayout.LayoutParams(px(68), px(68)));
        final boolean[] state = {on};
        f.setOnClickListener(v -> {
            if (toggle != null) {
                state[0] = !state[0];
                f.setBackground(circle(state[0] ? 0xFFFFFFFF : 0x33FFFFFF));
                g.setColor(state[0] ? 0xFF2F7BF6 : Color.WHITE);
                toggle.run();
            }
            if (action != null) action.run();
        });
        return f;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        d.setStroke(Math.max(1, px(1)), 0x55FFFFFF);
        return d;
    }

    private View wideTile(int glyph, String title, String sub, final Runnable action) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(px(14), 0, px(14), 0);
        row.setBackground(glass(px(34), 0x33FFFFFF));

        FrameLayout icon = new FrameLayout(ctx);
        icon.setBackground(circle(0xFFFFFFFF));
        icon.addView(new TosGlyph(ctx, glyph, 0xFF2F7BF6), new FrameLayout.LayoutParams(px(26), px(26), Gravity.CENTER));
        row.addView(icon, new LinearLayout.LayoutParams(px(42), px(42)));

        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(title, 15, Color.WHITE, true));
        texts.addView(text(sub, 12, 0xAAFFFFFF, false));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.leftMargin = px(10);
        row.addView(texts, tp);
        row.setOnClickListener(v -> action.run());
        return row;
    }

    private View mediaCard() {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(px(10), px(10), px(10), px(10));
        card.setBackground(glass(px(30), 0x33FFFFFF));

        TextView title = text("M\u00eddia", 15, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title);
        TextView sub = text("Controle o que est\u00e1 tocando", 11, 0xAAFFFFFF, false);
        sub.setGravity(Gravity.CENTER);
        card.addView(sub);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.addView(mediaButton(TosGlyph.PREV, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        row.addView(mediaButton(TosGlyph.PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        row.addView(mediaButton(TosGlyph.NEXT, KeyEvent.KEYCODE_MEDIA_NEXT));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = px(14);
        card.addView(row, rp);
        return card;
    }

    private View mediaButton(int glyph, final int keyCode) {
        TosGlyph g = new TosGlyph(ctx, glyph, Color.WHITE);
        g.setLayoutParams(new LinearLayout.LayoutParams(px(34), px(34)));
        g.setPadding(px(4), px(4), px(4), px(4));
        g.setOnClickListener(v -> {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        });
        return g;
    }

    private View sliderCard(String title, int glyph, int min, int max, int value, final IntCb cb) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(px(16), px(10), px(16), px(10));
        card.setBackground(glass(px(30), 0x33FFFFFF));
        card.addView(text(title, 14, Color.WHITE, true));

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(new TosGlyph(ctx, glyph, Color.WHITE), new LinearLayout.LayoutParams(px(22), px(22)));

        final int lo = min;
        SeekBar sb = new SeekBar(ctx);
        sb.setMax(max - min);
        sb.setProgress(Math.max(0, Math.min(max - min, value - min)));
        sb.setProgressTintList(ColorStateList.valueOf(Color.WHITE));
        sb.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) cb.on(progress + lo);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        row.addView(sb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        return card;
    }

    private int currentBrightness() {
        float b = activity.getWindow().getAttributes().screenBrightness;
        if (b >= 0f) return Math.max(5, Math.round(b * 100f));
        try {
            return Math.max(5, Settings.System.getInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) * 100 / 255);
        }
        catch (Settings.SettingNotFoundException e) {
            return 60;
        }
    }

    private void setBrightness(int percent) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.screenBrightness = Math.max(0.05f, percent / 100f);
        activity.getWindow().setAttributes(lp);
    }

    private boolean isCaptureOn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean("capture_pointer_on_external_mouse", true);
    }

    private void toggleCapture() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean now = !prefs.getBoolean("capture_pointer_on_external_mouse", true);
        prefs.edit().putBoolean("capture_pointer_on_external_mouse", now).apply();
        Toast.makeText(ctx, now ? "Mouse capturado (modo jogo). Vale ao reabrir o TOS."
            : "Mouse livre (modo desktop). Vale ao reabrir o TOS.", Toast.LENGTH_LONG).show();
    }

    private void openWifi() {
        Intent i = Build.VERSION.SDK_INT >= 29
            ? new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            : new Intent(Settings.ACTION_WIFI_SETTINGS);
        try {
            activity.startActivity(i);
        }
        catch (Exception e) {
            Toast.makeText(ctx, "N\u00e3o foi poss\u00edvel abrir o Wi-Fi", Toast.LENGTH_SHORT).show();
        }
    }

    private void openBluetooth() {
        try {
            activity.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        }
        catch (Exception e) {
            Toast.makeText(ctx, "N\u00e3o foi poss\u00edvel abrir o Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }
}
