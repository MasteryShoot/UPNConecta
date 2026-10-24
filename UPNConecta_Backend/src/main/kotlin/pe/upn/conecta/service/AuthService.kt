package pe.upn.conecta.service

import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import pe.upn.conecta.config.ApiException
import pe.upn.conecta.dto.LoginRequest
import pe.upn.conecta.dto.RegistroRequest
import pe.upn.conecta.dto.UsuarioResponse
import pe.upn.conecta.model.Universidad
import pe.upn.conecta.model.Usuario
import pe.upn.conecta.repository.SedeRepository
import pe.upn.conecta.repository.UniversidadRepository
import pe.upn.conecta.repository.UsuarioRepository

/**
 * Registro e inicio de sesión (HU1.01 y HU1.02).
 * Hay dos formas de entrar: correo + contraseña, o la cuenta de Microsoft 365
 * que la universidad ya entrega a cada alumno y docente.
 */
@Service
class AuthService(
    private val usuarioRepository: UsuarioRepository,
    private val universidadRepository: UniversidadRepository,
    private val sedeRepository: SedeRepository
) {

    private val encoder = BCryptPasswordEncoder()
    private val graph = RestClient.create("https://graph.microsoft.com/v1.0")

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
        return aResponse(usuario, universidadRepository.findById(usuario.idUniversidad).get())
    }

    /**
     * Se consulta /me de Microsoft Graph con el token que envió la app.
     * Si Microsoft lo acepta, el correo es auténtico y no hace falta contraseña.
     */
    @Transactional
    fun loginMicrosoft(accessToken: String): UsuarioResponse {
        val perfil = try {
            graph.get().uri("/me?\$select=displayName,mail,userPrincipalName")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(PerfilGraph::class.java)
        } catch (e: RestClientException) {
            null
        } ?: throw ApiException(HttpStatus.UNAUTHORIZED, "Microsoft no validó tu sesión. Vuelve a intentarlo.")

        val correo = (perfil.mail ?: perfil.userPrincipalName ?: "").trim().lowercase()
        val universidad = universidadDe(correo)

        val existente = usuarioRepository.findByCorreo(correo)
        if (existente != null) {
            existente.verificado = true
            return aResponse(existente, universidad)
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
        return aResponse(nuevo, universidad)
    }

    @Transactional
    fun cambiarSede(codigo: String, sedeCodigo: String): UsuarioResponse {
        validarSede(sedeCodigo)
        val usuario = usuarioRepository.findById(codigo.uppercase()).orElse(null)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "El usuario no existe")
        usuario.sedeCodigo = sedeCodigo
        return aResponse(usuario, universidadRepository.findById(usuario.idUniversidad).get())
    }

    // RN01: solo correos de una universidad registrada
    private fun universidadDe(correo: String): Universidad {
        val dominio = "@" + correo.substringAfter("@", "")
        return universidadRepository.findByDominioCorreo(dominio)
            ?: throw ApiException(HttpStatus.BAD_REQUEST, "Debe usar su correo institucional para ingresar")
    }

    private fun validarSede(sedeCodigo: String) {
        sedeRepository.findByCodigo(sedeCodigo)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "La sede indicada no existe")
    }

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
        val mail: String? = null,
        val userPrincipalName: String? = null
    )
}
