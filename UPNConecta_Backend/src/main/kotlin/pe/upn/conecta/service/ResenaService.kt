package pe.upn.conecta.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upn.conecta.config.ApiException
import pe.upn.conecta.dto.CalificacionResponse
import pe.upn.conecta.dto.OpinionResponse
import pe.upn.conecta.dto.ResenaRequest
import pe.upn.conecta.dto.ResenaResponse
import pe.upn.conecta.dto.UsuarioRequest
import pe.upn.conecta.model.Criterio
import pe.upn.conecta.model.Resena
import pe.upn.conecta.model.Usuario
import pe.upn.conecta.repository.CriterioRepository
import pe.upn.conecta.repository.ResenaRepository
import pe.upn.conecta.repository.SedeRepository
import pe.upn.conecta.repository.UniversidadRepository
import pe.upn.conecta.repository.UsuarioRepository
import java.time.LocalDateTime

/**
 * Reglas de negocio de la HU2.03 "Calificar un área de la sede".
 * Registrar, consultar, actualizar y eliminar la evaluación del ciclo actual.
 */
@Service
class ResenaService(
    private val resenaRepository: ResenaRepository,
    private val sedeRepository: SedeRepository,
    private val criterioRepository: CriterioRepository,
    private val usuarioRepository: UsuarioRepository,
    private val universidadRepository: UniversidadRepository,
    @Value("\${app.periodo-actual}") private val periodoActual: String
) {

    // ------------------------------------------------------------- CREATE

    @Transactional
    fun registrar(request: ResenaRequest): ResenaResponse {
        val usuario = obtenerOCrearUsuario(request.usuario)
        val sede = sedeRepository.findByCodigo(request.codigoSede)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "La sede indicada no existe")
        val criterios = validarCriterios(request)
        validarComentario(request.comentario)

        // HU2.03 - Escenario: segundo intento de evaluación en el mismo ciclo
        val existente = resenaRepository.findByUsuarioCodigoAndSedeCodigoAndAreaAndPeriodo(
            usuario.codigo, sede.codigo, request.area, periodoActual
        )
        if (existente != null) {
            throw ApiException(
                HttpStatus.CONFLICT,
                "Ya has enviado tu evaluación de esta área para esta sede este ciclo"
            )
        }

        val resena = Resena(
            usuario = usuario,
            sede = sede,
            area = request.area,
            periodo = periodoActual,
            fecha = LocalDateTime.now()
        )
        copiarDatos(request, resena)
        return aResponse(resenaRepository.save(resena), criterios)
    }

    // --------------------------------------------------------------- READ

    @Transactional(readOnly = true)
    fun buscarMia(codigoUsuario: String, codigoSede: String, area: String): ResenaResponse {
        val resena = resenaRepository.findByUsuarioCodigoAndSedeCodigoAndAreaAndPeriodo(
            codigoUsuario.uppercase(), codigoSede, area, periodoActual
        ) ?: throw ApiException(HttpStatus.NOT_FOUND, "Aún no has evaluado esta área en el ciclo actual")
        return aResponse(resena, criterioRepository.findByAreaOrderByOrden(area))
    }

    /** Opiniones públicas del área (sin datos personales del evaluador). */
    @Transactional(readOnly = true)
    fun listarPorArea(codigoSede: String, area: String): List<OpinionResponse> =
        resenaRepository.findBySedeCodigoAndAreaAndPeriodoOrderByFechaDesc(codigoSede, area, periodoActual)
            .map { r ->
                OpinionResponse(
                    idResena = r.idResena!!,
                    autor = if (r.esAnonima) "Anónimo" else nombreCorto(r.usuario!!.nombreCompleto),
                    rol = r.usuario!!.rol,
                    comentario = r.comentario,
                    promedio = promedio(r),
                    fecha = r.fecha.toString()
                )
            }

    // ------------------------------------------------------------- UPDATE

    @Transactional
    fun actualizar(idResena: Int, request: ResenaRequest): ResenaResponse {
        val resena = buscarPropia(idResena, request.usuario.codigo)
        if (resena.periodo != periodoActual) {
            throw ApiException(HttpStatus.CONFLICT, "Solo puedes modificar evaluaciones del ciclo actual")
        }
        if (resena.area != request.area || resena.sede!!.codigo != request.codigoSede) {
            throw ApiException(HttpStatus.BAD_REQUEST, "No se puede cambiar la sede ni el área de una evaluación")
        }
        val criterios = validarCriterios(request)
        validarComentario(request.comentario)

        copiarDatos(request, resena)
        resena.fecha = LocalDateTime.now()
        return aResponse(resenaRepository.save(resena), criterios)
    }

    // ------------------------------------------------------------- DELETE

    @Transactional
    fun eliminar(idResena: Int, codigoUsuario: String) {
        val resena = buscarPropia(idResena, codigoUsuario)
        resenaRepository.delete(resena)
    }

    // ---------------------------------------------------------- Auxiliares

    /** RN01, RN02 y RN03: correo institucional, rol según el correo y una cuenta por correo. */
    private fun obtenerOCrearUsuario(datos: UsuarioRequest): Usuario {
        val correo = datos.correo.trim().lowercase()
        val dominio = "@" + correo.substringAfter("@", "")
        val universidad = universidadRepository.findByDominioCorreo(dominio)
            ?: throw ApiException(HttpStatus.BAD_REQUEST, "Solo se aceptan correos institucionales")

        val codigo = correo.substringBefore("@").uppercase()
        if (codigo != datos.codigo.trim().uppercase()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "El código no corresponde al correo institucional")
        }

        usuarioRepository.findById(codigo).orElse(null)?.let { return it }

        val rol = ReglasUsuario.rolDeCodigo(codigo)
        return usuarioRepository.save(
            Usuario(
                codigo = codigo,
                idUniversidad = universidad.idUniversidad!!,
                correo = correo,
                nombreCompleto = datos.nombreCompleto.trim(),
                rol = rol,
                verificado = false
            )
        )
    }

    /** HU2.02 / HU2.04: se deben calificar todos los criterios del área, ni más ni menos. */
    private fun validarCriterios(request: ResenaRequest): List<Criterio> {
        val criterios = criterioRepository.findByAreaOrderByOrden(request.area)
        if (criterios.isEmpty()) {
            throw ApiException(HttpStatus.NOT_FOUND, "El área indicada no existe")
        }
        val esperados = criterios.map { it.idCriterio!! }.toSet()
        val enviados = request.calificaciones.map { it.idCriterio }
        if (enviados.size != esperados.size || enviados.toSet() != esperados) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Califica todos los criterios antes de guardar")
        }
        return criterios
    }

    private fun validarComentario(comentario: String?) {
        val error = Moderacion.validarComentario(comentario ?: "")
        if (error != null) throw ApiException(HttpStatus.BAD_REQUEST, error)
    }

    /** Solo el autor puede modificar o eliminar su evaluación. */
    private fun buscarPropia(idResena: Int, codigoUsuario: String): Resena {
        val resena = resenaRepository.findById(idResena).orElse(null)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "La evaluación no existe")
        if (resena.usuario!!.codigo != codigoUsuario.trim().uppercase()) {
            throw ApiException(HttpStatus.FORBIDDEN, "No puedes modificar la evaluación de otro usuario")
        }
        return resena
    }

    private fun copiarDatos(request: ResenaRequest, resena: Resena) {
        resena.experiencia = request.experiencia
        resena.comentario = request.comentario?.trim()?.ifBlank { null }
        resena.esAnonima = request.esAnonima
        resena.sinReciclaje = request.area == "sostenibilidad" && request.sinReciclaje
        // Los criterios ya se validaron, así que solo se actualiza el puntaje de cada uno
        request.calificaciones.forEach { resena.calificaciones[it.idCriterio] = it.puntaje }
    }

    private fun aResponse(resena: Resena, criterios: List<Criterio>) = ResenaResponse(
        idResena = resena.idResena!!,
        codigoUsuario = resena.usuario!!.codigo,
        codigoSede = resena.sede!!.codigo,
        area = resena.area,
        periodo = resena.periodo,
        experiencia = resena.experiencia,
        comentario = resena.comentario,
        esAnonima = resena.esAnonima,
        sinReciclaje = resena.sinReciclaje,
        fecha = resena.fecha.toString(),
        promedio = promedio(resena),
        calificaciones = criterios.map {
            CalificacionResponse(it.idCriterio!!, it.nombre, resena.calificaciones[it.idCriterio] ?: 0)
        }
    )

    private fun promedio(resena: Resena): Double =
        if (resena.calificaciones.isEmpty()) 0.0
        else Math.round(resena.calificaciones.values.average() * 10) / 10.0

    // "Jose Miguel Pariona" -> "Jose P."
    private fun nombreCorto(nombre: String): String {
        val partes = nombre.trim().split(Regex("\\s+"))
        return if (partes.size < 2) partes.first() else "${partes.first()} ${partes.last().first()}."
    }
}
