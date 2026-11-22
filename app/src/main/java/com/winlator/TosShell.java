package com.winlator;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
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
    private TextView clockView;
    private TextView batteryView;
    private FrameLayout cover;
    private ValueAnimator coverAnimator;
    private boolean coverHidden = false;

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
        buildMenuBar();
        buildDock();
        buildCover();
        handler.postDelayed(() -> hideCover(), 25000);
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String text, boolean bold) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(13);
        tv.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        tv.setPadding(dp(8), 0, dp(8), 0);
        return tv;
    }

    private void buildMenuBar() {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), 0, dp(14), 0);
        bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xCC001B3A, 0x66001B3A}));

        final TextView logo = new TextView(activity);
        logo.setText("T");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(20);
        logo.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));
        logo.setPadding(dp(10), 0, dp(10), 0);
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
        bar.addView(logo);
        bar.addView(label("TOS", true));
        bar.addView(label("Arquivo", false));
        bar.addView(label("Editar", false));
        bar.addView(label("Visualizar", false));
        bar.addView(label("Janela", false));
        bar.addView(label("Ajuda", false));

        View spacer = new View(activity);
        bar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        batteryView = label("", false);
        clockView = label("", false);
        bar.addView(batteryView);
        bar.addView(clockView);

        addView(bar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30), Gravity.TOP));
    }

    private void buildDock() {
        LinearLayout dock = new LinearLayout(activity);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x38FFFFFF);
        bg.setCornerRadius(dp(24));
        bg.setStroke(dp(1), 0x66FFFFFF);
        dock.setBackground(bg);

        dock.addView(dockItem("🗂", 0xFF52B8EE, 0xFF056FB7, () -> soon("Finder")));
        dock.addView(dockItem("🚀", 0xFFB4E3F8, 0xFF23A3E8, () -> soon("Launchpad")));
        dock.addView(dockItem("⬇", 0xFF23A3E8, 0xFF033A72, () -> soon("Instalar aplicativos")));
        dock.addView(dockItem("⚙", 0xFF8FA3AE, 0xFF4B5C66, () -> openSettings()));

        View divider = new View(activity);
        divider.setBackgroundColor(0x66FFFFFF);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(1), dp(44));
        dlp.setMargins(dp(6), 0, dp(6), 0);
        dock.addView(divider, dlp);

        dock.addView(dockItem("🗑", 0xFFB4E3F8, 0xFF84CEF3, () -> soon("Lixeira")));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = dp(22);
        addView(dock, lp);
    }

    private View dockItem(String glyph, int top, int bottom, final Runnable action) {
        TextView v = new TextView(activity);
        v.setText(glyph);
        v.setTextSize(26);
        v.setGravity(Gravity.CENTER);
        v.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0x55FFFFFF);
        v.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(56), dp(56));
        lp.setMargins(dp(6), 0, dp(6), 0);
        v.setLayoutParams(lp);
        v.setOnClickListener(view -> {
            view.animate().translationY(-dp(18)).setDuration(140).withEndAction(() ->
                view.animate().translationY(0).setInterpolator(new BounceInterpolator()).setDuration(380).start()).start();
            action.run();
        });
        return v;
    }

    private void soon(String name) {
        Toast.makeText(activity, name + ": em construção", Toast.LENGTH_SHORT).show();
    }

    private void openSettings() {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("selected_menu_item_id", R.id.menu_item_settings);
        activity.startActivity(intent);
    }

    private void buildCover() {
        cover = new FrameLayout(activity);
        cover.setBackgroundColor(Color.BLACK);
        cover.setClickable(true);

        FrameLayout track = new FrameLayout(activity);
        GradientDrawable tb = new GradientDrawable();
        tb.setColor(0x33FFFFFF);
        tb.setCornerRadius(dp(3));
        track.setBackground(tb);

        final View segment = new View(activity);
        GradientDrawable sb = new GradientDrawable();
        sb.setColor(Color.WHITE);
        sb.setCornerRadius(dp(3));
        segment.setBackground(sb);
        track.addView(segment, new FrameLayout.LayoutParams(dp(70), dp(5)));

        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(dp(220), dp(5), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        tlp.bottomMargin = dp(140);
        cover.addView(track, tlp);

        coverAnimator = ValueAnimator.ofFloat(0f, 1f);
        coverAnimator.setDuration(900);
        coverAnimator.setRepeatCount(ValueAnimator.INFINITE);
        coverAnimator.setRepeatMode(ValueAnimator.REVERSE);
        coverAnimator.addUpdateListener(a -> segment.setTranslationX((Float) a.getAnimatedValue() * dp(150)));
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
