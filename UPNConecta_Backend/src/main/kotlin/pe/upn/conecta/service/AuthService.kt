package pe.upn.conecta.service

import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import pe.upn.conecta.config.ApiException
import pe.upn.conecta.dto.DominioResponse
import pe.upn.conecta.dto.LoginRequest
import pe.upn.conecta.dto.RegistroRequest
import pe.upn.conecta.dto.UsuarioResponse
import pe.upn.conecta.model.Universidad
import pe.upn.conecta.model.Usuario
import pe.upn.conecta.repository.DominioPermitidoRepository
import pe.upn.conecta.repository.SedeRepository
import pe.upn.conecta.repository.UniversidadRepository
import pe.upn.conecta.repository.UsuarioRepository

/**
 * Ingreso a la app (HU1.01, HU1.02 y HU1.03).
 * La forma principal es la cuenta de Microsoft 365 que la universidad ya entrega: no hay
 * formulario de registro, se valida el dominio del correo y la cuenta se crea la primera vez.
 * Correo + contraseña queda como alternativa para quien no pueda usar Microsoft.
 */
@Service
class AuthService(
    private val usuarioRepository: UsuarioRepository,
    private val universidadRepository: UniversidadRepository,
    private val sedeRepository: SedeRepository,
    private val dominioRepository: DominioPermitidoRepository,
    private val tokenService: TokenService
) {

    private val encoder = BCryptPasswordEncoder()
    private val graph = RestClient.create("https://graph.microsoft.com/v1.0")

    /** RN01: dice si el correo puede ingresar, sin crear nada todavía. */
    @Transactional(readOnly = true)
    fun validarDominio(correo: String): DominioResponse {
        val limpio = correo.trim().lowercase()
        val universidad = universidadDe(limpio)
        val codigo = limpio.substringBefore("@").uppercase()
        return DominioResponse(limpio, limpio.substring(limpio.indexOf("@")), universidad.siglas, ReglasUsuario.rolDeCodigo(codigo))
    }

    @Transactional
    fun registrar(request: RegistroRequest): UsuarioResponse {
        val correo = request.correo.trim().lowercase()
        val universidad = universidadDe(correo)
        validarSede(request.sedeCodigo)

        // RN03: una sola cuenta por correo
        if (usuarioRepository.findByCorreo(correo) != null) {
            throw ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con este correo")
        }

        val codigo = correo.substringBefore("@").uppercase()
        val usuario = usuarioRepository.save(
            Usuario(
                codigo = codigo,
                idUniversidad = universidad.idUniversidad!!,
                correo = correo,
                nombreCompleto = request.nombreCompleto.trim(),
                rol = ReglasUsuario.rolDeCodigo(codigo),
                claveHash = encoder.encode(request.clave),
                sedeCodigo = request.sedeCodigo,
                proveedor = "CORREO"
            )
        )
        return aResponse(usuario, universidad)
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): UsuarioResponse {
        val correo = request.correo.trim().lowercase()
        val usuario = usuarioRepository.findByCorreo(correo)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "Esta cuenta no está registrada. Crea una cuenta primero.")

        val hash = usuario.claveHash
            ?: throw ApiException(HttpStatus.BAD_REQUEST, "Esta cuenta se creó con Microsoft. Usa \"Continuar con Microsoft\".")

        // Mismo mensaje en ambos casos para no revelar cuál dato falló
        if (!encoder.matches(request.clave, hash)) {
            throw ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos")
        }
        return conToken(usuario, universidadRepository.findById(usuario.idUniversidad).get())
    }

    /**
     * Se consulta /me de Microsoft Graph con el token que envió la app.
     * Se usa userPrincipalName y no mail: en una app multiinquilino el campo mail lo puede editar
     * el administrador de otro directorio, mientras que el dominio del UPN está verificado por Microsoft.
     */
    @Transactional
    fun loginMicrosoft(accessToken: String): UsuarioResponse {
        val perfil = try {
            graph.get().uri("/me?\$select=displayName,userPrincipalName")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(PerfilGraph::class.java)
        } catch (e: RestClientException) {
            null
        } ?: throw ApiException(HttpStatus.UNAUTHORIZED, "Microsoft no validó tu sesión. Vuelve a intentarlo.")

        val correo = (perfil.userPrincipalName ?: "").trim().lowercase()
        val universidad = universidadDe(correo)

        val existente = usuarioRepository.findByCorreo(correo)
        if (existente != null) {
            // Microsoft probó quién es el dueño del correo: una contraseña creada antes por registro
            // (que no verifica el correo) deja de servir, por si la creó otra persona
            existente.verificado = true
            existente.claveHash = null
            existente.proveedor = "MICROSOFT"
            return conToken(existente, universidad)
        }

        val codigo = correo.substringBefore("@").uppercase()
        val nuevo = usuarioRepository.save(
            Usuario(
                codigo = codigo,
                idUniversidad = universidad.idUniversidad!!,
                correo = correo,
                nombreCompleto = perfil.displayName?.trim()?.ifBlank { null } ?: codigo,
                rol = ReglasUsuario.rolDeCodigo(codigo),
                verificado = true,
                proveedor = "MICROSOFT"
            )
        )
        return conToken(nuevo, universidad)
    }

    @Transactional
    fun cambiarSede(codigo: String, sedeCodigo: String): UsuarioResponse {
        validarSede(sedeCodigo)
        val usuario = usuarioRepository.findById(codigo).orElse(null)
            ?: throw ApiException(HttpStatus.UNAUTHORIZED, "Tu sesión no es válida. Vuelve a iniciar sesión.")
        usuario.sedeCodigo = sedeCodigo
        return aResponse(usuario, universidadRepository.findById(usuario.idUniversidad).get())
    }

    // RN01: solo correos de un dominio institucional habilitado
    private fun universidadDe(correo: String): Universidad {
        val arroba = correo.indexOf("@")
        val dominio = if (arroba > 0) correo.substring(arroba) else ""
        val permitido = dominioRepository.findById(dominio).orElse(null)
            ?: throw ApiException(HttpStatus.BAD_REQUEST, "Debe usar su correo institucional para ingresar")
        val universidad = universidadRepository.findById(permitido.idUniversidad).get()
        if (!permitido.activo) {
            throw ApiException(HttpStatus.FORBIDDEN, "${universidad.siglas} todavía no está habilitada en la app")
        }
        return universidad
    }

    private fun validarSede(sedeCodigo: String) {
        sedeRepository.findByCodigo(sedeCodigo)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "La sede indicada no existe")
    }

    private fun conToken(u: Usuario, universidad: Universidad) =
        aResponse(u, universidad).copy(token = tokenService.crear(u.codigo))

    private fun aResponse(u: Usuario, universidad: Universidad) = UsuarioResponse(
        codigo = u.codigo,
        correo = u.correo,
        nombreCompleto = u.nombreCompleto,
        rol = u.rol,
        sedeCodigo = u.sedeCodigo,
        universidad = universidad.siglas,
        proveedor = u.proveedor
    )

    /** Campos que se leen de la respuesta de Microsoft Graph. */
    data class PerfilGraph(
        val displayName: String? = null,
        val userPrincipalName: String? = null
    )
}
