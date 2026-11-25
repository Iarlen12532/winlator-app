package com.winlator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.xenvironment.RootFS;

import org.json.JSONException;
import org.json.JSONObject;

public class TosSplashActivity extends AppCompatActivity {
    private TosSplashView splashView;
    private boolean drawn = false;
    private boolean ready = false;
    private boolean finishing = false;
    private int containerId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TosShell.register(getApplication());
        initMouseDefaults();

        splashView = new TosSplashView(this);
        splashView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(splashView);

        splashView.runDraw(() -> {
            drawn = true;
            tryFinish();
        });
        prepareSystem();
    }

    private void initMouseDefaults() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("tos_mouse_defaults_v1", false)) {
            prefs.edit()
                .putBoolean("capture_pointer_on_external_mouse", false)
                .putBoolean("tos_mouse_defaults_v1", true)
                .apply();
        }
    }

    private String nativeScreenSize() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        int w = Math.max(dm.widthPixels, dm.heightPixels);
        int h = Math.min(dm.widthPixels, dm.heightPixels);
        w -= w % 2;
        h -= h % 2;
        return w + "x" + h;
    }

    private void prepareSystem() {
        try {
            if (!RootFS.find(this).isValid()) {
                markReady(0);
                return;
            }

            final String size = nativeScreenSize();
            final ContainerManager manager = new ContainerManager(this);
            if (!manager.getContainers().isEmpty()) {
                Container first = manager.getContainers().get(0);
                if (!size.equals(first.getScreenSize())) {
                    first.setScreenSize(size);
                    first.saveData();
                }
                TosWallpaper.install(this, first);
                markReady(first.id);
                return;
            }

            JSONObject data = new JSONObject();
            data.put("name", "TOS");
            data.put("screenSize", size);
            manager.createContainerAsync(data, (created) -> {
                if (created != null) TosWallpaper.install(this, created);
                markReady(created != null ? created.id : 0);
            });
        }
        catch (JSONException e) {
            markReady(0);
        }
    }

    private void markReady(int id) {
        containerId = id;
        ready = true;
        tryFinish();
    }

    private void tryFinish() {
        if (!drawn || !ready || finishing || isFinishing()) return;
        finishing = true;
        splashView.runFinish(() -> launch());
    }

    private void launch() {
        Intent intent;
        if (containerId > 0) {
            intent = new Intent(this, XServerDisplayActivity.class);
            intent.putExtra("container_id", containerId);
            intent.putExtra("tos_boot", true);
        }
        else intent = new Intent(this, MainActivity.class);

        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        if (splashView != null) splashView.stop();
        super.onDestroy();
    }
}
