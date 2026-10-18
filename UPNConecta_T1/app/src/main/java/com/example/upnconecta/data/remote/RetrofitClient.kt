package com.example.upnconecta.data.remote

import com.example.upnconecta.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Instancia única de Retrofit para toda la app. */
object RetrofitClient {

    // En desarrollo se ve en Logcat lo que se envía y recibe; en release no se registra nada
    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    }

    private val cliente = OkHttpClient.Builder()
        .addInterceptor(logging)
        // Render "despierta" el servicio gratuito en unos segundos, por eso la lectura espera más
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
