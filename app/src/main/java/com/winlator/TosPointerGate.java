package com.winlator;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.winlator.math.XForm;
import com.winlator.renderer.ViewTransformation;
import com.winlator.xserver.Pointer;
import com.winlator.xserver.XServer;

public final class TosPointerGate {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final float[] XFORM = XForm.getInstance();

    private static TosShell shell;
    private static XServer xServer;
    private static int swallowed = 0;
    private static boolean dragging = false;
    private static boolean hovering = false;
    private static long downTime = 0;
    private static float lastX = 0;
    private static float lastY = 0;

    private TosPointerGate() {}

    public static void attach(TosShell s, XServer xs) {
        shell = s;
        xServer = xs;
        swallowed = 0;
        dragging = false;
        hovering = false;
        xs.pointer.addOnPointerMotionListener(new Pointer.OnPointerMotionListener() {
            @Override
            public void onPointerMove(short x, short y) {
                final float[] p = toLocal(x, y);
                MAIN.post(() -> handleMove(p[0], p[1]));
            }
        });
    }

    public static boolean onButton(XServer xs, Pointer.Button button, boolean pressed) {
        final TosShell s = shell;
        if (s == null || xs != xServer) return false;
        final int bit = 1 << button.ordinal();

        if (!pressed) {
            if ((swallowed & bit) == 0) return false;
            swallowed &= ~bit;
            if (button == Pointer.Button.BUTTON_LEFT) {
                MAIN.post(() -> {
                    if (dragging) {
                        touch(MotionEvent.ACTION_UP, lastX, lastY);
                        dragging = false;
                    }
                });
            }
            return true;
        }

        float[] p = toLocal(xs.pointer.getX(), xs.pointer.getY());
        if (!s.isOverUiLocal(p[0], p[1])) return false;
        swallowed |= bit;
        final float x = p[0];
        final float y = p[1];

        if (button == Pointer.Button.BUTTON_LEFT) {
            MAIN.post(() -> {
                if (hovering) {
                    hovering = false;
                    hover(MotionEvent.ACTION_HOVER_EXIT, x, y);
                }
                dragging = true;
                downTime = SystemClock.uptimeMillis();
                lastX = x;
                lastY = y;
                s.setCursor(true, x, y);
                touch(MotionEvent.ACTION_DOWN, x, y);
            });
        }
        else if (button == Pointer.Button.BUTTON_SCROLL_UP) MAIN.post(() -> scroll(x, y, 1f));
        else if (button == Pointer.Button.BUTTON_SCROLL_DOWN) MAIN.post(() -> scroll(x, y, -1f));
        return true;
    }

    private static void handleMove(float x, float y) {
        TosShell s = shell;
        if (s == null) return;
        lastX = x;
        lastY = y;

        if (dragging) {
            s.setCursor(true, x, y);
            touch(MotionEvent.ACTION_MOVE, x, y);
            return;
        }

        boolean over = s.isOverUiLocal(x, y);
        s.setCursor(over, x, y);
        if (over) {
            if (!hovering) {
                hovering = true;
                hover(MotionEvent.ACTION_HOVER_ENTER, x, y);
            }
            hover(MotionEvent.ACTION_HOVER_MOVE, x, y);
        }
        else if (hovering) {
            hovering = false;
            hover(MotionEvent.ACTION_HOVER_EXIT, x, y);
        }
    }

    private static void touch(int action, float x, float y) {
        TosShell s = shell;
        if (s == null) return;
        MotionEvent ev = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0);
        ev.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        s.dispatchTouchEvent(ev);
        ev.recycle();
    }

    private static void hover(int action, float x, float y) {
        TosShell s = shell;
        if (s == null) return;
        long now = SystemClock.uptimeMillis();
        MotionEvent ev = MotionEvent.obtain(now, now, action, x, y, 0);
        ev.setSource(InputDevice.SOURCE_MOUSE);
        s.dispatchGenericMotionEvent(ev);
        ev.recycle();
    }

    private static void scroll(float x, float y, float amount) {
        TosShell s = shell;
        if (s == null) return;
        long now = SystemClock.uptimeMillis();
        MotionEvent.PointerProperties[] props = {new MotionEvent.PointerProperties()};
        props[0].id = 0;
        props[0].toolType = MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords[] coords = {new MotionEvent.PointerCoords()};
        coords[0].x = x;
        coords[0].y = y;
        coords[0].setAxisValue(MotionEvent.AXIS_VSCROLL, amount);
        MotionEvent ev = MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL, 1, props, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0);
        s.dispatchGenericMotionEvent(ev);
        ev.recycle();
    }

    private static synchronized float[] toLocal(int px, int py) {
        TosShell s = shell;
        XServer xs = xServer;
        if (s == null || xs == null || s.getWidth() == 0 || s.getHeight() == 0) return new float[]{px, py};

        ViewTransformation vt = new ViewTransformation();
        vt.update(s.getWidth(), s.getHeight(), xs.screenInfo.width, xs.screenInfo.height);
        float inv = 1.0f / vt.aspect;
        if (!xs.getRenderer().isFullscreen()) {
            XForm.makeTranslation(XFORM, -vt.viewOffsetX, -vt.viewOffsetY);
            XForm.scale(XFORM, inv, inv);
        }
        else XForm.makeScale(XFORM, inv, inv);

        float[] a = XForm.transformPoint(XFORM, 0, 0);
        float x0 = a[0];
        float y0 = a[1];
        float[] b = XForm.transformPoint(XFORM, 100, 100);
        float scale = (b[0] - x0) / 100f;
        if (scale == 0f) scale = 1f;
        return new float[]{(px - x0) / scale, (py - y0) / scale};
    }
}
