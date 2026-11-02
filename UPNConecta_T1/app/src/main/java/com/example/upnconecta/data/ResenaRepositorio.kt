package com.example.upnconecta.data

import com.example.upnconecta.data.remote.ApiService
import com.example.upnconecta.data.remote.CalificacionApi
import com.example.upnconecta.data.remote.CriterioApi
import com.example.upnconecta.data.remote.MensajeApi
import com.example.upnconecta.data.remote.OpinionApi
import com.example.upnconecta.data.remote.ResenaApi
import com.example.upnconecta.data.remote.ResenaRequest
import com.example.upnconecta.data.remote.RetrofitClient
import com.example.upnconecta.data.remote.llamarApi
import retrofit2.HttpException

/** Repositorio de reseñas: centraliza las llamadas de Retrofit (el autor va en el token). */
class ResenaRepositorio(private val api: ApiService = RetrofitClient.api) {

    suspend fun criterios(area: String): Result<List<CriterioApi>> =
        llamarApi { api.obtenerCriterios(area) }

    /** Devuelve null si el usuario todavía no evaluó el área en el ciclo. */
    suspend fun miResena(sedeId: String, areaId: String): Result<ResenaApi?> =
        llamarApi {
            val respuesta = api.obtenerMiResena(sedeId, areaId)
            when {
                respuesta.isSuccessful -> respuesta.body()
                respuesta.code() == 404 -> null
                else -> throw HttpException(respuesta)
            }
        }

    suspend fun opiniones(sedeId: String, areaId: String): Result<List<OpinionApi>> =
        llamarApi { api.obtenerOpiniones(sedeId, areaId) }

    suspend fun registrar(datos: DatosResena): Result<ResenaApi> =
        llamarApi { api.registrarResena(datos.aRequest()) }

    suspend fun actualizar(idResena: Int, datos: DatosResena): Result<ResenaApi> =
        llamarApi { api.actualizarResena(idResena, datos.aRequest()) }

    suspend fun eliminar(idResena: Int): Result<MensajeApi> =
        llamarApi { api.eliminarResena(idResena) }
}

/** Datos del formulario de calificación que se envían al backend. */
data class DatosResena(
    val sedeId: String,
    val areaId: String,
    val experiencia: Int,
    val comentario: String,
    val anonimo: Boolean,
    val sinReciclaje: Boolean,
    val puntajes: Map<Int, Int>   // idCriterio -> estrellas
) {
    fun aRequest() = ResenaRequest(
        codigoSede = sedeId,
        area = areaId,
        experiencia = experiencia,
        comentario = comentario.trim().ifBlank { null },
        esAnonima = anonimo,
        sinReciclaje = sinReciclaje,
        calificaciones = puntajes.map { (id, puntaje) -> CalificacionApi(id, puntaje) }
    )
}
