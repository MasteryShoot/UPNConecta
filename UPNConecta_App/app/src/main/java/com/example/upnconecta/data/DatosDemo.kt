package com.example.upnconecta.data

import kotlin.math.abs

/**
 * Datos de ejemplo de la T1. En la siguiente entrega estos datos
 * vendrán de Firebase (Firestore); por ahora se cargan localmente.
 */
object DatosDemo {

    const val CICLO_ACTUAL = "2026-2"

    // Ubicación referencial de cada campus (direcciones publicadas por la UPN)
    val sedes = listOf(
        Sede("los_olivos", "Los Olivos", "Lima", "Av. Alfredo Mendiola 6062", -11.9942, -77.0617, "sede_los_olivos"),
        Sede("brena", "Breña", "Lima", "Av. Tingo María 1122", -12.0625, -77.0527, "sede_brena"),
        Sede("comas", "Comas", "Lima", "Av. Andrés Belaunde cdra. 10", -11.9395, -77.0560, "sede_comas"),
        Sede("sjl", "San Juan de Lurigancho", "Lima", "Av. El Sol 461", -12.0262, -77.0040, "sede_sjl"),
        Sede("chorrillos", "Chorrillos", "Lima", "Av. Guardia Peruana 890", -12.1730, -77.0130, "sede_chorrillos"),
        Sede("el_molino", "Trujillo El Molino", "Trujillo", "Av. El Ejército 920", -8.1030, -79.0307, "sede_el_molino"),
        Sede("san_isidro", "Trujillo San Isidro", "Trujillo", "Urb. Deán Saavedra, El Cortijo", -8.0870, -79.0340, "sede_san_isidro"),
        Sede("cajamarca", "Cajamarca", "Cajamarca", "Vía de Evitamiento Norte cdra. 15", -7.1476, -78.5080, "sede_cajamarca")
    )

    // Áreas y criterios según las historias de usuario de la PC 04
    val areas = listOf(
        Area(
            "infraestructura", "Infraestructura", "ESTADO FÍSICO",
            "Pabellones, aulas y laboratorios de la sede",
            listOf("Pabellones", "Aulas", "Laboratorios")
        ),
        Area(
            "limpieza", "Limpieza", "HIGIENE",
            "Baños, áreas comunes y cafeterías",
            listOf("Baños", "Áreas comunes", "Cafeterías")
        ),
        Area(
            "gestion", "Gestión administrativa", "ATENCIÓN",
            "Secretaría, bienestar y soporte técnico",
            listOf("Secretaría", "Bienestar", "Soporte técnico")
        ),
        Area(
            "sostenibilidad", "Sostenibilidad", "ODS 12",
            "Reciclaje y segregación de residuos",
            listOf("Tachos de reciclaje", "Puntos de segregación", "Señalización ambiental")
        )
    )

    val docentes = listOf(
        Docente("D001", "Carlos Gómez Ríos", "Programación Orientada a Objetos", "Ingeniería", "los_olivos"),
        Docente("D002", "Ana Torres Salazar", "Base de Datos", "Ingeniería", "los_olivos"),
        Docente("D003", "Luis Ramírez Vega", "Desarrollo de Aplicaciones Móviles", "Ingeniería", "los_olivos"),
        Docente("D004", "María García Paredes", "Cálculo I", "Ingeniería", "brena"),
        Docente("D005", "Jorge García Luna", "Contabilidad General", "Negocios", "comas"),
        Docente("D006", "Rosa Huamán Quispe", "Psicología del Desarrollo", "Salud", "sjl"),
        Docente("D007", "Pedro Castillo Ñique", "Derecho Constitucional", "Derecho", "el_molino"),
        Docente("D008", "Lucía Mendoza Ortiz", "Arquitectura de Software", "Ingeniería", "cajamarca")
    )

    val sorteos = listOf(
        Sorteo("kit_eco", "Kit Eco UPN", "Tomatodo térmico y bolsa reutilizable", "Termina el 30 de noviembre", 183),
        Sorteo("vale_cafeteria", "Vale digital S/ 50", "Consumo en la cafetería de tu sede", "Termina el 15 de diciembre", 412),
        Sorteo("cuadernos", "Pack de cuadernos reciclados", "3 cuadernos de papel 100 % reciclado", "Termina el 15 de diciembre", 96)
    )

    val ganadores = listOf(
        "Gisela S. (N0014****)" to "Audífonos inalámbricos",
        "Renato L. (N0019****)" to "Vale digital S/ 50"
    )

    fun sede(id: String?): Sede = sedes.firstOrNull { it.id == id } ?: sedes.first()

    fun area(id: String?): Area = areas.firstOrNull { it.id == id } ?: areas.first()

    fun docente(codigo: String?): Docente? = docentes.firstOrNull { it.codigo == codigo }

    // Puntajes base de cada criterio (antes de sumar las evaluaciones del usuario)
    private val baseArea = mapOf(
        "infraestructura" to listOf(4.3f, 3.9f, 3.4f),
        "limpieza" to listOf(2.9f, 3.8f, 3.5f),
        "gestion" to listOf(3.6f, 4.2f, 3.1f),
        "sostenibilidad" to listOf(2.6f, 2.4f, 3.0f)
    )

    /** Cantidad simulada de evaluaciones del ciclo por sede y área. */
    fun votosBase(sedeId: String, areaId: String): Int {
        // Chorrillos aún no tiene evaluaciones de sostenibilidad (escenario "sin datos" de HU4.01)
        if (sedeId == "chorrillos" && areaId == "sostenibilidad") return 0
        return 40 + abs((sedeId + areaId).hashCode()) % 180
    }

    /** Variación pequeña por sede para que cada campus tenga resultados distintos. */
    fun variacion(sedeId: String, indice: Int): Float {
        val valor = abs(sedeId.hashCode() * 31 + indice * 17) % 11
        return (valor - 5) / 10f
    }

    // Ajuste general de cada campus para que los resultados no sean iguales entre sedes
    private val ajusteSede = mapOf(
        "los_olivos" to 0.4f, "brena" to -0.3f, "comas" to -0.6f, "sjl" to 0.1f,
        "chorrillos" to 0.3f, "el_molino" to 0.6f, "san_isidro" to -0.2f, "cajamarca" to 0.8f
    )

    fun puntajesBase(sedeId: String, areaId: String): List<Float> {
        val base = baseArea[areaId] ?: listOf(3f, 3f, 3f)
        val ajuste = ajusteSede[sedeId] ?: 0f
        return base.mapIndexed { i, valor ->
            (valor + ajuste + variacion(sedeId, i + areaId.length)).coerceIn(1f, 5f)
        }
    }

    /** Opiniones de ejemplo que se muestran en el detalle de cada área. */
    fun opinionesBase(areaId: String): List<Opinion> = when (areaId) {
        "infraestructura" -> listOf(
            Opinion("Milagros R.", Roles.ALUMNO, "Los laboratorios del pabellón C tienen buenas computadoras, pero faltan enchufes en varias aulas.", 4, "Laboratorios", "Hace 2 días"),
            Opinion("Anónimo", Roles.ALUMNO, "Los ascensores del pabellón B pasan mucho tiempo en mantenimiento y en hora punta es difícil subir.", 3, "Pabellones", "Hace 5 días")
        )
        "limpieza" -> listOf(
            Opinion("Carlos D.", Roles.ALUMNO, "Los baños del segundo piso casi siempre están sin papel después de las 6 pm.", 2, "Baños", "Ayer"),
            Opinion("Prof. Ana T.", Roles.DOCENTE, "Las áreas comunes se mantienen limpias durante el día, buen trabajo del personal.", 4, "Áreas comunes", "Hace 3 días")
        )
        "gestion" -> listOf(
            Opinion("Anónimo", Roles.ALUMNO, "En secretaría me resolvieron el trámite de constancia en el mismo día, muy rápido.", 5, "Secretaría", "Hace 1 día"),
            Opinion("Jesús M.", Roles.ALUMNO, "Soporte técnico demora en responder cuando falla el aula virtual.", 2, "Soporte técnico", "Hace 4 días")
        )
        else -> listOf(
            Opinion("Vanessa B.", Roles.ALUMNO, "Hay tachos de colores en el patio, pero casi nadie separa bien los residuos.", 3, "Puntos de segregación", "Hace 2 días"),
            Opinion("Prof. Luis R.", Roles.DOCENTE, "Faltan carteles que expliquen qué va en cada tacho de reciclaje.", 2, "Señalización ambiental", "Hace 6 días")
        )
    }

    fun opinionesDocenteBase(): List<Opinion> = listOf(
        Opinion("Anónimo", Roles.ALUMNO, "Explica con ejemplos reales y responde las dudas en el foro del aula virtual.", 5, "Metodología", "Hace 3 días"),
        Opinion("Jeremy W.", Roles.ALUMNO, "Siempre llega puntual y es respetuoso, aunque avanza rápido en algunos temas.", 4, "Puntualidad", "Hace 1 semana")
    )
}
