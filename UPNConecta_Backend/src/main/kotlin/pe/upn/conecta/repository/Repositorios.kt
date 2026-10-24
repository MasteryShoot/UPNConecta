package pe.upn.conecta.repository

import org.springframework.data.jpa.repository.JpaRepository
import pe.upn.conecta.model.Criterio
import pe.upn.conecta.model.Resena
import pe.upn.conecta.model.Sede
import pe.upn.conecta.model.Universidad
import pe.upn.conecta.model.Usuario

// Spring Data JPA genera las consultas parametrizadas a partir del nombre del método

interface UniversidadRepository : JpaRepository<Universidad, Int> {
    fun findByDominioCorreo(dominioCorreo: String): Universidad?
}

interface SedeRepository : JpaRepository<Sede, Int> {
    fun findByCodigo(codigo: String): Sede?
}

interface UsuarioRepository : JpaRepository<Usuario, String> {
    fun findByCorreo(correo: String): Usuario?
}

interface CriterioRepository : JpaRepository<Criterio, Int> {
    fun findByAreaOrderByOrden(area: String): List<Criterio>
}

interface ResenaRepository : JpaRepository<Resena, Int> {
    fun findByUsuarioCodigoAndSedeCodigoAndAreaAndPeriodo(
        codigoUsuario: String, codigoSede: String, area: String, periodo: String
    ): Resena?

    fun findBySedeCodigoAndAreaAndPeriodoOrderByFechaDesc(
        codigoSede: String, area: String, periodo: String
    ): List<Resena>
}
