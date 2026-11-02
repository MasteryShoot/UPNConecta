package com.example.upnconecta.data

/** Quita tildes y pasa a minúsculas para buscar sin importar acentos ("Garcia" = "García"). */
fun String.sinTildes(): String =
    java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        .trim()

/**
 * Reglas de moderación básicas para comentarios (PC 03 y reglas de negocio de la PC 04).
 */
object Moderacion {

    const val MIN_CARACTERES = 50   // RN06
    const val MAX_CARACTERES = 500  // HU2.05

    // RN09: lista corta de palabras no permitidas (se ampliará en el backend)
    private val palabrasOfensivas = listOf(
        "idiota", "imbecil", "imbécil", "estupido", "estúpido", "tarado",
        "inutil", "inútil", "basura", "mierda", "carajo", "cojudo", "huevon", "huevón"
    )

    // RN09: se bloquean datos personales como DNI, celulares o correos
    private val numeroLargo = Regex("\\d{8,}")
    private val celular = Regex("\\b9\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{3}\\b")
    private val correo = Regex("[\\w.+-]+@[\\w-]+\\.[\\w.]+")

    /** Devuelve el mensaje de error o null si el comentario es válido. */
    fun validarComentario(texto: String, obligatorio: Boolean): String? {
        val limpio = texto.trim()
        if (limpio.isEmpty()) {
            return if (obligatorio) "Escribe tu comentario o sugerencia" else null
        }
        if (limpio.length < MIN_CARACTERES) {
            return "El comentario debe tener al menos $MIN_CARACTERES caracteres (llevas ${limpio.length})"
        }
        val minusculas = limpio.lowercase()
        if (palabrasOfensivas.any { Regex("\\b$it\\b").containsMatchIn(minusculas) }) {
            return "Tu comentario contiene lenguaje ofensivo. Por favor, redáctalo con respeto."
        }
        if (numeroLargo.containsMatchIn(limpio) || celular.containsMatchIn(limpio) || correo.containsMatchIn(limpio)) {
            return "No publiques datos personales (DNI, celulares o correos)."
        }
        return null
    }
}
