package de.l3d00m.ha_updater

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


class HomeassistantRepository(url: String, authToken: String) {
    private val client: HomeassistantAPI by lazy {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        // Add logs to HTTP calls in case of debug build
        val httpClientBuilder = if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.HEADERS
            OkHttpClient.Builder().addInterceptor(interceptor)
        } else OkHttpClient.Builder()

        // Append the auth token to all requests as a Header
        val httpClient = httpClientBuilder.addInterceptor { chain ->
            val request = chain.request().newBuilder().addHeader("Authorization", "Bearer $authToken").build()
            chain.proceed(request)
        }.build()

        // Modify the URL
        var baseUrl = url
        // Add trailing URL backslash in case user doesn't input it
        if (!baseUrl.endsWith("/"))
            baseUrl += "/"
        // Remove "api/" suffix as it'll be appended later already
        baseUrl = baseUrl.removeSuffix("api/")


        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        retrofit.create(HomeassistantAPI::class.java)
    }

    suspend fun putState(entityId: String, timeString: String): List<HomeassistantPOJO.EntityResponse?>? {
        return client.updateEntity(HomeassistantPOJO.DatetimeServiceBody(entityId, timeString))
    }


    suspend fun getApiStatus(): HomeassistantPOJO.ApiResponse? {
        return client.getApiStatus()
    }
}