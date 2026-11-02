package com.example.upnconecta

import android.app.Application
import com.example.upnconecta.data.remote.Conectividad
import com.example.upnconecta.data.remote.RetrofitClient
import kotlinx.coroutines.launch

/** Se ejecuta una vez al abrir la app, antes de cualquier pantalla. */
class UpnConectaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.iniciar(this)
        Conectividad.iniciar(this)

        // Despierta el backend en Render desde ya, para que el login no sea el que espere
        RetrofitClient.alcance.launch {
            runCatching { RetrofitClient.api.estado() }
        }
    }
}
