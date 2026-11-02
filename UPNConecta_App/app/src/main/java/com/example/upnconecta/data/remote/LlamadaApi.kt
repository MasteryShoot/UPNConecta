package com.example.upnconecta.data.remote

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Ejecuta una llamada de Retrofit y convierte cualquier falla en un mensaje para el usuario.
 * Lo usan todos los repositorios para que los errores se manejen igual en toda la app.
 */
suspend fun <T> llamarApi(bloque: suspend () -> T): Result<T> =
    try {
        Result.success(bloque())
    } catch (e: HttpException) {
        Result.failure(Exception(mensajeDelServidor(e)))
    } catch (e: SocketTimeoutException) {
        Result.failure(Exception("El servidor está despertando. Vuelve a intentarlo en un minuto."))
    } catch (e: IOException) {
        Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión a internet."))
    }

// El backend responde los errores como {"mensaje": "..."}
private fun mensajeDelServidor(e: HttpException): String {
    val cuerpo = e.response()?.errorBody()?.string()
    val mensaje = runCatching { Gson().fromJson(cuerpo, MensajeApi::class.java)?.mensaje }.getOrNull()
    return mensaje ?: "Error del servidor (${e.code()})"
}
