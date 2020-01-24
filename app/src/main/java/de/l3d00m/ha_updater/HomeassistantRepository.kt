package de.l3d00m.ha_updater

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val BASE_URL = "http://192.168.0.40:8123/api/"
const val ENTITY_ID = "sensor.alarm_clock"
const val AUTH_TOKEN =
    "Bearer " + ""

class HomeassistantRepository {
    private val client: HomeassistantAPI by lazy {

        val gson = GsonBuilder()
            .setLenient()
            .create()
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        retrofit.create(HomeassistantAPI::class.java)

    }

    suspend fun putState(newState: Int) =
        client.updateEntity(AUTH_TOKEN, ENTITY_ID, HomeassistantPOJO.Entity(newState))
}