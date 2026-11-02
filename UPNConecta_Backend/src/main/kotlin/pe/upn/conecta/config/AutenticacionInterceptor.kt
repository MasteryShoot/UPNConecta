package pe.upn.conecta.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import pe.upn.conecta.service.TokenService

/** Revisa el token "Authorization: Bearer ..." antes de entrar a una ruta privada. */
@Component
class AutenticacionInterceptor(private val tokenService: TokenService) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // Las opiniones son públicas: no hace falta iniciar sesión para leerlas
        if (request.method == "GET" && request.requestURI.trimEnd('/') == "/api/resenas") return true

        val cabecera = request.getHeader(HttpHeaders.AUTHORIZATION) ?: ""
        val codigo = if (cabecera.startsWith("Bearer ")) tokenService.verificar(cabecera.removePrefix("Bearer ")) else null
        if (codigo == null) {
            throw ApiException(HttpStatus.UNAUTHORIZED, "Tu sesión no es válida. Vuelve a iniciar sesión.")
        }
        request.setAttribute(USUARIO, codigo)
        return true
    }

    companion object {
        const val USUARIO = "codigoUsuario"
    }
}

@Configuration
class ConfiguracionWeb(private val autenticacion: AutenticacionInterceptor) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(autenticacion).addPathPatterns("/api/resenas", "/api/resenas/**", "/api/usuarios/**")
    }
}
