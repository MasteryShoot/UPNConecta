package com.example.upnconecta.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Rutas del backend: cuentas (T2) y la HU2.03 "Calificar un área de la sede".
 * Cada operación del CRUD se mapea a un verbo HTTP.
 */
interface ApiService {

    // Cuentas: registro e inicio de sesión
    @POST("auth/registro")
    suspend fun registrar(@Body datos: RegistroRequest): UsuarioRespuesta

    @POST("auth/login")
    suspend fun iniciarSesion(@Body datos: LoginRequest): UsuarioRespuesta

    @POST("auth/microsoft")
    suspend fun iniciarConMicrosoft(@Body datos: MicrosoftRequest): UsuarioRespuesta

    @GET("criterios")
    suspend fun obtenerCriterios(@Query("area") area: String): List<CriterioApi>

    // CREATE
    @POST("resenas")
    suspend fun registrarResena(@Body resena: ResenaRequest): ResenaApi

    // READ: devuelve 404 si el usuario aún no evaluó, por eso se usa Response
    @GET("resenas/mia")
    suspend fun obtenerMiResena(
        @Query("usuario") codigoUsuario: String,
        @Query("sede") codigoSede: String,
        @Query("area") area: String
    ): Response<ResenaApi>

    @GET("resenas")
    suspend fun obtenerOpiniones(
        @Query("sede") codigoSede: String,
        @Query("area") area: String
    ): List<OpinionApi>

    // UPDATE
    @PUT("resenas/{idResena}")
    suspend fun actualizarResena(
        @Path("idResena") idResena: Int,
        @Body resena: ResenaRequest
    ): ResenaApi

    // DELETE
    @DELETE("resenas/{idResena}")
    suspend fun eliminarResena(
        @Path("idResena") idResena: Int,
        @Query("usuario") codigoUsuario: String
    ): MensajeApi
}
