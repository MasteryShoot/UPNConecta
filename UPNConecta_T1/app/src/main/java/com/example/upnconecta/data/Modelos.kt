package com.example.upnconecta.data

/** Sede (campus) de la universidad. [imagen] es el nombre del drawable con su foto. */
data class Sede(
    val id: String,
    val nombre: String,
    val ciudad: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val imagen: String
)

/**
 * Área evaluable de una sede (HU2.02 - HU2.04 y HU5.01).
 * Cada área se califica por criterios con una escala de 1 a 5 estrellas.
 */
data class Area(
    val id: String,
    val nombre: String,
    val categoria: String,
    val descripcion: String,
    val criterios: List<String>
)

/** Docente que puede ser evaluado por los alumnos (Epic 3). */
data class Docente(
    val codigo: String,
    val nombre: String,
    val curso: String,
    val facultad: String,
    val sedeId: String
)

/** Opinión escrita por un usuario sobre un área o un docente. */
data class Opinion(
    val autor: String,
    val rol: String,
    val texto: String,
    val puntaje: Int,
    val etiqueta: String,
    val fecha: String
)

/** Resultado consolidado de un área de una sede en el ciclo actual. */
data class ResumenArea(
    val promedioCriterios: List<Float>,
    val promedioAlumnos: Float,
    val promedioDocentes: Float,
    val totalEvaluaciones: Int
) {
    val promedioGeneral: Float
        get() = if (promedioCriterios.isEmpty()) 0f else promedioCriterios.average().toFloat()

    val sinDatos: Boolean
        get() = totalEvaluaciones == 0
}

/** Sorteo de fin de ciclo (RN12: se priorizan premios digitales o reutilizables). */
data class Sorteo(
    val id: String,
    val titulo: String,
    val detalle: String,
    val cierre: String,
    val inscritos: Int
)

/** Usuario registrado en el dispositivo. */
data class Usuario(
    val nombre: String,
    val correo: String,
    val sedeId: String
) {
    val rol: String
        get() = Roles.rolDeCorreo(correo) ?: Roles.ALUMNO

    val codigo: String
        get() = correo.substringBefore("@").uppercase()

    val primerNombre: String
        get() = nombre.trim().substringBefore(" ")
}

object Roles {
    const val ALUMNO = "Alumno"
    const val DOCENTE = "Docente"
    const val DOMINIO = "@upn.pe"

    // Los alumnos usan su código como correo: N00329320@upn.pe
    private val codigoAlumno = Regex("^[nN]\\d{8}$")

    /**
     * RN01 y RN02: solo se aceptan correos @upn.pe y el rol se asigna según el correo.
     * Devuelve null si el correo no es institucional.
     */
    fun rolDeCorreo(correo: String): String? {
        val limpio = correo.trim()
        if (!limpio.endsWith(DOMINIO, ignoreCase = true)) return null
        val usuario = limpio.substringBefore("@")
        if (usuario.isBlank()) return null
        return if (codigoAlumno.matches(usuario)) ALUMNO else DOCENTE
    }
}
