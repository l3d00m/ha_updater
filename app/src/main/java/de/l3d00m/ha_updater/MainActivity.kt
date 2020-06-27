package de.l3d00m.ha_updater

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) Timber.plant(LineNumberDebugTree())

        setContentView(R.layout.activity_main)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, MainSettingsFragment())
            .commit()

        setSupportActionBar(toolbar)

        //HomeassistantInteractor(this).pushNewAlarm()
    }
}
