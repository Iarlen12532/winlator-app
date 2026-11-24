package com.winlator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public class TosGlyph extends View {
    public static final int WIFI = 0;
    public static final int BLUETOOTH = 1;
    public static final int MOON = 2;
    public static final int SUN = 3;
    public static final int SPEAKER = 4;
    public static final int PREV = 5;
    public static final int PLAY = 6;
    public static final int NEXT = 7;
    public static final int COMPASS = 8;
    public static final int SLIDERS = 9;
    public static final int MOUSE = 10;
    public static final int GEAR = 11;

    private final int type;
    private int color;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();

    public TosGlyph(Context context, int type, int color) {
        super(context);
        this.type = type;
        this.color = color;
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }

    private void stroke(float w) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    private void fill() {
        p.setStyle(Paint.Style.FILL);
    }

    private void poly(Canvas c, float s, boolean close, float... pts) {
        path.reset();
        path.moveTo(pts[0] * s, pts[1] * s);
        for (int i = 2; i < pts.length; i += 2) path.lineTo(pts[i] * s, pts[i + 1] * s);
        if (close) path.close();
        c.drawPath(path, p);
    }

    private void rays(Canvas c, float s, float from, float to) {
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45.0);
            float cos = (float) Math.cos(a);
            float sin = (float) Math.sin(a);
            c.drawLine(s * (0.5f + cos * from), s * (0.5f + sin * from), s * (0.5f + cos * to), s * (0.5f + sin * to), p);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight());
        p.setShader(null);
        p.setColor(color);

        switch (type) {
            case WIFI: {
                stroke(s * 0.075f);
                float cx = s * 0.5f;
                float cy = s * 0.78f;
                float[] radii = {0.5f, 0.34f, 0.18f};
                for (float rad : radii) {
                    r.set(cx - rad * s, cy - rad * s, cx + rad * s, cy + rad * s);
                    c.drawArc(r, -135f, 90f, false, p);
                }
                fill();
                c.drawCircle(cx, cy, s * 0.055f, p);
                break;
            }
            case BLUETOOTH:
                stroke(s * 0.075f);
                poly(c, s, false, 0.30f, 0.34f, 0.70f, 0.66f, 0.50f, 0.84f, 0.50f, 0.16f, 0.70f, 0.34f, 0.30f, 0.66f);
                break;
            case MOON: {
                fill();
                Path a = new Path();
                a.addCircle(s * 0.5f, s * 0.5f, s * 0.32f, Path.Direction.CW);
                Path b = new Path();
                b.addCircle(s * 0.66f, s * 0.38f, s * 0.27f, Path.Direction.CW);
                a.op(b, Path.Op.DIFFERENCE);
                c.drawPath(a, p);
                break;
            }
            case SUN:
                fill();
                c.drawCircle(s * 0.5f, s * 0.5f, s * 0.17f, p);
                stroke(s * 0.06f);
                rays(c, s, 0.28f, 0.40f);
                break;
            case SPEAKER:
                fill();
                poly(c, s, true, 0.18f, 0.40f, 0.34f, 0.40f, 0.52f, 0.24f, 0.52f, 0.76f, 0.34f, 0.60f, 0.18f, 0.60f);
                stroke(s * 0.06f);
                float[] waves = {0.22f, 0.36f};
                for (float rad : waves) {
                    r.set(s * (0.52f - rad), s * (0.5f - rad), s * (0.52f + rad), s * (0.5f + rad));
                    c.drawArc(r, -40f, 80f, false, p);
                }
                break;
            case PREV:
                fill();
                poly(c, s, true, 0.50f, 0.50f, 0.78f, 0.30f, 0.78f, 0.70f);
                poly(c, s, true, 0.22f, 0.50f, 0.50f, 0.30f, 0.50f, 0.70f);
                break;
            case NEXT:
                fill();
                poly(c, s, true, 0.50f, 0.50f, 0.22f, 0.30f, 0.22f, 0.70f);
                poly(c, s, true, 0.78f, 0.50f, 0.50f, 0.30f, 0.50f, 0.70f);
                break;
            case PLAY:
                fill();
                poly(c, s, true, 0.34f, 0.24f, 0.34f, 0.76f, 0.78f, 0.50f);
                break;
            case COMPASS:
                stroke(s * 0.06f);
                c.drawCircle(s * 0.5f, s * 0.5f, s * 0.38f, p);
                fill();
                poly(c, s, true, 0.66f, 0.34f, 0.46f, 0.46f, 0.54f, 0.54f);
                p.setColor((color & 0x00FFFFFF) | 0x96000000);
                poly(c, s, true, 0.34f, 0.66f, 0.46f, 0.46f, 0.54f, 0.54f);
                break;
            case SLIDERS:
                stroke(s * 0.06f);
                r.set(s * 0.12f, s * 0.22f, s * 0.88f, s * 0.44f);
                c.drawRoundRect(r, s * 0.11f, s * 0.11f, p);
                r.set(s * 0.12f, s * 0.56f, s * 0.88f, s * 0.78f);
                c.drawRoundRect(r, s * 0.11f, s * 0.11f, p);
                fill();
                c.drawCircle(s * 0.34f, s * 0.33f, s * 0.07f, p);
                c.drawCircle(s * 0.66f, s * 0.67f, s * 0.07f, p);
                break;
            case MOUSE:
                stroke(s * 0.07f);
                r.set(s * 0.30f, s * 0.14f, s * 0.70f, s * 0.86f);
                c.drawRoundRect(r, s * 0.2f, s * 0.2f, p);
                c.drawLine(s * 0.5f, s * 0.14f, s * 0.5f, s * 0.42f, p);
                break;
            default:
                stroke(s * 0.09f);
                c.drawCircle(s * 0.5f, s * 0.5f, s * 0.2f, p);
                stroke(s * 0.1f);
                rays(c, s, 0.30f, 0.40f);
                break;
        }
    }
}
