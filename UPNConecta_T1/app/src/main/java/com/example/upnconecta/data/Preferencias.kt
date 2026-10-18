package com.example.upnconecta.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.security.MessageDigest

/**
 * Manejo de SharedPreferences de UPN Conecta.
 *
 * Aquí se guarda todo lo que la app necesita recordar en el dispositivo:
 * cuentas registradas, sesión, sede elegida, evaluaciones, comentarios y sorteos.
 * Las claves usan "|" como separador porque los ids tienen guiones bajos.
 */
class Preferencias(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(NOMBRE_ARCHIVO, Context.MODE_PRIVATE)

    private val ciclo = DatosDemo.CICLO_ACTUAL

    // ---------------------------------------------------------------- Cuentas

    /** RN03: una sola cuenta por correo institucional. */
    fun existeUsuario(correo: String): Boolean =
        prefs.contains(claveUsuario(correo, "nombre"))

    fun registrarUsuario(nombre: String, correo: String, sedeId: String, contrasena: String) {
        prefs.edit {
            putString(claveUsuario(correo, "nombre"), nombre.trim())
            putString(claveUsuario(correo, "sede"), sedeId)
            // La contraseña no se guarda en texto plano, solo su hash SHA-256
            putString(claveUsuario(correo, "clave"), hash(contrasena))
        }
    }

    fun credencialesValidas(correo: String, contrasena: String): Boolean =
        prefs.getString(claveUsuario(correo, "clave"), null) == hash(contrasena)

    fun usuario(correo: String): Usuario? {
        val nombre = prefs.getString(claveUsuario(correo, "nombre"), null) ?: return null
        val sede = prefs.getString(claveUsuario(correo, "sede"), null) ?: DatosDemo.sedes.first().id
        return Usuario(nombre, normalizar(correo), sede)
    }

    // ----------------------------------------------------------------- Sesión

    fun iniciarSesion(correo: String, recordar: Boolean) {
        prefs.edit {
            putString(SESION_CORREO, normalizar(correo))
            putBoolean(SESION_RECORDAR, recordar)
            putString(ULTIMO_CORREO, if (recordar) normalizar(correo) else "")
        }
    }

    fun cerrarSesion() {
        prefs.edit {
            remove(SESION_CORREO)
            putBoolean(SESION_RECORDAR, false)
        }
    }

    /** Usuario con la sesión abierta (o null si nadie inició sesión). */
    fun usuarioActual(): Usuario? {
        val correo = prefs.getString(SESION_CORREO, null) ?: return null
        return usuario(correo)
    }

    /** El Splash solo salta el login si el usuario marcó "Recordarme". */
    fun sesionRecordada(): Boolean =
        prefs.getBoolean(SESION_RECORDAR, false) && usuarioActual() != null

    fun ultimoCorreo(): String = prefs.getString(ULTIMO_CORREO, "") ?: ""

    // ------------------------------------------------------------------- Sede

    fun sedeActual(): String = usuarioActual()?.sedeId ?: DatosDemo.sedes.first().id

    fun guardarSede(sedeId: String) {
        val correo = prefs.getString(SESION_CORREO, null) ?: return
        prefs.edit { putString(claveUsuario(correo, "sede"), sedeId) }
    }

    // ---------------------------------------------------- Evaluación de áreas

    /** HU2.03: una evaluación por área, sede y ciclo. */
    fun yaEvaluoArea(sedeId: String, areaId: String): Boolean =
        prefs.contains(claveEvaluacion(sedeId, areaId))

    fun guardarEvaluacionArea(
        sedeId: String,
        areaId: String,
        puntajes: List<Int>,
        experiencia: Int,
        sinReciclaje: Boolean
    ) {
        val valor = puntajes.joinToString(",") + ";" + experiencia + ";" + System.currentTimeMillis()
        prefs.edit {
            putString(claveEvaluacion(sedeId, areaId), valor)
            // HU5.01: se registra una alerta si la sede no tiene contenedores visibles
            if (sinReciclaje) {
                putInt("alerta|$sedeId", alertasReciclaje(sedeId) + 1)
            }
        }
    }

    /** Copia local de la evaluación eliminada en el backend (DELETE). */
    fun eliminarEvaluacionArea(sedeId: String, areaId: String) {
        prefs.edit {
            remove(claveEvaluacion(sedeId, areaId))
            remove("comentu|${correoSesion()}|$ciclo|$sedeId|$areaId")
        }
    }

    /** Registra si la evaluación enviada al backend tiene comentario (para el contador del perfil). */
    fun marcarComentarioArea(sedeId: String, areaId: String, tieneComentario: Boolean) {
        val clave = "comentu|${correoSesion()}|$ciclo|$sedeId|$areaId"
        prefs.edit { if (tieneComentario) putBoolean(clave, true) else remove(clave) }
    }

    fun puntajesUsuario(sedeId: String, areaId: String): List<Int>? {
        val valor = prefs.getString(claveEvaluacion(sedeId, areaId), null) ?: return null
        return valor.substringBefore(";").split(",").mapNotNull { it.toIntOrNull() }
    }

    fun alertasReciclaje(sedeId: String): Int = prefs.getInt("alerta|$sedeId", 0)

    /** Cantidad de áreas que el usuario evaluó en la sede (sirve para los boletos del sorteo). */
    fun areasEvaluadas(sedeId: String): Int =
        DatosDemo.areas.count { yaEvaluoArea(sedeId, it.id) }

    /** Todas las evaluaciones del usuario en el ciclo, de la más reciente a la más antigua. */
    fun misEvaluaciones(): List<MiEvaluacion> {
        val correo = correoSesion() ?: return emptyList()
        val prefijo = "eval|$correo|$ciclo|"
        val deAreas = prefs.all.mapNotNull { (clave, valor) ->
            if (!clave.startsWith(prefijo) || valor !is String) return@mapNotNull null
            val partes = clave.removePrefix(prefijo).split("|")
            if (partes.size != 2) return@mapNotNull null
            val datos = valor.split(";")
            val puntajes = datos[0].split(",").mapNotNull { it.toIntOrNull() }
            MiEvaluacion(
                titulo = DatosDemo.area(partes[1]).nombre,
                subtitulo = "UPN " + DatosDemo.sede(partes[0]).nombre,
                promedio = if (puntajes.isEmpty()) 0f else puntajes.average().toFloat(),
                fecha = datos.getOrNull(2)?.toLongOrNull() ?: 0L
            )
        }
        return (deAreas + misEvaluacionesDocentes()).sortedByDescending { it.fecha }
    }

    /**
     * Resultado del área combinando los datos del ciclo con la evaluación
     * del usuario (si ya la envió). Separa alumnos y docentes (HU4.01).
     */
    fun resumenArea(sedeId: String, areaId: String): ResumenArea {
        val base = DatosDemo.puntajesBase(sedeId, areaId)
        val votos = DatosDemo.votosBase(sedeId, areaId)
        val propios = puntajesUsuario(sedeId, areaId)

        if (votos == 0 && propios == null) {
            return ResumenArea(emptyList(), 0f, 0f, 0)
        }

        val criterios = base.mapIndexed { i, valor ->
            val mio = propios?.getOrNull(i)?.toFloat()
            if (mio == null) valor else (valor * votos + mio) / (votos + 1)
        }
        val general = criterios.average().toFloat()
        // Diferencia simulada entre la percepción de alumnos y docentes (entre -1.2 y +1.2)
        val diferencia = ((kotlin.math.abs((sedeId + areaId).hashCode()) % 13) - 6) / 5f
        return ResumenArea(
            promedioCriterios = criterios,
            promedioAlumnos = (general - diferencia / 2).coerceIn(1f, 5f),
            promedioDocentes = (general + diferencia / 2).coerceIn(1f, 5f),
            totalEvaluaciones = votos + if (propios != null) 1 else 0
        )
    }

    // ------------------------------------------------- Evaluación de docentes

    /** HU3.02: un alumno solo puede evaluar una vez a cada docente por semestre. */
    fun yaEvaluoDocente(codigo: String): Boolean =
        prefs.contains(claveDocente(codigo))

    fun guardarEvaluacionDocente(codigo: String, puntajes: List<Int>) {
        val valor = puntajes.joinToString(",") + ";" + System.currentTimeMillis()
        prefs.edit { putString(claveDocente(codigo), valor) }
    }

    private fun misEvaluacionesDocentes(): List<MiEvaluacion> {
        val correo = correoSesion() ?: return emptyList()
        val prefijo = "evaldoc|$correo|$ciclo|"
        return prefs.all.mapNotNull { (clave, valor) ->
            if (!clave.startsWith(prefijo) || valor !is String) return@mapNotNull null
            val docente = DatosDemo.docente(clave.removePrefix(prefijo)) ?: return@mapNotNull null
            val datos = valor.split(";")
            val puntajes = datos[0].split(",").mapNotNull { it.toIntOrNull() }
            MiEvaluacion(
                titulo = docente.nombre,
                subtitulo = docente.curso,
                promedio = if (puntajes.isEmpty()) 0f else puntajes.average().toFloat(),
                fecha = datos.getOrNull(1)?.toLongOrNull() ?: 0L
            )
        }
    }

    // ------------------------------------------------------------ Comentarios

    /** RN04: una opinión por área y sede en cada semestre. */
    fun yaComentoArea(sedeId: String, areaId: String): Boolean =
        prefs.getBoolean("comentu|${correoSesion()}|$ciclo|$sedeId|$areaId", false)

    fun publicarComentarioArea(sedeId: String, areaId: String, opinion: Opinion) {
        agregarOpinion("coment|$sedeId|$areaId", opinion)
        prefs.edit { putBoolean("comentu|${correoSesion()}|$ciclo|$sedeId|$areaId", true) }
    }

    fun comentariosArea(sedeId: String, areaId: String): List<Opinion> =
        leerOpiniones("coment|$sedeId|$areaId")

    fun publicarComentarioDocente(codigo: String, opinion: Opinion) =
        agregarOpinion("comentdoc|$codigo", opinion)

    fun comentariosDocente(codigo: String): List<Opinion> =
        leerOpiniones("comentdoc|$codigo")

    fun totalComentariosUsuario(): Int {
        val prefijo = "comentu|${correoSesion()}|"
        return prefs.all.keys.count { it.startsWith(prefijo) }
    }

    // Cada opinión se guarda en una sola línea; los campos van separados con \u001F
    private fun agregarOpinion(clave: String, opinion: Opinion) {
        val linea = listOf(
            opinion.autor, opinion.rol, opinion.texto.replace("\n", " "),
            opinion.puntaje.toString(), opinion.etiqueta, System.currentTimeMillis().toString()
        ).joinToString(SEP_CAMPO) { it.replace(SEP_CAMPO, " ").replace(SEP_REGISTRO, " ") }
        val actual = prefs.getString(clave, "") ?: ""
        val nuevo = if (actual.isEmpty()) linea else linea + SEP_REGISTRO + actual
        prefs.edit { putString(clave, nuevo) }
    }

    private fun leerOpiniones(clave: String): List<Opinion> {
        val guardado = prefs.getString(clave, "") ?: ""
        if (guardado.isEmpty()) return emptyList()
        return guardado.split(SEP_REGISTRO).mapNotNull { linea ->
            val c = linea.split(SEP_CAMPO)
            if (c.size < 6) return@mapNotNull null
            Opinion(
                autor = c[0], rol = c[1], texto = c[2],
                puntaje = c[3].toIntOrNull() ?: 0, etiqueta = c[4],
                fecha = fechaRelativa(c[5].toLongOrNull() ?: 0L)
            )
        }
    }

    // ---------------------------------------------------------------- Sorteos

    fun estaInscrito(sorteoId: String): Boolean =
        prefs.getBoolean("sorteo|${correoSesion()}|$sorteoId", false)

    fun inscribirSorteo(sorteoId: String) {
        prefs.edit { putBoolean("sorteo|${correoSesion()}|$sorteoId", true) }
    }

    fun totalSorteosInscritos(): Int =
        DatosDemo.sorteos.count { estaInscrito(it.id) }

    // ------------------------------------------------------------- Utilidades

    private fun correoSesion(): String? = prefs.getString(SESION_CORREO, null)

    private fun claveUsuario(correo: String, campo: String) = "usuario|${normalizar(correo)}|$campo"

    private fun claveEvaluacion(sedeId: String, areaId: String) =
        "eval|${correoSesion()}|$ciclo|$sedeId|$areaId"

    private fun claveDocente(codigo: String) = "evaldoc|${correoSesion()}|$ciclo|$codigo"

    private fun normalizar(correo: String) = correo.trim().lowercase()

    private fun hash(texto: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(texto.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val NOMBRE_ARCHIVO = "upn_conecta_prefs"
        private const val SESION_CORREO = "sesion_correo"
        private const val SESION_RECORDAR = "sesion_recordar"
        private const val ULTIMO_CORREO = "ultimo_correo"
        private const val SEP_CAMPO = "\u001F"
        private const val SEP_REGISTRO = "\u001E"

        fun fechaRelativa(milis: Long): String {
            if (milis <= 0L) return ""
            val dias = ((System.currentTimeMillis() - milis) / 86_400_000L).toInt()
            return when {
                dias <= 0 -> "Hoy"
                dias == 1 -> "Ayer"
                else -> "Hace $dias días"
            }
        }
    }
}

/** Fila de "Mis calificaciones recientes" del perfil. */
data class MiEvaluacion(
    val titulo: String,
    val subtitulo: String,
    val promedio: Float,
    val fecha: Long
)
