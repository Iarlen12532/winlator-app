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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

public class TosSplashView extends View {
    private static final long DRAW_MS = 2300;
    private static final long JUMP_MS = 220;
    private static final long ZOOM_MS = 650;
    private static final float BAR_SLOW_END = 0.72f;
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
    private float drawProgress = 0f;
    private float barProgress = 0f;
    private float zoom = 0f;
    private boolean cancelled = false;
    private ValueAnimator drawAnimator;
    private ValueAnimator jumpAnimator;
    private ValueAnimator zoomAnimator;

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

    public void runDraw(final Runnable onDone) {
        drawAnimator = ValueAnimator.ofFloat(0f, 1f);
        drawAnimator.setDuration(DRAW_MS);
        drawAnimator.setInterpolator(new LinearInterpolator());
        drawAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                drawProgress = (Float) a.getAnimatedValue();
                barProgress = BAR_SLOW_END * drawProgress;
                invalidate();
            }
        });
        drawAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                if (!cancelled) onDone.run();
            }
        });
        drawAnimator.start();
    }

    public void runFinish(final Runnable onDone) {
        zoomAnimator = ValueAnimator.ofFloat(0f, 1f);
        zoomAnimator.setDuration(ZOOM_MS);
        zoomAnimator.setInterpolator(new AccelerateInterpolator(1.6f));
        zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                zoom = (Float) a.getAnimatedValue();
                invalidate();
            }
        });
        zoomAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                if (!cancelled) onDone.run();
            }
        });

        jumpAnimator = ValueAnimator.ofFloat(BAR_SLOW_END, 1f);
        jumpAnimator.setDuration(JUMP_MS);
        jumpAnimator.setInterpolator(new DecelerateInterpolator());
        jumpAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                barProgress = (Float) a.getAnimatedValue();
                invalidate();
            }
        });
        jumpAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                if (!cancelled) zoomAnimator.start();
            }
        });
        jumpAnimator.start();
    }

    public void stop() {
        cancelled = true;
        if (drawAnimator != null) drawAnimator.cancel();
        if (jumpAnimator != null) jumpAnimator.cancel();
        if (zoomAnimator != null) zoomAnimator.cancel();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        int w = getWidth(), h = getHeight();
        float size = Math.min(w, h) * 0.34f;
        float scale = size / 1024f;
        float left = (w - size) / 2f;
        float top = h * 0.5f - size * 0.62f;
        float cx = left + size / 2f;
        float cy = top + size * 0.55f;

        float zs = 1f + 19f * zoom;
        int alpha = 255;
        if (zoom > 0.55f) alpha = (int) (255 * Math.max(0f, 1f - (zoom - 0.55f) / 0.45f));
        dotPaint.setAlpha(alpha);

        canvas.save();
        canvas.scale(zs, zs, cx, cy);
        int count = (int) (drawProgress * xs.length);
        for (int i = 0; i < count; i++) {
            canvas.drawCircle(left + xs[i] * scale, top + ys[i] * scale, rs[i] * scale, dotPaint);
        }
        canvas.restore();

        float barAlpha = Math.max(0f, 1f - zoom * 4f) * 255f;
        if (barAlpha > 0f) {
            float barW = size * 0.9f;
            float barH = Math.max(6f, size * 0.03f);
            float barLeft = (w - barW) / 2f;
            float barTop = top + size + size * 0.12f;
            barPaint.setColor(Color.argb((int) (barAlpha * 0.2f), 255, 255, 255));
            rect.set(barLeft, barTop, barLeft + barW, barTop + barH);
            canvas.drawRoundRect(rect, barH / 2f, barH / 2f, barPaint);
            barPaint.setColor(Color.argb((int) barAlpha, 255, 255, 255));
            rect.set(barLeft, barTop, barLeft + barW * barProgress, barTop + barH);
            canvas.drawRoundRect(rect, barH / 2f, barH / 2f, barPaint);
        }
    }
}
