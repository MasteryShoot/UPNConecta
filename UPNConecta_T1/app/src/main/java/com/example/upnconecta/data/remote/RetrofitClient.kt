package com.example.upnconecta.data.remote

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.upnconecta.BuildConfig
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.usuario.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Instancia única de Retrofit para toda la app. */
object RetrofitClient {

    private lateinit var contexto: Context
    private val preferencias by lazy { Preferencias(contexto) }

    // Para llamadas que deben terminar aunque el usuario ya haya salido de la pantalla
    val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Se llama una sola vez desde UpnConectaApp, antes de abrir cualquier pantalla. */
    fun iniciar(context: Context) {
        contexto = context.applicationContext
    }

    // En desarrollo se ve en Logcat lo que se envía y recibe; en release no se registra nada
    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
    }

    // Agrega el token de sesión; si el backend ya no lo acepta, se vuelve al Login
    private val sesion = Interceptor { cadena ->
        val token = preferencias.token()
        val pedido = cadena.request()
        val respuesta = cadena.proceed(
            if (token == null) pedido
            else pedido.newBuilder().header("Authorization", "Bearer $token").build()
        )
        // En /auth/ un 401 solo significa contraseña incorrecta
        if (respuesta.code == 401 && !pedido.url.encodedPath.contains("/auth/")) sesionVencida()
        respuesta
    }

    private fun sesionVencida() {
        preferencias.cerrarSesion()
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(contexto, "Tu sesión venció. Vuelve a iniciar sesión.", Toast.LENGTH_LONG).show()
            contexto.startActivity(
                Intent(contexto, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    private val cliente = OkHttpClient.Builder()
        .addInterceptor(sesion)
        .addInterceptor(logging)
        // Render apaga el servicio gratuito sin uso y tarda más de un minuto en despertarlo
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
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
