package com.example.upnconecta.data

import com.example.upnconecta.data.remote.ApiService
import com.example.upnconecta.data.remote.DominioApi
import com.example.upnconecta.data.remote.LoginRequest
import com.example.upnconecta.data.remote.MicrosoftAuth
import com.example.upnconecta.data.remote.MicrosoftRequest
import com.example.upnconecta.data.remote.RegistroRequest
import com.example.upnconecta.data.remote.RetrofitClient
import com.example.upnconecta.data.remote.SedeUsuarioRequest
import com.example.upnconecta.data.remote.UsuarioRespuesta
import com.example.upnconecta.data.remote.llamarApi

/** Ingreso a la app contra el backend: validación por dominio, Microsoft y contraseña. */
class CuentaRepositorio(private val api: ApiService = RetrofitClient.api) {

    suspend fun validarDominio(correo: String): Result<DominioApi> =
        llamarApi { api.validarDominio(correo.trim()) }

    suspend fun registrar(nombre: String, correo: String, sedeId: String, clave: String): Result<UsuarioRespuesta> =
        llamarApi { api.registrar(RegistroRequest(nombre.trim(), correo.trim(), sedeId, clave)) }

    suspend fun iniciarSesion(correo: String, clave: String): Result<UsuarioRespuesta> =
        llamarApi { api.iniciarSesion(LoginRequest(correo.trim(), clave)) }

    /** Recibe el código que devolvió Microsoft y termina el inicio de sesión. */
    suspend fun iniciarConMicrosoft(codigo: String, estado: String?): Result<UsuarioRespuesta> {
        val token = MicrosoftAuth.obtenerToken(codigo, estado).getOrElse { return Result.failure(it) }
        return llamarApi { api.iniciarConMicrosoft(MicrosoftRequest(token)) }
    }

    suspend fun guardarSede(sedeId: String): Result<UsuarioRespuesta> =
        llamarApi { api.guardarSede(SedeUsuarioRequest(sedeId)) }
}
