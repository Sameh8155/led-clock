package com.ledclock.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/** يحسب أقرب منبه ويسجله في AlarmManager (يرن حتى لو الجهاز نايم أو التطبيق مقفول). */
public class Sched {
    static final String P = "led";

    static SharedPreferences sp(Context c) { return c.getSharedPreferences(P, Context.MODE_PRIVATE); }

    static JSONArray alarms(Context c) {
        try { return new JSONArray(sp(c).getString("alarms", "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    static int flags() { return PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0); }

    static boolean has(JSONArray d, int day) {
        for (int i = 0; i < d.length(); i++) if (d.optInt(i, -1) == day) return true;
        return false;
    }

    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        long now = System.currentTimeMillis(), best = 0, id = -1;
        String label = "";
        JSONArray a = alarms(c);
        for (int k = 0; k < a.length(); k++) {
            try {
                JSONObject o = a.getJSONObject(k);
                if (!o.optBoolean("on", true)) continue;
                String[] hm = o.getString("t").split(":");
                JSONArray d = o.optJSONArray("d");
                for (int off = 0; off < 8; off++) {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, off);
                    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0]));
                    cal.set(Calendar.MINUTE, Integer.parseInt(hm[1]));
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    long t = cal.getTimeInMillis();
                    if (t <= now) continue;
                    if (d != null && d.length() > 0 && !has(d, cal.get(Calendar.DAY_OF_WEEK) - 1)) continue;
                    if (best == 0 || t < best) { best = t; id = o.optLong("id", -1); label = o.optString("l", ""); }
                    break;
                }
            } catch (Exception ignored) {}
        }
        long sn = sp(c).getLong("snooze", 0);
        if (sn > now && (best == 0 || sn < best)) { best = sn; id = -1; label = "غفوة"; }

        Intent i = new Intent(c, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 77, i, flags());
        if (best == 0) { am.cancel(pi); return; }
        Calendar f = Calendar.getInstance();
        f.setTimeInMillis(best);
        i.putExtra("id", id).putExtra("l", label)
         .putExtra("t", String.format("%02d:%02d", f.get(Calendar.HOUR_OF_DAY), f.get(Calendar.MINUTE)));
        pi = PendingIntent.getBroadcast(c, 77, i, flags());
        PendingIntent show = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), flags());
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(best, show), pi);
    }
}
