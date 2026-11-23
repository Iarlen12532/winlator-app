package com.winlator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

public class TosIconView extends View {
    public static final int FINDER = 0;
    public static final int LAUNCHPAD = 1;
    public static final int INSTALL = 2;
    public static final int SETTINGS = 3;
    public static final int TRASH = 4;

    private final int type;
    private final int topColor;
    private final int bottomColor;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();

    public TosIconView(Context context, int type, int topColor, int bottomColor) {
        super(context);
        this.type = type;
        this.topColor = topColor;
        this.bottomColor = bottomColor;
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight());
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setShader(new LinearGradient(0, 0, 0, s, topColor, bottomColor, Shader.TileMode.CLAMP));
        r.set(0, 0, s, s);
        c.drawRoundRect(r, s * 0.23f, s * 0.23f, bgPaint);

        fg.setShader(null);
        fg.setStyle(Paint.Style.STROKE);
        fg.setStrokeCap(Paint.Cap.ROUND);
        fg.setStrokeJoin(Paint.Join.ROUND);
        fg.setStrokeWidth(s * 0.02f);
        fg.setColor(0x55FFFFFF);
        r.set(s * 0.01f, s * 0.01f, s * 0.99f, s * 0.99f);
        c.drawRoundRect(r, s * 0.22f, s * 0.22f, fg);
        fg.setColor(Color.WHITE);

        switch (type) {
            case FINDER: drawFinder(c, s); break;
            case LAUNCHPAD: drawLaunchpad(c, s); break;
            case INSTALL: drawInstall(c, s); break;
            case SETTINGS: drawSettings(c, s); break;
            default: drawTrash(c, s); break;
        }
    }

    private void drawFinder(Canvas c, float s) {
        fg.setStyle(Paint.Style.STROKE);
        fg.setStrokeWidth(s * 0.07f);
        c.drawLine(s * 0.34f, s * 0.30f, s * 0.34f, s * 0.46f, fg);
        c.drawLine(s * 0.66f, s * 0.30f, s * 0.66f, s * 0.46f, fg);
        fg.setStrokeWidth(s * 0.05f);
        c.drawLine(s * 0.52f, s * 0.32f, s * 0.50f, s * 0.56f, fg);
        fg.setStrokeWidth(s * 0.06f);
        r.set(s * 0.24f, s * 0.44f, s * 0.76f, s * 0.80f);
        c.drawArc(r, 25f, 130f, false, fg);
    }

    private void drawLaunchpad(Canvas c, float s) {
        fg.setStyle(Paint.Style.FILL);
        float cell = s * 0.17f;
        float gap = s * 0.07f;
        float start = (s - (3 * cell + 2 * gap)) / 2f;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                float x = start + j * (cell + gap);
                float y = start + i * (cell + gap);
                r.set(x, y, x + cell, y + cell);
                c.drawRoundRect(r, cell * 0.3f, cell * 0.3f, fg);
            }
        }
    }

    private void drawInstall(Canvas c, float s) {
        fg.setStyle(Paint.Style.STROKE);
        fg.setStrokeWidth(s * 0.09f);
        c.drawLine(s * 0.5f, s * 0.22f, s * 0.5f, s * 0.60f, fg);
        path.reset();
        path.moveTo(s * 0.32f, s * 0.44f);
        path.lineTo(s * 0.5f, s * 0.62f);
        path.lineTo(s * 0.68f, s * 0.44f);
        c.drawPath(path, fg);
        fg.setStrokeWidth(s * 0.08f);
        c.drawLine(s * 0.28f, s * 0.78f, s * 0.72f, s * 0.78f, fg);
    }

    private void drawSettings(Canvas c, float s) {
        float cx = s / 2f;
        float cy = s / 2f;
        fg.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 8; i++) {
            c.save();
            c.rotate(i * 45f, cx, cy);
            r.set(cx - s * 0.055f, cy - s * 0.36f, cx + s * 0.055f, cy - s * 0.20f);
            c.drawRoundRect(r, s * 0.02f, s * 0.02f, fg);
            c.restore();
        }
        c.drawCircle(cx, cy, s * 0.26f, fg);
        bgPaint.setShader(null);
        bgPaint.setColor(bottomColor);
        c.drawCircle(cx, cy, s * 0.11f, bgPaint);
    }

    private void drawTrash(Canvas c, float s) {
        fg.setStyle(Paint.Style.STROKE);
        fg.setStrokeWidth(s * 0.055f);
        c.drawLine(s * 0.27f, s * 0.30f, s * 0.73f, s * 0.30f, fg);
        path.reset();
        path.moveTo(s * 0.42f, s * 0.30f);
        path.lineTo(s * 0.42f, s * 0.22f);
        path.lineTo(s * 0.58f, s * 0.22f);
        path.lineTo(s * 0.58f, s * 0.30f);
        c.drawPath(path, fg);
        path.reset();
        path.moveTo(s * 0.33f, s * 0.36f);
        path.lineTo(s * 0.37f, s * 0.76f);
        path.lineTo(s * 0.63f, s * 0.76f);
        path.lineTo(s * 0.67f, s * 0.36f);
        c.drawPath(path, fg);
        fg.setStrokeWidth(s * 0.04f);
        c.drawLine(s * 0.46f, s * 0.44f, s * 0.46f, s * 0.68f, fg);
        c.drawLine(s * 0.54f, s * 0.44f, s * 0.54f, s * 0.68f, fg);
    }
}
