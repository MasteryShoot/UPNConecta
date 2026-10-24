package pe.upn.conecta.service

/** Regla para deducir el rol a partir del código del correo institucional (RN02). */
object ReglasUsuario {

    // UPN: N00329320 | UPC y UTP: U + dígitos | Universidad de Lima: solo dígitos
    private val codigosAlumno = listOf(
        Regex("^N\\d{8}$"),
        Regex("^U\\d{6,}$"),
        Regex("^\\d{8}$")
    )

    fun rolDeCodigo(codigo: String): String =
        if (codigosAlumno.any { it.matches(codigo.uppercase()) }) "ALUMNO" else "DOCENTE"
}
