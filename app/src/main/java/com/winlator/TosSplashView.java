package com.winlator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class TosSplashView extends View {
    private static final long DURATION_MS = 2500;
    private static final int STEPS = 220;
    private static final float[][] STROKES = {
        {205, 340, 390, 235, 650, 245, 830, 305, 30, 64},
        {540, 255, 440, 430, 610, 610, 505, 790, 74, 50},
        {505, 790, 470, 860, 360, 880, 322, 810, 50, 26},
        {322, 810, 298, 765, 345, 735, 372, 765, 26, 18}
    };

    private final float[] xs = new float[STROKES.length * (STEPS + 1)];
    private final float[] ys = new float[xs.length];
    private final float[] rs = new float[xs.length];
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float progress = 0;
    private boolean cancelled = false;
    private ValueAnimator animator;

    public TosSplashView(Context context) {
        super(context);
        dotPaint.setColor(Color.WHITE);
        int k = 0;
        for (float[] s : STROKES) {
            for (int i = 0; i <= STEPS; i++) {
                float t = i / (float) STEPS;
                float u = 1 - t;
                xs[k] = u*u*u*s[0] + 3*u*u*t*s[2] + 3*u*t*t*s[4] + t*t*t*s[6];
                ys[k] = u*u*u*s[1] + 3*u*u*t*s[3] + 3*u*t*t*s[5] + t*t*t*s[7];
                rs[k] = (s[8] + (s[9] - s[8]) * t) / 2f;
                k++;
            }
        }
    }

    public void start(final Runnable onEnd) {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                progress = (Float) a.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                if (!cancelled) onEnd.run();
            }
        });
        animator.start();
    }

    public void stop() {
        cancelled = true;
        if (animator != null) animator.cancel();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        int w = getWidth(), h = getHeight();
        float size = Math.min(w, h) * 0.34f;
        float scale = size / 1024f;
        float left = (w - size) / 2f;
        float top = h * 0.5f - size * 0.62f;

        int count = (int) (progress * xs.length);
        for (int i = 0; i < count; i++) {
            canvas.drawCircle(left + xs[i] * scale, top + ys[i] * scale, rs[i] * scale, dotPaint);
        }

        float barW = size * 0.9f;
        float barH = Math.max(6f, size * 0.03f);
        float barLeft = (w - barW) / 2f;
        float barTop = top + size + size * 0.12f;
        barPaint.setColor(0x33FFFFFF);
        rect.set(barLeft, barTop, barLeft + barW, barTop + barH);
        canvas.drawRoundRect(rect, barH / 2f, barH / 2f, barPaint);
        barPaint.setColor(Color.WHITE);
        rect.set(barLeft, barTop, barLeft + barW * progress, barTop + barH);
        canvas.drawRoundRect(rect, barH / 2f, barH / 2f, barPaint);
    }
}
