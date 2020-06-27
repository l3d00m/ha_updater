package de.l3d00m.ha_updater

import retrofit2.http.*


interface HomeassistantAPI {
    @Headers("Content-Type: application/json")
    @POST("api/services/input_datetime/set_datetime")
    suspend fun updateEntity(
        @Body datetimeServiceBody: HomeassistantPOJO.DatetimeServiceBody
    ): List<HomeassistantPOJO.EntityResponse?>?

    @Headers("Content-Type: application/json")
    @GET("api/")
    suspend fun getApiStatus(): HomeassistantPOJO.ApiResponse?
}