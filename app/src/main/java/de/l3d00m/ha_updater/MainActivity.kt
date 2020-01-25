package de.l3d00m.ha_updater

import android.os.Bundle
import android.webkit.URLUtil
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) Timber.plant(LineNumberDebugTree())

        setContentView(R.layout.activity_main)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, MainSettingsFragment())
            .commit()

        setSupportActionBar(toolbar)

        pushNewAlarm()
    }

    private fun pushNewAlarm() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val baseUrl = sharedPreferences.getString(KeyConstants(resources).HA_URL_KEY, "")
        if (!URLUtil.isValidUrl(baseUrl)) {
            Timber.i("URL not valid. Was: $baseUrl")
            return
        }

        // Null cast because URLUtil returns false if URL is null
        val repository = HomeassistantRepository(baseUrl!!)

        val result = this.launch { repository.putState(12, this@MainActivity) }
    }


    override val coroutineContext: CoroutineContext =
        Dispatchers.Main + SupervisorJob()

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext[Job]!!.cancel()
    }


}
