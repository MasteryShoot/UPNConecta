package com.example.upnconecta.data.remote

/*
 * Modelos que viajan en JSON entre la app y el backend (Spring Boot).
 * Gson los convierte automáticamente; los nombres deben coincidir con la API.
 */

data class CriterioApi(
    val idCriterio: Int,
    val area: String,
    val nombre: String
)

data class UsuarioApi(
    val codigo: String,
    val correo: String,
    val nombreCompleto: String
)

data class CalificacionApi(
    val idCriterio: Int,
    val puntaje: Int,
    val nombre: String? = null
)

/** Cuerpo del POST y del PUT de /resenas. */
data class ResenaRequest(
    val usuario: UsuarioApi,
    val codigoSede: String,
    val area: String,
    val experiencia: Int,
    val comentario: String?,
    val esAnonima: Boolean,
    val sinReciclaje: Boolean,
    val calificaciones: List<CalificacionApi>
)

/** Respuesta con la evaluación completa del usuario. */
data class ResenaApi(
    val idResena: Int,
    val codigoUsuario: String,
    val codigoSede: String,
    val area: String,
    val periodo: String,
    val experiencia: Int,
    val comentario: String?,
    val esAnonima: Boolean,
    val sinReciclaje: Boolean,
    val fecha: String,
    val promedio: Double,
    val calificaciones: List<CalificacionApi>
)

/** Opinión pública de un área (sin datos personales del evaluador). */
data class OpinionApi(
    val idResena: Int,
    val autor: String,
    val rol: String,
    val comentario: String?,
    val promedio: Double,
    val fecha: String
)

data class MensajeApi(val mensaje: String)
