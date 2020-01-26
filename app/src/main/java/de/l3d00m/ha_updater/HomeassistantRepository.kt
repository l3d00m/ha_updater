package de.l3d00m.ha_updater

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber


class HomeassistantRepository(var url: String) {
    private val client: HomeassistantAPI by lazy {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        val httpClient = if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            OkHttpClient.Builder().addInterceptor(interceptor).build()
        } else OkHttpClient.Builder().build()

        // Fix URL in case user enters it wrong
        if (!url.endsWith("/"))
            url += "/"


        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        retrofit.create(HomeassistantAPI::class.java)
    }

    suspend fun putState(newState: Long, context: Context): HomeassistantPOJO.EntityResponse? {
        //TODO Error Handling, default values etc
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val authTokenKey = context.resources.getString(R.string.HA_API_TOKEN)
        var authToken = sharedPreferences.getString(authTokenKey, "")!!
        authToken = "Bearer $authToken"
        val entityIdKey = context.resources.getString(R.string.ALARM_ENTITY_ID)
        val entityId = sharedPreferences.getString(entityIdKey, "")!!
        return try {
            client.updateEntity(authToken, entityId, HomeassistantPOJO.Entity(newState))
        } catch (he: HttpException) {
            Timber.w("Putting state failed with HTTP ${he.code()}: ${he.message()}")
            null
        }
    }

    suspend fun getStatus(token: String): HomeassistantPOJO.ApiResponse? {
        val authToken = "Bearer $token"
        return client.getApiStatus(authToken)
    }
}