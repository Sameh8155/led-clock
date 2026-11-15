package com.ledclock.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {
    private WebView web;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 33) {
            try { requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1); } catch (Exception ignored) {}
        }
        web = new WebView(this);
        setContentView(web);
        web.setBackgroundColor(0xFF000000);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.addJavascriptInterface(new Bridge(), "LedNative");
        web.loadUrl("file:///android_asset/index.html");
        hideBars();
        Sched.schedule(this);
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override protected void onResume() { super.onResume(); hideBars(); if (web != null) web.onResume(); }

    class Bridge {
        @JavascriptInterface public void setAlarms(String json) {
            Sched.sp(MainActivity.this).edit().putString("alarms", json).apply();
            Sched.schedule(MainActivity.this);
        }
        @JavascriptInterface public String getAlarms() { return Sched.sp(MainActivity.this).getString("alarms", "[]"); }
        @JavascriptInterface public boolean ringing() { return AlarmService.ringing; }
        @JavascriptInterface public String info() { return AlarmService.info; }
        @JavascriptInterface public void stop() {
            startService(new Intent(MainActivity.this, AlarmService.class).setAction("STOP"));
        }
        @JavascriptInterface public void snooze() {
            startService(new Intent(MainActivity.this, AlarmService.class).setAction("SNOOZE"));
        }
    }
}
