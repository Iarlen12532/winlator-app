package com.winlator;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class TosWindow extends LinearLayout {
    public final float widthFrac;
    public final float heightFrac;
    protected final TosShell shell;
    protected final Context ctx;
    protected final FrameLayout content;
    private boolean maximized = false;
    private int[] saved = new int[4];
    private float downX, downY;
    private int startA, startB;

    public TosWindow(TosShell shell, String title, float widthFrac, float heightFrac) {
        super(shell.getContext());
        this.shell = shell;
        this.ctx = shell.getContext();
        this.widthFrac = widthFrac;
        this.heightFrac = heightFrac;

        setOrientation(VERTICAL);
        setClickable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF2B2B2B);
        bg.setCornerRadius(px(16));
        setBackground(bg);
        setClipToOutline(true);
        setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        setElevation(px(24));

        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(px(14), 0, px(14), 0);
        bar.setBackgroundColor(0xFF5E6366);
        bar.addView(dot(0xFFFF5F56, () -> close()));
        bar.addView(dot(0xFFFFBD2E, () -> close()));
        bar.addView(dot(0xFF27C93F, () -> toggleMaximize()));

        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextColor(0xCCFFFFFF);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(13));
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        bar.addView(t, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(new View(ctx), new LayoutParams(px(60), 1));

        bar.setOnTouchListener((v, e) -> {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            if (lp == null) return true;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getRawX();
                    downY = e.getRawY();
                    startA = lp.leftMargin;
                    startB = lp.topMargin;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (maximized) return true;
                    lp.leftMargin = startA + (int) (e.getRawX() - downX);
                    lp.topMargin = Math.max(px(30), startB + (int) (e.getRawY() - downY));
                    setLayoutParams(lp);
                    return true;
            }
            return true;
        });
        addView(bar, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(38)));

        content = new FrameLayout(ctx);
        addView(content, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    protected int px(float dp) {
        return TosPrefs.px(getContext(), dp);
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

    /** Chame no fim do construtor da janela filha, para o canto de redimensionar ficar por cima. */
    protected void finishLayout() {
        View handle = new View(ctx);
        handle.setOnTouchListener((v, e) -> {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            if (lp == null || maximized) return true;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getRawX();
                    downY = e.getRawY();
                    startA = lp.width;
                    startB = lp.height;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    lp.width = Math.max(px(360), startA + (int) (e.getRawX() - downX));
                    lp.height = Math.max(px(240), startB + (int) (e.getRawY() - downY));
                    setLayoutParams(lp);
                    return true;
            }
            return true;
        });
        content.addView(handle, new FrameLayout.LayoutParams(px(26), px(26), Gravity.BOTTOM | Gravity.END));
    }

    public void toggleMaximize() {
        ViewGroup p = (ViewGroup) getParent();
        if (p == null) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (!maximized) {
            saved = new int[]{lp.leftMargin, lp.topMargin, lp.width, lp.height};
            lp.leftMargin = px(8);
            lp.topMargin = px(34);
            lp.width = p.getWidth() - px(16);
            lp.height = p.getHeight() - px(34) - shell.bottomInset();
            maximized = true;
        }
        else {
            lp.leftMargin = saved[0];
            lp.topMargin = saved[1];
            lp.width = saved[2];
            lp.height = saved[3];
            maximized = false;
        }
        setLayoutParams(lp);
    }

    public void close() {
        shell.closeWindow(this);
    }

    /** Chamado quando a janela \u00e9 fechada. */
    public void onClosed() {}

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) post(() -> bringToFront());
        return false;
    }
}
