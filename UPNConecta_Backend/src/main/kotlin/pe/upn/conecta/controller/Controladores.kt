package pe.upn.conecta.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pe.upn.conecta.config.AutenticacionInterceptor.Companion.USUARIO
import pe.upn.conecta.dto.CriterioResponse
import pe.upn.conecta.dto.DominioResponse
import pe.upn.conecta.dto.LoginRequest
import pe.upn.conecta.dto.MicrosoftLoginRequest
import pe.upn.conecta.dto.RegistroRequest
import pe.upn.conecta.dto.SedeUsuarioRequest
import pe.upn.conecta.dto.UniversidadResponse
import pe.upn.conecta.dto.UsuarioResponse
import pe.upn.conecta.dto.MensajeResponse
import pe.upn.conecta.dto.OpinionResponse
import pe.upn.conecta.dto.ResenaRequest
import pe.upn.conecta.dto.ResenaResponse
import pe.upn.conecta.dto.SedeResponse
import pe.upn.conecta.repository.CriterioRepository
import pe.upn.conecta.repository.SedeRepository
import pe.upn.conecta.repository.UniversidadRepository
import pe.upn.conecta.service.AuthService
import pe.upn.conecta.service.ResenaService

/** Datos maestros que la app necesita para armar el formulario. */
@RestController
@RequestMapping("/api")
class CatalogoController(
    private val sedeRepository: SedeRepository,
    private val criterioRepository: CriterioRepository,
    private val universidadRepository: UniversidadRepository
) {

    @GetMapping("/universidades")
    fun universidades(): List<UniversidadResponse> =
        universidadRepository.findAll().map {
            UniversidadResponse(it.idUniversidad!!, it.nombre, it.siglas, it.dominioCorreo)
        }

    @GetMapping("/sedes")
    fun sedes(): List<SedeResponse> =
        sedeRepository.findAll().map { SedeResponse(it.idSede!!, it.codigo, it.nombre, it.direccion) }

    @GetMapping("/criterios")
    fun criterios(@RequestParam area: String): List<CriterioResponse> =
        criterioRepository.findByAreaOrderByOrden(area)
            .map { CriterioResponse(it.idCriterio!!, it.area, it.nombre) }
}

/** CRUD de reseñas de áreas (HU2.03). El autor siempre sale del token, nunca de lo que envía la app. */
@RestController
@RequestMapping("/api/resenas")
class ResenaController(private val resenaService: ResenaService) {

    // CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "sesion")
    fun registrar(
        @Valid @RequestBody request: ResenaRequest,
        @Parameter(hidden = true) @RequestAttribute(USUARIO) usuario: String
    ): ResenaResponse = resenaService.registrar(request, usuario)

    // READ: evaluación del usuario en una sede y área del ciclo actual
    @GetMapping("/mia")
    @SecurityRequirement(name = "sesion")
    fun mia(
        @RequestParam sede: String,
        @RequestParam area: String,
        @Parameter(hidden = true) @RequestAttribute(USUARIO) usuario: String
    ): ResenaResponse = resenaService.buscarMia(usuario, sede, area)

    // READ: opiniones públicas de una sede y área (no pide sesión)
    @GetMapping
    fun listar(@RequestParam sede: String, @RequestParam area: String): List<OpinionResponse> =
        resenaService.listarPorArea(sede, area)

    // UPDATE
    @PutMapping("/{idResena}")
    @SecurityRequirement(name = "sesion")
    fun actualizar(
        @PathVariable idResena: Int,
        @Valid @RequestBody request: ResenaRequest,
        @Parameter(hidden = true) @RequestAttribute(USUARIO) usuario: String
    ): ResenaResponse = resenaService.actualizar(idResena, request, usuario)

    // DELETE
    @DeleteMapping("/{idResena}")
    @SecurityRequirement(name = "sesion")
    fun eliminar(
        @PathVariable idResena: Int,
        @Parameter(hidden = true) @RequestAttribute(USUARIO) usuario: String
    ): MensajeResponse {
        resenaService.eliminar(idResena, usuario)
        return MensajeResponse("Evaluación eliminada correctamente")
    }
}

/** Ingreso a la app: validación por dominio, Microsoft 365 y, como alternativa, contraseña. */
@RestController
@RequestMapping("/api")
class AuthController(private val authService: AuthService) {

    @GetMapping("/auth/dominio")
    @Operation(summary = "Valida si un correo pertenece a un dominio institucional habilitado")
    fun dominio(@RequestParam correo: String): DominioResponse =
        authService.validarDominio(correo)

    @PostMapping("/auth/microsoft")
    @Operation(summary = "Ingreso principal: crea la cuenta la primera vez, sin formulario de registro")
    fun microsoft(@Valid @RequestBody request: MicrosoftLoginRequest): UsuarioResponse =
        authService.loginMicrosoft(request.accessToken)

    @PostMapping("/auth/login")
    @Operation(summary = "Alternativa: ingreso con contraseña")
    fun login(@Valid @RequestBody request: LoginRequest): UsuarioResponse =
        authService.login(request)

    @PostMapping("/auth/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Alternativa: crear una cuenta con contraseña")
    fun registro(@Valid @RequestBody request: RegistroRequest): UsuarioResponse =
        authService.registrar(request)

    @PutMapping("/usuarios/me/sede")
    @SecurityRequirement(name = "sesion")
    fun cambiarSede(
        @Valid @RequestBody request: SedeUsuarioRequest,
        @Parameter(hidden = true) @RequestAttribute(USUARIO) usuario: String
    ): UsuarioResponse = authService.cambiarSede(usuario, request.sedeCodigo)
}
