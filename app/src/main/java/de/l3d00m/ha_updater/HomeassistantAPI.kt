package de.l3d00m.ha_updater

import retrofit2.http.*


interface HomeassistantAPI {
    @Headers("Content-Type: application/json")
    @POST("states/{entity_id}")
    suspend fun updateEntity(
        @Header("Authorization") token: String,
        @Path("entity_id") entity_id: String,
        @Body entity: HomeassistantPOJO.Entity
    ): HomeassistantPOJO.EntityResponse?
}