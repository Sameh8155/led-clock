package com.ledclock.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        long id = i.getLongExtra("id", -1);
        if (id >= 0) {
            // المنبه "مرة واحدة" (من غير أيام) يتقفل بعد ما يرن
            try {
                JSONArray a = Sched.alarms(c);
                for (int k = 0; k < a.length(); k++) {
                    JSONObject o = a.getJSONObject(k);
                    if (o.optLong("id", -2) == id) {
                        JSONArray d = o.optJSONArray("d");
                        if (d == null || d.length() == 0) o.put("on", false);
                    }
                }
                Sched.sp(c).edit().putString("alarms", a.toString()).apply();
            } catch (Exception ignored) {}
        } else {
            Sched.sp(c).edit().remove("snooze").apply();
        }
        Intent s = new Intent(c, AlarmService.class)
                .putExtra("l", i.getStringExtra("l")).putExtra("t", i.getStringExtra("t"));
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(s); else c.startService(s);
        } catch (Exception ignored) {}
        Sched.schedule(c);
    }
}
