package de.l3d00m.ha_updater

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class AlarmClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
            return // Wrong action, return
        Timber.d("Next alarm: %l", getNextAlarmMs(context))
    }

    private fun getNextAlarmMs(context: Context): Long {
        val alarmManager: AlarmManager? =
            context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val clockInfo: AlarmManager.AlarmClockInfo? = alarmManager?.nextAlarmClock
        return clockInfo?.triggerTime ?: 0
    }

}