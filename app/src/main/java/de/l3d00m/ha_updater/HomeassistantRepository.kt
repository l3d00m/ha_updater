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
import java.text.SimpleDateFormat
import java.util.*


class HomeassistantRepository(var url: String, authToken: String) {
    private val client: HomeassistantAPI by lazy {
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

    suspend fun putState(newState: Long, context: Context): List<HomeassistantPOJO.EntityResponse?> {
        //TODO Error Handling, default values etc
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val entityIdKey = KeyConstants(context.resources).ENTITIY_ID_KEY
        val entityId = sharedPreferences.getString(entityIdKey, "")!!
        val timestring = getDateTime(newState)
        return try {
            client.updateEntity(
                HomeassistantPOJO.DatetimeServiceBody(entityId, timestring!!)
            )
        } catch (he: HttpException) {
            Timber.w("Putting state failed with HTTP ${he.code()}: ${he.message()}")
            Collections.emptyList()
        }
    }

    private fun getDateTime(input: Long): String? {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val netDate = Date(input)
        return sdf.format(netDate)
    }

    suspend fun getStatus(): HomeassistantPOJO.ApiResponse? {
        return client.getApiStatus()
    }
}