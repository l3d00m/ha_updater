package de.l3d00m.ha_updater

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
            return // Wrong action, return
        HomeassistantInteractor(context).pushNewAlarm()
    }

}