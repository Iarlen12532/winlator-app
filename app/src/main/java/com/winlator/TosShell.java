package com.winlator;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.winlator.xserver.Window;
import com.winlator.xserver.WindowManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TosShell extends FrameLayout {
    public static final String TAG = "tos_shell";
    private static boolean registered = false;

    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("EEE HH:mm", Locale.getDefault());
    private FrameLayout windowLayer;
    private LinearLayout menuBar;
    private LinearLayout dock;
    private TextView clockView;
    private TextView batteryView;
    private FrameLayout cover;
    private ValueAnimator coverAnimator;
    private boolean coverHidden = false;
    private TosSettingsWindow settingsWindow;
    private TosControlCenter controlCenter;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            handler.postDelayed(this, 20000);
        }
    };

    // ------------------------------------------------------------------ registro

    public static void register(Application app) {
        if (registered) return;
        registered = true;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                if (activity instanceof XServerDisplayActivity) attach((XServerDisplayActivity) activity);
            }
            @Override public void onActivityCreated(Activity activity, Bundle state) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    private static void attach(XServerDisplayActivity activity) {
        FrameLayout root = activity.findViewById(R.id.FLXServerDisplay);
        if (root == null || root.findViewWithTag(TAG) != null) return;
        TosShell shell = new TosShell(activity);
        shell.setTag(TAG);
        root.addView(shell, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        shell.watchBoot(activity);
    }

    private static TosShell find(Activity activity) {
        View root = activity.findViewById(R.id.FLXServerDisplay);
        if (root == null) return null;
        View shell = root.findViewWithTag(TAG);
        return shell instanceof TosShell ? (TosShell) shell : null;
    }

    /** Usado pelo XServerDisplayActivity: true quando o mouse esta sobre a interface do TOS. */
    public static boolean isPointerOverUi(Activity activity, MotionEvent event) {
        TosShell shell = find(activity);
        if (shell == null) return false;
        boolean over = shell.isOverUi(event.getRawX(), event.getRawY());
        if (!over && event.getActionMasked() == MotionEvent.ACTION_BUTTON_PRESS) {
            View focus = activity.getCurrentFocus();
            if (focus != null) focus.clearFocus();
        }
        return over;
    }

    /** Usado pelo XServerDisplayActivity: true quando o teclado deve ir para um campo do TOS. */
    public static boolean wantsKeyboard(Activity activity) {
        if (find(activity) == null) return false;
        View focus = activity.getCurrentFocus();
        return focus instanceof EditText || focus instanceof WebView;
    }

    // ------------------------------------------------------------------ construcao

    public TosShell(Activity activity) {
        super(activity);
        this.activity = activity;
        windowLayer = new FrameLayout(activity);
        addView(windowLayer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        buildMenuBar();
        buildDock();
        buildCover();
        handler.postDelayed(() -> hideCover(), 25000);
    }

    private int px(float dp) {
        return TosPrefs.px(activity, dp);
    }

    private TextView label(String text, boolean bold) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(13));
        tv.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        tv.setPadding(px(8), 0, px(8), 0);
        return tv;
    }

    private void buildMenuBar() {
        menuBar = new LinearLayout(activity);
        menuBar.setOrientation(LinearLayout.HORIZONTAL);
        menuBar.setGravity(Gravity.CENTER_VERTICAL);
        menuBar.setPadding(px(10), 0, px(14), 0);
        menuBar.setBackgroundColor(Color.BLACK);
        menuBar.setClickable(true);

        final TextView logo = new TextView(activity);
        logo.setText("T");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(20));
        logo.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));
        logo.setPadding(px(10), 0, px(10), 0);
        logo.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(activity, logo);
            popup.getMenu().add(0, 1, 0, "Sobre o TOS");
            popup.getMenu().add(0, 2, 1, "Ajustes do Sistema...");
            popup.getMenu().add(0, 3, 2, "Sair do TOS");
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        Toast.makeText(activity, "TOS 0.1 - feito sobre o Winlator", Toast.LENGTH_LONG).show();
                        break;
                    case 2:
                        openSettings();
                        break;
                    case 3:
                        activity.finish();
                        break;
                }
                return true;
            });
            popup.show();
        });
        menuBar.addView(logo);
        menuBar.addView(label("TOS", true));
        menuBar.addView(label("Arquivo", false));
        menuBar.addView(label("Editar", false));
        menuBar.addView(label("Visualizar", false));
        menuBar.addView(label("Janela", false));
        menuBar.addView(label("Ajuda", false));

        menuBar.addView(new View(activity), new LinearLayout.LayoutParams(0, 1, 1f));

        TosGlyph cc = new TosGlyph(activity, TosGlyph.SLIDERS, Color.WHITE);
        cc.setOnClickListener(v -> toggleControlCenter());
        LinearLayout.LayoutParams ccp = new LinearLayout.LayoutParams(px(22), px(22));
        ccp.setMargins(px(10), 0, px(10), 0);
        menuBar.addView(cc, ccp);

        batteryView = label("", false);
        clockView = label("", false);
        menuBar.addView(batteryView);
        menuBar.addView(clockView);

        addView(menuBar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(30), Gravity.TOP));
    }

    private void buildDock() {
        dock = new LinearLayout(activity);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(px(10), px(8), px(10), px(8));
        dock.setClickable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x66202428);
        bg.setCornerRadius(px(24));
        bg.setStroke(Math.max(1, px(1)), 0x66FFFFFF);
        dock.setBackground(bg);

        dock.addView(dockItem(TosIconView.FINDER, 0xFF7CC8F5, 0xFF1C6FD1, () -> soon("Finder")));
        dock.addView(dockItem(TosIconView.LAUNCHPAD, 0xFFB4E3F8, 0xFF23A3E8, () -> soon("Launchpad")));
        dock.addView(glyphIcon(TosGlyph.COMPASS, 0xFF5AC8FA, 0xFF0A64D8, () -> openBrowser()));
        dock.addView(dockItem(TosIconView.INSTALL, 0xFF52B8EE, 0xFF033A72, () -> soon("Instalar aplicativos")));
        dock.addView(dockItem(TosIconView.SETTINGS, 0xFF9AA7B0, 0xFF4B5C66, () -> openSettings()));

        int size = TosPrefs.dockPx(activity);
        View divider = new View(activity);
        divider.setBackgroundColor(0x66FFFFFF);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Math.max(1, px(1)), (int) (size * 0.8f));
        dlp.setMargins(px(6), 0, px(6), 0);
        dock.addView(divider, dlp);

        dock.addView(dockItem(TosIconView.TRASH, 0xFFB4E3F8, 0xFF5AA9D6, () -> soon("Lixeira")));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = px(22);
        addView(dock, lp);
    }

    private View dockItem(int type, int top, int bottom, Runnable action) {
        TosIconView v = new TosIconView(activity, type, top, bottom);
        attachDockBehavior(v, action);
        return v;
    }

    private View glyphIcon(int glyph, int top, int bottom, Runnable action) {
        int size = TosPrefs.dockPx(activity);
        FrameLayout f = new FrameLayout(activity);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        bg.setCornerRadius(size * 0.23f);
        bg.setStroke(Math.max(1, px(1)), 0x55FFFFFF);
        f.setBackground(bg);
        int g = (int) (size * 0.62f);
        f.addView(new TosGlyph(activity, glyph, Color.WHITE), new FrameLayout.LayoutParams(g, g, Gravity.CENTER));
        attachDockBehavior(f, action);
        return f;
    }

    private void attachDockBehavior(View v, final Runnable action) {
        final int size = TosPrefs.dockPx(activity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(px(6), 0, px(6), 0);
        v.setLayoutParams(lp);
        v.setOnHoverListener((view, e) -> {
            if (e.getAction() == MotionEvent.ACTION_HOVER_ENTER) view.animate().scaleX(1.18f).scaleY(1.18f).setDuration(120).start();
            else if (e.getAction() == MotionEvent.ACTION_HOVER_EXIT) view.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            return false;
        });
        v.setOnClickListener(view -> {
            view.animate().translationY(-size * 0.3f).setDuration(140).withEndAction(() ->
                view.animate().translationY(0).setInterpolator(new BounceInterpolator()).setDuration(380).start()).start();
            action.run();
        });
    }

    public void applyScale() {
        removeView(menuBar);
        removeView(dock);
        buildMenuBar();
        buildDock();
        updateStatus();
        cover.bringToFront();
    }

    public int bottomInset() {
        return dock.getHeight() + px(22) + px(8);
    }

    private void soon(String name) {
        Toast.makeText(activity, name + ": em construcao", Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ mouse sobre a interface

    private boolean hit(View v, float x, float y) {
        if (v == null || v.getVisibility() != View.VISIBLE || v.getParent() == null) return false;
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        return x >= loc[0] && x <= loc[0] + v.getWidth() && y >= loc[1] && y <= loc[1] + v.getHeight();
    }

    public boolean isOverUi(float x, float y) {
        if (fullscreenView != null) return true;
        if (!coverHidden) return true;
        if (hit(menuBar, x, y) || hit(dock, x, y)) return true;
        for (int i = 0; i < windowLayer.getChildCount(); i++) {
            if (hit(windowLayer.getChildAt(i), x, y)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ janelas

    public void openWindow(TosWindow w) {
        int sw = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
        int sh = getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;
        int ww = (int) (sw * w.widthFrac);
        int hh = (int) (sh * w.heightFrac);
        int n = windowLayer.getChildCount() % 5;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ww, hh, Gravity.TOP | Gravity.LEFT);
        lp.leftMargin = (sw - ww) / 2 + px(24) * n;
        lp.topMargin = px(44) + px(24) * n;
        windowLayer.addView(w, lp);
        w.setAlpha(0f);
        w.setScaleX(0.9f);
        w.setScaleY(0.9f);
        w.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).start();
    }

    public void closeWindow(final TosWindow w) {
        w.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(160).withEndAction(() -> {
            windowLayer.removeView(w);
            w.onClosed();
        }).start();
    }

    public void openBrowser() {
        openWindow(new TosBrowserWindow(this));
    }

    public void openSettings() {
        if (settingsWindow != null && settingsWindow.getParent() != null) {
            settingsWindow.bringToFront();
            return;
        }
        settingsWindow = new TosSettingsWindow(this);
        int w = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
        int h = getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) (w * 0.62f), (int) (h * 0.66f), Gravity.CENTER);
        windowLayer.addView(settingsWindow, lp);
        settingsWindow.setAlpha(0f);
        settingsWindow.setScaleX(0.9f);
        settingsWindow.setScaleY(0.9f);
        settingsWindow.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).start();
    }

    public void closeSettings() {
        if (settingsWindow == null) return;
        final View w = settingsWindow;
        settingsWindow = null;
        w.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(160)
            .withEndAction(() -> windowLayer.removeView(w)).start();
    }

    public void toggleControlCenter() {
        if (controlCenter != null && controlCenter.getParent() != null) {
            closeControlCenter();
            return;
        }
        controlCenter = new TosControlCenter(this, activity);
        windowLayer.addView(controlCenter, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        controlCenter.setAlpha(0f);
        controlCenter.animate().alpha(1f).setDuration(160).start();
    }

    public void closeControlCenter() {
        if (controlCenter == null) return;
        final View v = controlCenter;
        controlCenter = null;
        v.animate().alpha(0f).setDuration(120).withEndAction(() -> windowLayer.removeView(v)).start();
    }

    // ------------------------------------------------------------------ tela cheia (ex.: video do YouTube)

    public void enterFullscreen(View view, WebChromeClient.CustomViewCallback callback) {
        exitFullscreen();
        fullscreenView = view;
        fullscreenCallback = callback;
        windowLayer.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        menuBar.setVisibility(View.GONE);
        dock.setVisibility(View.GONE);
    }

    public void exitFullscreen() {
        if (fullscreenView == null) return;
        windowLayer.removeView(fullscreenView);
        fullscreenView = null;
        if (fullscreenCallback != null) {
            fullscreenCallback.onCustomViewHidden();
            fullscreenCallback = null;
        }
        menuBar.setVisibility(View.VISIBLE);
        dock.setVisibility(View.VISIBLE);
    }

    // ------------------------------------------------------------------ tela de espera do boot

    private void buildCover() {
        cover = new FrameLayout(activity);
        cover.setBackgroundColor(Color.BLACK);
        cover.setClickable(true);

        FrameLayout track = new FrameLayout(activity);
        GradientDrawable tb = new GradientDrawable();
        tb.setColor(0x33FFFFFF);
        tb.setCornerRadius(px(3));
        track.setBackground(tb);

        final View segment = new View(activity);
        GradientDrawable sb = new GradientDrawable();
        sb.setColor(Color.WHITE);
        sb.setCornerRadius(px(3));
        segment.setBackground(sb);
        track.addView(segment, new FrameLayout.LayoutParams(px(70), px(5)));

        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(px(220), px(5), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        tlp.bottomMargin = px(140);
        cover.addView(track, tlp);

        coverAnimator = ValueAnimator.ofFloat(0f, 1f);
        coverAnimator.setDuration(900);
        coverAnimator.setRepeatCount(ValueAnimator.INFINITE);
        coverAnimator.setRepeatMode(ValueAnimator.REVERSE);
        coverAnimator.addUpdateListener(a -> segment.setTranslationX((Float) a.getAnimatedValue() * px(150)));
        coverAnimator.start();

        addView(cover, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void hideCover() {
        if (coverHidden) return;
        coverHidden = true;
        cover.animate().alpha(0f).setDuration(350).withEndAction(() -> {
            cover.setVisibility(View.GONE);
            if (coverAnimator != null) coverAnimator.cancel();
        }).start();
    }

    public void watchBoot(XServerDisplayActivity xActivity) {
        if (xActivity.getXServer() == null) {
            hideCover();
            return;
        }
        xActivity.getXServer().windowManager.addOnWindowModificationListener(new WindowManager.OnWindowModificationListener() {
            private boolean done = false;

            @Override
            public void onUpdateWindowContent(Window window) {}

            @Override
            public void onMapWindow(Window window) {
                if (!done && window.isRenderable() && !window.getClassName().isEmpty()) {
                    done = true;
                    TosShell.this.post(() -> hideCover());
                }
            }

            @Override
            public void onUnmapWindow(Window window) {}
        });
    }

    // ------------------------------------------------------------------ relogio e bateria

    private void updateStatus() {
        if (clockView == null || batteryView == null) return;
        clockView.setText(clockFormat.format(new Date()));
        Intent i = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i != null) {
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (level >= 0 && scale > 0) batteryView.setText((level * 100 / scale) + "%");
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(ticker);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        if (coverAnimator != null) coverAnimator.cancel();
        super.onDetachedFromWindow();
    }
}
