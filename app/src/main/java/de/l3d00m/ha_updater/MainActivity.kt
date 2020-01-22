package de.l3d00m.ha_updater

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    val repository: HomeassistantRepository = HomeassistantRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val ergebnis = this.launch { repository.putState(12) }
    }


    override val coroutineContext: CoroutineContext =
        Dispatchers.Main + SupervisorJob()

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext[Job]!!.cancel()
    }


}
