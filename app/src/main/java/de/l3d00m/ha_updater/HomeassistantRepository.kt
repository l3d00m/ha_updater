package de.l3d00m.ha_updater

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


class HomeassistantRepository(var url: String, authToken: String) {
    private val client: HomeassistantAPI by lazy {
        if (url.isEmpty()) throw Exception("No Homeassistant URL specified")
        if (authToken.isEmpty()) throw Exception("No API Token specified")
        val gson = GsonBuilder()
            .setLenient()
            .create()

        val httpClientBuilder = if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            OkHttpClient.Builder().addInterceptor(interceptor)
        } else OkHttpClient.Builder()

        val httpClient = httpClientBuilder.addInterceptor { chain ->
            val request = chain.request().newBuilder().addHeader("Authorization", "Bearer $authToken").build()
            chain.proceed(request)
        }.build()

        // Add trailing URL backslash in case user doesn't input it
        if (!url.endsWith("/"))
            url += "/"
        // Remove "api/" suffix as it'll be appended later already
        url = url.removeSuffix("api/")


        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        retrofit.create(HomeassistantAPI::class.java)
    }

    suspend fun putState(newState: Long, context: Context): List<HomeassistantPOJO.EntityResponse?>? {
        val entityId = Prefs(context).entityId
        if (entityId.isEmpty()) throw Exception("No entity ID specified")
        val timeString = getDateTime(newState)
        return client.updateEntity(HomeassistantPOJO.DatetimeServiceBody(entityId, timeString))
    }

    private fun getDateTime(input: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val netDate = Date(input)
        return sdf.format(netDate)
    }

    suspend fun getStatus(): HomeassistantPOJO.ApiResponse? {
        return client.getApiStatus()
    }
}