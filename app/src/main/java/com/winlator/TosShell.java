package com.winlator;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
import android.view.ViewParent;
import android.view.animation.BounceInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.winlator.xserver.Window;
import com.winlator.xserver.WindowManager;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class TosShell extends FrameLayout {
    public static final String TAG = "tos_shell";
    private static boolean registered = false;
    private static final HashMap<String, Bitmap> ICONS = new HashMap<>();

    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("EEE HH:mm", Locale.getDefault());
    private FrameLayout windowLayer;
    private LinearLayout menuBar;
    private LinearLayout dock;
    private TextView clockView;
    private TextView batteryView;
    private FrameLayout cover;
    private CursorView cursor;
    private ValueAnimator coverAnimator;
    private boolean coverHidden = false;
    private TosSettingsWindow settingsWindow;
    private TosControlCenter controlCenter;
    private FrameLayout tMenu;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            handler.postDelayed(this, 20000);
        }
    };

    // ---------------------------------------------------------------- registro

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

    public static boolean wantsKeyboard(Activity activity) {
        if (find(activity) == null) return false;
        View focus = activity.getCurrentFocus();
        return focus instanceof EditText || focus instanceof WebView;
    }

    // ---------------------------------------------------------------- construcao

    public TosShell(Activity activity) {
        super(activity);
        this.activity = activity;
        windowLayer = new FrameLayout(activity);
        addView(windowLayer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        buildMenuBar();
        buildDock();
        buildCover();
        cursor = new CursorView(activity);
        cursor.setVisibility(View.GONE);
        addView(cursor, new FrameLayout.LayoutParams(px(18), px(26)));
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

        TextView logo = new TextView(activity);
        logo.setText("T");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(20));
        logo.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));
        logo.setPadding(px(10), 0, px(10), 0);
        logo.setOnClickListener(v -> toggleTMenu());
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

        dock.addView(imageIcon("tos_finder.png", () -> soon("Finder")));
        dock.addView(imageIcon("tos_launchpad.png", () -> soon("Launchpad")));
        dock.addView(imageIcon("tos_chrome.png", () -> openBrowser()));
        dock.addView(imageIcon("tos_settings.png", () -> openSettings()));

        int size = TosPrefs.dockPx(activity);
        View divider = new View(activity);
        divider.setBackgroundColor(0x66FFFFFF);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Math.max(1, px(1)), (int) (size * 0.8f));
        dlp.setMargins(px(6), 0, px(6), 0);
        dock.addView(divider, dlp);

        dock.addView(imageIcon("tos_downloads.png", () -> soon("Downloads")));
        dock.addView(imageIcon("tos_trash.png", () -> soon("Lixeira")));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = px(22);
        addView(dock, lp);
    }

    private Bitmap icon(String file) {
        Bitmap b = ICONS.get(file);
        if (b == null) {
            try (InputStream in = activity.getAssets().open(file)) {
                b = BitmapFactory.decodeStream(in);
            }
            catch (IOException e) {
                b = null;
            }
            if (b != null) ICONS.put(file, b);
        }
        return b;
    }

    private View imageIcon(String file, final Runnable action) {
        ImageView v = new ImageView(activity);
        Bitmap b = icon(file);
        if (b != null) v.setImageBitmap(b);
        else v.setBackgroundColor(0x55FFFFFF);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);

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
        return v;
    }

    public void applyScale() {
        removeView(menuBar);
        removeView(dock);
        buildMenuBar();
        buildDock();
        updateStatus();
        cover.bringToFront();
        cursor.bringToFront();
    }

    public int bottomInset() {
        return dock.getHeight() + px(22) + px(8);
    }

    private void soon(String name) {
        Toast.makeText(activity, name + ": em construcao", Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------- cursor e deteccao da interface

    private static class CursorView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        CursorView(Context context) {
            super(context);
            fill.setColor(Color.WHITE);
            line.setColor(Color.BLACK);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected void onDraw(Canvas c) {
            float s = getHeight() / 20f;
            line.setStrokeWidth(Math.max(1.5f, s * 1.2f));
            path.reset();
            path.moveTo(1 * s, 1 * s);
            path.lineTo(1 * s, 16 * s);
            path.lineTo(5 * s, 12.5f * s);
            path.lineTo(8 * s, 19 * s);
            path.lineTo(10.5f * s, 18 * s);
            path.lineTo(7.5f * s, 11.5f * s);
            path.lineTo(12.5f * s, 11.5f * s);
            path.close();
            c.drawPath(path, fill);
            c.drawPath(path, line);
        }
    }

    public void setCursor(boolean visible, float x, float y) {
        if (visible) {
            cursor.setVisibility(View.VISIBLE);
            cursor.setTranslationX(x);
            cursor.setTranslationY(y);
        }
        else cursor.setVisibility(View.GONE);
    }

    private boolean hitLocal(View v, float x, float y) {
        if (v == null || v.getVisibility() != View.VISIBLE || v.getParent() == null) return false;
        float left = 0;
        float top = 0;
        View cur = v;
        while (cur != null && cur != this) {
            left += cur.getLeft() + cur.getTranslationX();
            top += cur.getTop() + cur.getTranslationY();
            ViewParent p = cur.getParent();
            cur = (p instanceof View) ? (View) p : null;
        }
        return x >= left && x <= left + v.getWidth() && y >= top && y <= top + v.getHeight();
    }

    public boolean isOverUiLocal(float x, float y) {
        if (fullscreenView != null) return true;
        if (!coverHidden) return true;
        if (hitLocal(menuBar, x, y) || hitLocal(dock, x, y)) return true;
        for (int i = 0; i < windowLayer.getChildCount(); i++) {
            if (hitLocal(windowLayer.getChildAt(i), x, y)) return true;
        }
        return false;
    }

    public boolean isOverUi(float rawX, float rawY) {
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        return isOverUiLocal(rawX - loc[0], rawY - loc[1]);
    }

    // ---------------------------------------------------------------- menu do T

    private View menuItem(String text, final Runnable action) {
        TextView t = new TextView(activity);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(15));
        t.setPadding(px(14), px(10), px(14), px(10));
        t.setOnHoverListener((view, e) -> {
            if (e.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                GradientDrawable h = new GradientDrawable();
                h.setColor(0x883B82F6);
                h.setCornerRadius(px(8));
                view.setBackground(h);
            }
            else if (e.getAction() == MotionEvent.ACTION_HOVER_EXIT) view.setBackground(null);
            return false;
        });
        t.setOnClickListener(v -> {
            closeTMenu();
            action.run();
        });
        return t;
    }

    private void toggleTMenu() {
        if (tMenu != null && tMenu.getParent() != null) {
            closeTMenu();
            return;
        }
        FrameLayout scrim = new FrameLayout(activity);
        scrim.setClickable(true);
        scrim.setOnClickListener(v -> closeTMenu());

        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(px(6), px(6), px(6), px(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF0202428);
        bg.setCornerRadius(px(12));
        bg.setStroke(Math.max(1, px(1)), 0x66FFFFFF);
        panel.setBackground(bg);
        panel.setClickable(true);

        panel.addView(menuItem("Sobre o TOS", () ->
            Toast.makeText(activity, "TOS 0.1 - feito sobre o Winlator", Toast.LENGTH_LONG).show()));
        panel.addView(menuItem("Ajustes do Sistema...", () -> openSettings()));
        panel.addView(menuItem("Sair do TOS", () -> activity.finish()));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(240), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT);
        lp.leftMargin = px(10);
        lp.topMargin = px(34);
        scrim.addView(panel, lp);

        tMenu = scrim;
        windowLayer.addView(scrim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void closeTMenu() {
        if (tMenu == null) return;
        windowLayer.removeView(tMenu);
        tMenu = null;
    }

    // ---------------------------------------------------------------- janelas

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

    // ---------------------------------------------------------------- tela cheia

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

    // ---------------------------------------------------------------- tela de espera do boot

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
        TosPointerGate.attach(this, xActivity.getXServer());
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

    // ---------------------------------------------------------------- relogio e bateria

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
