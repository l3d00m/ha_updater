package de.l3d00m.ha_updater

import androidx.annotation.WorkerThread
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path


interface HomeassistantAPI {
    @POST("states/{entity_id}")
    suspend fun updateEntity(@Path("entity_id") entity_id:String, @Body state: HomeassistantState): String?
}