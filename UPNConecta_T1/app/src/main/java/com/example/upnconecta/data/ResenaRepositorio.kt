package com.example.upnconecta.data

import com.example.upnconecta.data.remote.ApiService
import com.example.upnconecta.data.remote.CalificacionApi
import com.example.upnconecta.data.remote.CriterioApi
import com.example.upnconecta.data.remote.MensajeApi
import com.example.upnconecta.data.remote.OpinionApi
import com.example.upnconecta.data.remote.ResenaApi
import com.example.upnconecta.data.remote.ResenaRequest
import com.example.upnconecta.data.remote.RetrofitClient
import com.example.upnconecta.data.remote.UsuarioApi
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/**
 * Repositorio de reseñas: centraliza las llamadas de Retrofit y
 * convierte los errores HTTP en mensajes que se pueden mostrar al usuario.
 */
class ResenaRepositorio(private val api: ApiService = RetrofitClient.api) {

    suspend fun criterios(area: String): Result<List<CriterioApi>> =
        llamar { api.obtenerCriterios(area) }

    /** Devuelve null si el usuario todavía no evaluó el área en el ciclo. */
    suspend fun miResena(usuario: Usuario, sedeId: String, areaId: String): Result<ResenaApi?> =
        llamar {
            val respuesta = api.obtenerMiResena(usuario.codigo, sedeId, areaId)
            when {
                respuesta.isSuccessful -> respuesta.body()
                respuesta.code() == 404 -> null
                else -> throw HttpException(respuesta)
            }
        }

    suspend fun opiniones(sedeId: String, areaId: String): Result<List<OpinionApi>> =
        llamar { api.obtenerOpiniones(sedeId, areaId) }

    suspend fun registrar(datos: DatosResena): Result<ResenaApi> =
        llamar { api.registrarResena(datos.aRequest()) }

    suspend fun actualizar(idResena: Int, datos: DatosResena): Result<ResenaApi> =
        llamar { api.actualizarResena(idResena, datos.aRequest()) }

    suspend fun eliminar(idResena: Int, usuario: Usuario): Result<MensajeApi> =
        llamar { api.eliminarResena(idResena, usuario.codigo) }

    private suspend fun <T> llamar(bloque: suspend () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (e: HttpException) {
            Result.failure(Exception(mensajeDelServidor(e)))
        } catch (e: IOException) {
            Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión a internet."))
        }

    // El backend responde los errores como {"mensaje": "..."}
    private fun mensajeDelServidor(e: HttpException): String {
        val cuerpo = e.response()?.errorBody()?.string()
        val mensaje = try {
            Gson().fromJson(cuerpo, MensajeApi::class.java)?.mensaje
        } catch (_: Exception) {
            null
        }
        return mensaje ?: "Error del servidor (${e.code()})"
    }
}

/** Datos del formulario de calificación que se envían al backend. */
data class DatosResena(
    val usuario: Usuario,
    val sedeId: String,
    val areaId: String,
    val experiencia: Int,
    val comentario: String,
    val anonimo: Boolean,
    val sinReciclaje: Boolean,
    val puntajes: Map<Int, Int>   // idCriterio -> estrellas
) {
    fun aRequest() = ResenaRequest(
        usuario = UsuarioApi(usuario.codigo, usuario.correo, usuario.nombre),
        codigoSede = sedeId,
        area = areaId,
        experiencia = experiencia,
        comentario = comentario.trim().ifBlank { null },
        esAnonima = anonimo,
        sinReciclaje = sinReciclaje,
        calificaciones = puntajes.map { (id, puntaje) -> CalificacionApi(id, puntaje) }
    )
}
