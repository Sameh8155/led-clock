package com.ledclock.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;

public class AlarmService extends Service {
    static volatile boolean ringing = false;
    static volatile String info = "";
    private MediaPlayer mp;
    private PowerManager.WakeLock wl;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent i, int f, int id) {
        String act = i == null ? null : i.getAction();
        if ("STOP".equals(act)) { stopAll(); return START_NOT_STICKY; }
        if ("SNOOZE".equals(act)) {
            Sched.sp(this).edit().putLong("snooze", System.currentTimeMillis() + 5 * 60000L).apply();
            Sched.schedule(this);
            stopAll();
            return START_NOT_STICKY;
        }
        String t = i == null ? "" : String.valueOf(i.getStringExtra("t"));
        String l = i == null ? "" : String.valueOf(i.getStringExtra("l"));
        if ("null".equals(l)) l = "";
        info = t + "|" + l;
        ringing = true;
        startFg(t, l);
        play();
        h.removeCallbacksAndMessages(null);
        h.postDelayed(this::stopAll, 2 * 60000L); // يوقف لوحده بعد دقيقتين
        return START_NOT_STICKY;
    }

    private void startFg(String t, String l) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("alarm", "المنبه", NotificationManager.IMPORTANCE_HIGH));
        }
        int fl = Sched.flags();
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent po = PendingIntent.getActivity(this, 1, open, fl);
        PendingIntent ps = PendingIntent.getService(this, 2, new Intent(this, AlarmService.class).setAction("STOP"), fl);
        PendingIntent pz = PendingIntent.getService(this, 3, new Intent(this, AlarmService.class).setAction("SNOOZE"), fl);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "alarm") : new Notification.Builder(this);
        b.setContentTitle("⏰ " + (l.isEmpty() ? "حان وقت المنبه" : l)).setContentText(t)
         .setSmallIcon(R.mipmap.ic_launcher).setContentIntent(po).setFullScreenIntent(po, true)
         .setOngoing(true).setCategory(Notification.CATEGORY_ALARM)
         .addAction(0, "غفوة ٥ دقايق", pz).addAction(0, "إيقاف", ps);
        startForeground(2, b.build());
    }

    private void play() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "led:alarm");
            wl.acquire(3 * 60000L);
        } catch (Exception ignored) {}
        try {
            if (mp != null) mp.release();
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            mp.setDataSource(this, u);
            mp.setLooping(true);
            mp.prepare();
            mp.start();
        } catch (Exception ignored) {}
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(new long[]{0, 600, 400}, 0);
        } catch (Exception ignored) {}
    }

    private void stopAll() {
        h.removeCallbacksAndMessages(null);
        try { if (mp != null) mp.release(); } catch (Exception ignored) {}
        mp = null;
        try { Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); if (v != null) v.cancel(); } catch (Exception ignored) {}
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception ignored) {}
        wl = null;
        ringing = false;
        info = "";
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() { stopAll(); super.onDestroy(); }
}
