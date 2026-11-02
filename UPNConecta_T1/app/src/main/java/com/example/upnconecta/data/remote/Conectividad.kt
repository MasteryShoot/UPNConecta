package com.example.upnconecta.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Avisa a la interfaz cuando el teléfono pierde o recupera la conexión a internet. */
object Conectividad {

    private val estado = MutableStateFlow(true)
    val enLinea: StateFlow<Boolean> = estado.asStateFlow()

    fun iniciar(context: Context) {
        val gestor = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        estado.value = gestor.getNetworkCapabilities(gestor.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        gestor.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                estado.value = true
            }

            override fun onLost(network: Network) {
                estado.value = false
            }
        })
    }
}
