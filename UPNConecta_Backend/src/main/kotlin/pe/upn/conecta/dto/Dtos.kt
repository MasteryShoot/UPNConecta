package pe.upn.conecta.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

/*
 * Datos de entrada y salida de la API (lo que viaja en JSON).
 * Se separan de las entidades para no exponer columnas internas.
 */

// ------------------------------------------------------------- Entrada

data class UsuarioRequest(
    @field:NotBlank(message = "El código de usuario es obligatorio")
    val codigo: String,
    @field:NotBlank(message = "El correo es obligatorio")
    val correo: String,
    @field:NotBlank(message = "El nombre es obligatorio")
    @field:Size(max = 100)
    val nombreCompleto: String
)

data class CalificacionRequest(
    val idCriterio: Int,
    @field:Min(1, message = "El puntaje mínimo es 1")
    @field:Max(5, message = "El puntaje máximo es 5")
    val puntaje: Int
)

/** Se usa tanto para registrar (POST) como para actualizar (PUT) una reseña. */
data class ResenaRequest(
    @field:Valid
    val usuario: UsuarioRequest,
    @field:NotBlank(message = "La sede es obligatoria")
    val codigoSede: String,
    @field:NotBlank(message = "El área es obligatoria")
    val area: String,
    @field:Min(1, message = "Indica tu experiencia general")
    @field:Max(5)
    val experiencia: Int,
    @field:Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
    val comentario: String? = null,
    val esAnonima: Boolean = false,
    val sinReciclaje: Boolean = false,
    @field:NotEmpty(message = "Califica todos los criterios")
    @field:Valid
    val calificaciones: List<CalificacionRequest>
)

// --------------------------------------------------------------- Salida

data class SedeResponse(
    val idSede: Int,
    val codigo: String,
    val nombre: String,
    val direccion: String
)

data class CriterioResponse(
    val idCriterio: Int,
    val area: String,
    val nombre: String
)

data class CalificacionResponse(
    val idCriterio: Int,
    val nombre: String,
    val puntaje: Int
)

/** Reseña completa: solo se devuelve a su propio autor. */
data class ResenaResponse(
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
    val calificaciones: List<CalificacionResponse>
)

/** Opinión pública de un área: sin correo ni código del evaluador. */
data class OpinionResponse(
    val idResena: Int,
    val autor: String,
    val rol: String,
    val comentario: String?,
    val promedio: Double,
    val fecha: String
)

data class MensajeResponse(val mensaje: String)
