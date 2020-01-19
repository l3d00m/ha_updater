package de.l3d00m.ha_updater

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Hasdasdi", "Next alarm: " + getNextAlarmMs(context))
    }

    private fun getNextAlarmMs(context: Context): Long {
        val alarmManager: AlarmManager? =
            context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val clockInfo: AlarmManager.AlarmClockInfo? = alarmManager?.nextAlarmClock
        val time = clockInfo?.triggerTime ?: 0
        return time
    }

}