package pe.upn.conecta.service

/**
 * Mismas reglas de moderación que la app (RN06 y RN09), repetidas en el servidor
 * porque no se debe confiar solo en la validación del cliente.
 */
object Moderacion {

    const val MIN_CARACTERES = 50
    const val MAX_CARACTERES = 500

    private val palabrasOfensivas = listOf(
        "idiota", "imbecil", "imbécil", "estupido", "estúpido", "tarado",
        "inutil", "inútil", "basura", "mierda", "carajo", "cojudo", "huevon", "huevón"
    )

    private val numeroLargo = Regex("\\d{8,}")
    private val celular = Regex("\\b9\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{3}\\b")
    private val correo = Regex("[\\w.+-]+@[\\w-]+\\.[\\w.]+")

    /** Devuelve el mensaje de error o null si el comentario es válido (el comentario es opcional). */
    fun validarComentario(texto: String): String? {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return null
        if (limpio.length < MIN_CARACTERES) {
            return "El comentario debe tener al menos $MIN_CARACTERES caracteres"
        }
        if (limpio.length > MAX_CARACTERES) {
            return "El comentario no puede superar los $MAX_CARACTERES caracteres"
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
