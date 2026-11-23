package com.winlator;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class TosSettingsWindow extends LinearLayout {
    private interface IntCb { void on(int value); }

    private final TosShell shell;
    private final Context ctx;
    private final String[] names = {"Geral", "Teclado e Mouse", "Tela e Resolução", "Drives", "Estatísticas"};
    private final TextView[] pills = new TextView[5];
    private final ScrollView scroll;
    private boolean large = false;
    private float downX, downY, startTx, startTy;

    public TosSettingsWindow(TosShell shell) {
        super(shell.getContext());
        this.shell = shell;
        this.ctx = shell.getContext();
        setOrientation(VERTICAL);
        setClickable(true);
        setBackground(round(0xFF3A3A3A, px(20)));
        setClipToOutline(true);
        setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        setElevation(px(24));

        LinearLayout title = new LinearLayout(ctx);
        title.setOrientation(HORIZONTAL);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(px(16), 0, px(16), 0);
        title.setBackgroundColor(0xFF5E6366);
        title.addView(dot(0xFFFF5F56, () -> shell.closeSettings()));
        title.addView(dot(0xFFFFBD2E, () -> shell.closeSettings()));
        title.addView(dot(0xFF27C93F, () -> toggleSize()));
        title.addView(text("Ajustes do Sistema", 13, 0xCCFFFFFF, false));
        title.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getRawX();
                    downY = e.getRawY();
                    startTx = getTranslationX();
                    startTy = getTranslationY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    setTranslationX(startTx + e.getRawX() - downX);
                    setTranslationY(startTy + e.getRawY() - downY);
                    return true;
            }
            return true;
        });
        addView(title, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(38)));

        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(HORIZONTAL);

        LinearLayout sidebar = new LinearLayout(ctx);
        sidebar.setOrientation(VERTICAL);
        sidebar.setBackgroundColor(0xFF2E2E2E);
        sidebar.setPadding(px(12), px(16), px(12), px(12));
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView pill = text(names[i], 15, Color.WHITE, false);
            pill.setGravity(Gravity.CENTER_VERTICAL);
            pill.setPadding(px(18), 0, px(12), 0);
            pill.setOnClickListener(v -> select(index));
            LayoutParams plp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(46));
            plp.bottomMargin = px(8);
            sidebar.addView(pill, plp);
            pills[i] = pill;
        }
        body.addView(sidebar, new LayoutParams(px(230), ViewGroup.LayoutParams.MATCH_PARENT));

        scroll = new ScrollView(ctx);
        scroll.setBackgroundColor(0xFF454545);
        body.addView(scroll, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        addView(body, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        select(0);
    }

    private int px(float dp) { return TosPrefs.px(ctx, dp); }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
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

    private View dot(int color, final Runnable action) {
        View v = new View(ctx);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        v.setBackground(g);
        LayoutParams lp = new LayoutParams(px(13), px(13));
        lp.rightMargin = px(8);
        v.setLayoutParams(lp);
        v.setOnClickListener(x -> action.run());
        return v;
    }

    private void toggleSize() {
        ViewGroup p = (ViewGroup) getParent();
        if (p == null) return;
        large = !large;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        lp.width = (int) (p.getWidth() * (large ? 0.92f : 0.62f));
        lp.height = (int) (p.getHeight() * (large ? 0.88f : 0.66f));
        setLayoutParams(lp);
        setTranslationX(0);
        setTranslationY(0);
    }

    private void select(int index) {
        for (int i = 0; i < pills.length; i++) {
            pills[i].setBackground(round(i == index ? 0xFF787878 : 0xFF565656, px(23)));
        }
        LinearLayout pane;
        switch (index) {
            case 0: pane = buildGeral(); break;
            case 1: pane = buildMouse(); break;
            case 2: pane = buildTela(); break;
            case 3: pane = buildDrives(); break;
            default: pane = buildStats(); break;
        }
        scroll.removeAllViews();
        scroll.addView(pane, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private LinearLayout newPane() {
        LinearLayout pane = new LinearLayout(ctx);
        pane.setOrientation(VERTICAL);
        pane.setPadding(px(20), px(20), px(20), px(20));
        return pane;
    }

    private LinearLayout panel(LinearLayout pane) {
        LinearLayout p = new LinearLayout(ctx);
        p.setOrientation(VERTICAL);
        p.setBackground(round(0xFF333333, px(18)));
        p.setPadding(px(18), px(12), px(18), px(12));
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = px(14);
        pane.addView(p, lp);
        return p;
    }

    private void header(LinearLayout pane, String title, String desc) {
        LinearLayout h = panel(pane);
        h.setGravity(Gravity.CENTER_HORIZONTAL);
        h.setPadding(px(18), px(18), px(18), px(18));
        TosIconView icon = new TosIconView(ctx, TosIconView.SETTINGS, 0xFF9AA7B0, 0xFF4B5C66);
        h.addView(icon, new LayoutParams(px(64), px(64)));
        TextView t = text(title, 28, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        h.addView(t);
        TextView d = text(desc, 14, 0xFFBBBBBB, false);
        d.setGravity(Gravity.CENTER);
        h.addView(d);
    }

    private void addRow(LinearLayout panel, String label, String value) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(HORIZONTAL);
        row.setPadding(0, px(9), 0, px(9));
        row.addView(text(label, 15, Color.WHITE, false), new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = text(value, 15, 0xFFBBBBBB, false);
        v.setGravity(Gravity.END);
        row.addView(v);
        panel.addView(row);
    }

    private void addSlider(LinearLayout panel, final String label, final int min, int max, int value, final String unit, final IntCb cb) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(VERTICAL);
        row.setPadding(0, px(8), 0, px(8));
        final TextView t = text(label + ": " + value + unit, 15, Color.WHITE, false);
        row.addView(t);
        SeekBar sb = new SeekBar(ctx);
        sb.setMax(max - min);
        sb.setProgress(value - min);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int v = progress + min;
                t.setText(label + ": " + v + unit);
                if (fromUser) cb.on(v);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        row.addView(sb, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(row);
    }

    private String screenText() {
        DisplayMetrics dm = new DisplayMetrics();
        ((android.app.Activity) ((ViewGroup) shell.getParent()).getContext()).getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        return Math.max(dm.widthPixels, dm.heightPixels) + " x " + Math.min(dm.widthPixels, dm.heightPixels);
    }

    private ActivityManager.MemoryInfo memInfo() {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi;
    }

    private LinearLayout buildGeral() {
        LinearLayout pane = newPane();
        header(pane, "Geral", "Informações do sistema, versão e mais.");
        LinearLayout p = panel(pane);
        addRow(p, "Sobre este computador", Build.MANUFACTURER + " " + Build.MODEL);
        addRow(p, "Sistema base", "Android " + Build.VERSION.RELEASE);
        addRow(p, "Tela", screenText());
        addRow(p, "Atualizações", "TOS 0.1 (base Winlator)");
        addRow(p, "Senha e segurança", "em breve");
        return pane;
    }

    private LinearLayout buildMouse() {
        LinearLayout pane = newPane();
        header(pane, "Teclado e Mouse", "Velocidade, tamanho do cursor e captura do mouse.");
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        LinearLayout p = panel(pane);
        addSlider(p, "Velocidade do cursor", 10, 200, Math.round(prefs.getFloat("cursor_speed", 1.0f) * 100), "%",
            v -> prefs.edit().putFloat("cursor_speed", v / 100f).apply());
        addSlider(p, "Tamanho do cursor", 50, 300, Math.round(prefs.getFloat("cursor_scale", 1.0f) * 100), "%",
            v -> prefs.edit().putFloat("cursor_scale", v / 100f).apply());

        CheckBox cb = new CheckBox(ctx);
        cb.setText("Capturar ponteiro do mouse externo (use em jogos; desmarque para clicar no Dock e nas janelas do TOS)");
        cb.setTextColor(Color.WHITE);
        cb.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(14));
        cb.setChecked(prefs.getBoolean("capture_pointer_on_external_mouse", true));
        cb.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean("capture_pointer_on_external_mouse", checked).apply());
        p.addView(cb);
        p.addView(text("As mudanças desta página valem na próxima vez que o TOS abrir.", 13, 0xFF9A9A9A, false));
        return pane;
    }

    private LinearLayout buildTela() {
        LinearLayout pane = newPane();
        header(pane, "Tela e Resolução", "Tamanho dos ícones, da barra e dos textos do TOS.");
        LinearLayout p = panel(pane);
        addSlider(p, "Tamanho dos ícones do Dock", 40, 110, TosPrefs.dockIconDp(ctx), " dp", v -> {
            TosPrefs.setDockIconDp(ctx, v);
            shell.applyScale();
        });
        addSlider(p, "Tamanho da interface (barra e textos)", 80, 200, TosPrefs.uiPercent(ctx), "%", v -> {
            TosPrefs.setUiPercent(ctx, v);
            shell.applyScale();
        });
        LinearLayout r = panel(pane);
        addRow(r, "Resolução do sistema", screenText());
        r.addView(text("O TOS usa a resolução nativa do tablet.", 13, 0xFF9A9A9A, false));
        return pane;
    }

    private LinearLayout buildDrives() {
        LinearLayout pane = newPane();
        header(pane, "Drives", "Unidades visíveis para os programas do Windows.");
        LinearLayout p = panel(pane);
        addRow(p, "C:", "Disco do sistema (Wine)");
        addRow(p, "Z:", "Arquivos do Android");
        return pane;
    }

    private LinearLayout buildStats() {
        LinearLayout pane = newPane();
        header(pane, "Estatísticas", "Uso de memória e bateria.");
        LinearLayout p = panel(pane);
        ActivityManager.MemoryInfo mi = memInfo();
        long totalMb = mi.totalMem / (1024 * 1024);
        long usedMb = (mi.totalMem - mi.availMem) / (1024 * 1024);
        addRow(p, "Memória em uso", usedMb + " MB de " + totalMb + " MB");
        Intent i = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i != null) {
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (level >= 0 && scale > 0) addRow(p, "Bateria", (level * 100 / scale) + "%");
        }
        return pane;
    }
}
