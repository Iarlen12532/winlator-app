package com.winlator;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressLint("SetJavaScriptEnabled")
public class TosBrowserWindow extends TosWindow {
    private static final String DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private WebView web;
    private EditText urlField;

    public TosBrowserWindow(final TosShell shell) {
        super(shell, "Navegador", 0.78f, 0.74f);

        web = new WebView(ctx);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setUserAgentString(DESKTOP_UA);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (urlField != null && !urlField.hasFocus()) urlField.setText(url);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                shell.enterFullscreen(view, callback);
            }

            @Override
            public void onHideCustomView() {
                shell.exitFullscreen();
            }
        });

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(0xFF2B2B2B);
        bar.setPadding(px(8), px(6), px(8), px(6));

        bar.addView(navButton("\u2039", new Runnable() {
            @Override
            public void run() {
                if (web.canGoBack()) web.goBack();
            }
        }));
        bar.addView(navButton("\u203A", new Runnable() {
            @Override
            public void run() {
                if (web.canGoForward()) web.goForward();
            }
        }));
        bar.addView(navButton("\u21BB", new Runnable() {
            @Override
            public void run() {
                web.reload();
            }
        }));

        urlField = new EditText(ctx);
        urlField.setSingleLine(true);
        urlField.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlField.setTextColor(Color.WHITE);
        urlField.setHintTextColor(0x88FFFFFF);
        urlField.setHint("Pesquisar ou digitar endere\u00e7o");
        urlField.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(14));
        urlField.setPadding(px(14), px(6), px(14), px(6));
        GradientDrawable fb = new GradientDrawable();
        fb.setColor(0xFF3E3E3E);
        fb.setCornerRadius(px(16));
        urlField.setBackground(fb);
        urlField.setOnEditorActionListener((v, actionId, e) -> {
            boolean enter = e != null && e.getKeyCode() == KeyEvent.KEYCODE_ENTER && e.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                go(urlField.getText().toString());
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ulp.leftMargin = px(8);
        bar.addView(urlField, ulp);

        col.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        content.addView(col, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        finishLayout();

        web.loadUrl("https://www.google.com");
    }

    private TextView navButton(String text, final Runnable action) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(22));
        t.setGravity(Gravity.CENTER);
        t.setPadding(px(12), 0, px(12), 0);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        return t;
    }

    private void go(String text) {
        String t = text.trim();
        if (t.isEmpty()) return;
        String url;
        if (t.startsWith("http://") || t.startsWith("https://")) url = t;
        else if (t.contains(".") && !t.contains(" ")) url = "https://" + t;
        else url = "https://www.google.com/search?q=" + Uri.encode(t);
        web.loadUrl(url);
        web.requestFocus();
    }

    @Override
    public void onClosed() {
        shell.exitFullscreen();
        web.stopLoading();
        web.destroy();
    }
}
