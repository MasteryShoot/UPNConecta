package com.example.upnconecta.data

import com.example.upnconecta.data.remote.ApiService
import com.example.upnconecta.data.remote.LoginRequest
import com.example.upnconecta.data.remote.MensajeApi
import com.example.upnconecta.data.remote.MicrosoftAuth
import com.example.upnconecta.data.remote.MicrosoftRequest
import com.example.upnconecta.data.remote.RegistroRequest
import com.example.upnconecta.data.remote.RetrofitClient
import com.example.upnconecta.data.remote.UsuarioRespuesta
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/** Registro e inicio de sesión contra el backend (T2). */
class CuentaRepositorio(private val api: ApiService = RetrofitClient.api) {

    suspend fun registrar(nombre: String, correo: String, sedeId: String, clave: String): Result<UsuarioRespuesta> =
        llamar { api.registrar(RegistroRequest(nombre.trim(), correo.trim(), sedeId, clave)) }

    suspend fun iniciarSesion(correo: String, clave: String): Result<UsuarioRespuesta> =
        llamar { api.iniciarSesion(LoginRequest(correo.trim(), clave)) }

    /** Recibe el código que devolvió Microsoft y termina el inicio de sesión. */
    suspend fun iniciarConMicrosoft(codigo: String): Result<UsuarioRespuesta> {
        val token = MicrosoftAuth.obtenerToken(codigo).getOrElse { return Result.failure(it) }
        return llamar { api.iniciarConMicrosoft(MicrosoftRequest(token)) }
    }

    private suspend fun <T> llamar(bloque: suspend () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (e: HttpException) {
            val cuerpo = e.response()?.errorBody()?.string()
            val mensaje = runCatching { Gson().fromJson(cuerpo, MensajeApi::class.java)?.mensaje }.getOrNull()
            Result.failure(Exception(mensaje ?: "Error del servidor (${e.code()})"))
        } catch (e: IOException) {
            Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión a internet."))
        }
}
