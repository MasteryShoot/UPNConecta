package com.example.upnconecta.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Ajustes del usuario en un archivo de SharedPreferences aparte ("upn_conecta_ajustes").
 *
 * A diferencia de lo visto en clase (guardar la sesión), aquí se guardan
 * preferencias de uso: cómo publicar por defecto, avisos y el método de ingreso.
 * Cada usuario tiene sus propios ajustes (la clave lleva su correo).
 */
class Ajustes(context: Context, private val correo: String) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    /** Si está activo, el check "Publicar como anónimo" aparece marcado. */
    var anonimoPorDefecto: Boolean
        get() = prefs.getBoolean(clave("anonimo"), false)
        set(valor) = prefs.edit { putBoolean(clave("anonimo"), valor) }

    /** Mostrar u ocultar el aviso de sorteos activos en el Home. */
    var avisoSorteos: Boolean
        get() = prefs.getBoolean(clave("aviso_sorteos"), true)
        set(valor) = prefs.edit { putBoolean(clave("aviso_sorteos"), valor) }

    /** "CORREO" o "MICROSOFT": se muestra en el perfil. */
    var metodoIngreso: String
        get() = prefs.getString(clave("metodo"), METODO_CORREO) ?: METODO_CORREO
        set(valor) = prefs.edit { putString(clave("metodo"), valor) }

    private fun clave(nombre: String) = "${correo.lowercase()}|$nombre"

    companion object {
        private const val ARCHIVO = "upn_conecta_ajustes"
        const val METODO_CORREO = "CORREO"
        const val METODO_MICROSOFT = "MICROSOFT"
    }
}
