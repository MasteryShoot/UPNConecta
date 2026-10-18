package pe.upn.conecta.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pe.upn.conecta.dto.CriterioResponse
import pe.upn.conecta.dto.MensajeResponse
import pe.upn.conecta.dto.OpinionResponse
import pe.upn.conecta.dto.ResenaRequest
import pe.upn.conecta.dto.ResenaResponse
import pe.upn.conecta.dto.SedeResponse
import pe.upn.conecta.repository.CriterioRepository
import pe.upn.conecta.repository.SedeRepository
import pe.upn.conecta.service.ResenaService

/** Datos maestros que la app necesita para armar el formulario. */
@RestController
@RequestMapping("/api")
class CatalogoController(
    private val sedeRepository: SedeRepository,
    private val criterioRepository: CriterioRepository
) {

    @GetMapping("/sedes")
    fun sedes(): List<SedeResponse> =
        sedeRepository.findAll().map { SedeResponse(it.idSede!!, it.codigo, it.nombre, it.direccion) }

    @GetMapping("/criterios")
    fun criterios(@RequestParam area: String): List<CriterioResponse> =
        criterioRepository.findByAreaOrderByOrden(area)
            .map { CriterioResponse(it.idCriterio!!, it.area, it.nombre) }
}

/** CRUD de reseñas de áreas (HU2.03). */
@RestController
@RequestMapping("/api/resenas")
class ResenaController(private val resenaService: ResenaService) {

    // CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registrar(@Valid @RequestBody request: ResenaRequest): ResenaResponse =
        resenaService.registrar(request)

    // READ: evaluación del usuario en una sede y área del ciclo actual
    @GetMapping("/mia")
    fun mia(
        @RequestParam usuario: String,
        @RequestParam sede: String,
        @RequestParam area: String
    ): ResenaResponse = resenaService.buscarMia(usuario, sede, area)

    // READ: opiniones públicas de una sede y área
    @GetMapping
    fun listar(@RequestParam sede: String, @RequestParam area: String): List<OpinionResponse> =
        resenaService.listarPorArea(sede, area)

    // UPDATE
    @PutMapping("/{idResena}")
    fun actualizar(
        @PathVariable idResena: Int,
        @Valid @RequestBody request: ResenaRequest
    ): ResenaResponse = resenaService.actualizar(idResena, request)

    // DELETE
    @DeleteMapping("/{idResena}")
    fun eliminar(@PathVariable idResena: Int, @RequestParam usuario: String): MensajeResponse {
        resenaService.eliminar(idResena, usuario)
        return MensajeResponse("Evaluación eliminada correctamente")
    }
}
