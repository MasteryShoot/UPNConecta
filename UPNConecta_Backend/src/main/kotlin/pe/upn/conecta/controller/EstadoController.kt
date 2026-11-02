package pe.upn.conecta.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import pe.upn.conecta.repository.ResenaRepository
import pe.upn.conecta.repository.SedeRepository
import pe.upn.conecta.repository.UsuarioRepository
import javax.sql.DataSource

/** Rutas para comprobar desde un navegador que el backend publicado está vivo. */
@RestController
class EstadoController(
    private val dataSource: DataSource,
    private val sedeRepository: SedeRepository,
    private val usuarioRepository: UsuarioRepository,
    private val resenaRepository: ResenaRepository
) {

    @GetMapping("/")
    fun inicio() = mapOf(
        "servicio" to "UPN Conecta API",
        "alojadoEn" to "Render (Web Service con Docker)",
        "documentacion" to "/swagger-ui.html",
        "estado" to "/api/estado",
        "codigo" to "https://github.com/MasteryShoot/UPNConecta"
    )

    /** Consulta la base de verdad: si responde, la API y PostgreSQL están conectadas. */
    @GetMapping("/api/estado")
    fun estado(): Map<String, Any> {
        val motor = dataSource.connection.use {
            "${it.metaData.databaseProductName} ${it.metaData.databaseProductVersion}"
        }
        return mapOf(
            "estado" to "UP",
            "baseDatos" to motor,
            "sedes" to sedeRepository.count(),
            "usuarios" to usuarioRepository.count(),
            "resenas" to resenaRepository.count()
        )
    }
}
