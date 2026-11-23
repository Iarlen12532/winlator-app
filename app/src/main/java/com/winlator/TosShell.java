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

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            handler.postDelayed(this, 20000);
        }
    };

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
            popup.getMenu().add(0, 2, 1, "Ajustes do Sistema…");
            popup.getMenu().add(0, 3, 2, "Sair do TOS");
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        Toast.makeText(activity, "TOS 0.1 • feito sobre o Winlator", Toast.LENGTH_LONG).show();
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

    private View dockItem(int type, int top, int bottom, final Runnable action) {
        final int size = TosPrefs.dockPx(activity);
        TosIconView v = new TosIconView(activity, type, top, bottom);
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
    }

    private void soon(String name) {
        Toast.makeText(activity, name + ": em construção", Toast.LENGTH_SHORT).show();
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
